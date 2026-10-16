package com.chubb.claims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_policy", columnList = "policyNumber"),
        @Index(name = "idx_claim_status", columnList = "status"),
        @Index(name = "idx_claim_officer", columnList = "assignedOfficer")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "notes")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String claimNumber;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String claimantName;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String claimantEmail;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String policyNumber;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String description;

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDate incidentDate;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 13, fraction = 2)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal claimAmount;

    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(precision = 15, scale = 2)
    private BigDecimal liabilityExposure;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClaimStatus status;

    @Size(max = 150)
    @Column(length = 150)
    private String assignedOfficer;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ClaimNote> notes = new ArrayList<>();

    public void addNote(ClaimNote note) {
        notes.add(note);
        note.setClaim(this);
    }

    public void removeNote(ClaimNote note) {
        notes.remove(note);
        note.setClaim(null);
    }
}
