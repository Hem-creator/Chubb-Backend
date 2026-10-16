package com.chubb.claims.repository;

import com.chubb.claims.config.JpaConfig;
import org.springframework.context.annotation.Import;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimNote;
import com.chubb.claims.entity.ClaimStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private ClaimNoteRepository claimNoteRepository;

    @BeforeEach
    void seed() {
        claimRepository.save(claim("CLM-1", ClaimStatus.NEW, "alice", "100.00", "a@x.com"));
        claimRepository.save(claim("CLM-2", ClaimStatus.UNDER_REVIEW, "alice", "250.50", "b@x.com"));
        claimRepository.save(claim("CLM-3", ClaimStatus.UNDER_REVIEW, "bob", "50.00", "b@x.com"));
        claimRepository.save(claim("CLM-4", ClaimStatus.SETTLED, "bob", "900.00", "c@x.com"));
        claimRepository.save(claim("CLM-5", ClaimStatus.REJECTED, null, "700.00", "d@x.com"));
        claimRepository.save(claim("CLM-6", ClaimStatus.APPROVED, "carol", "999.00", "e@x.com"));
        claimRepository.save(claim("CLM-7", ClaimStatus.MORE_INFO_REQUIRED, "bob", "10.00", "f@x.com"));
    }

    @Test
    void derivedFinders() {
        assertThat(claimRepository.findByClaimNumber("CLM-1")).isPresent();
        assertThat(claimRepository.findByStatus(ClaimStatus.UNDER_REVIEW)).hasSize(2);
        assertThat(claimRepository.findByAssignedOfficer("alice")).hasSize(2);
        assertThat(claimRepository.findByClaimantEmail("b@x.com")).hasSize(2);
    }

    @Test
    void openClaimsCountIsNewUnderReviewAndMoreInfoOnly() {
        assertThat(claimRepository.countOpenClaims()).isEqualTo(4);
    }

    @Test
    void totalLiabilityExposureSumsOpenClaimsOnly() {
        assertThat(claimRepository.sumOpenLiabilityExposure()).isEqualByComparingTo("410.50");
    }

    @Test
    void totalLiabilityExposureIsZeroWhenNoClaims() {
        claimRepository.deleteAll();
        assertThat(claimRepository.sumOpenLiabilityExposure()).isEqualByComparingTo("0");
    }

    @Test
    void claimsGroupedByStatus() {
        Map<ClaimStatus, Long> counts = claimRepository.countClaimsByStatus().stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));
        assertThat(counts).containsEntry(ClaimStatus.NEW, 1L)
                .containsEntry(ClaimStatus.UNDER_REVIEW, 2L)
                .containsEntry(ClaimStatus.SETTLED, 1L)
                .containsEntry(ClaimStatus.REJECTED, 1L)
                .containsEntry(ClaimStatus.APPROVED, 1L)
                .containsEntry(ClaimStatus.MORE_INFO_REQUIRED, 1L);
    }

    @Test
    void openClaimsCountedPerOfficer() {
        Map<String, Long> counts = claimRepository.countOpenClaimsByOfficer().stream()
                .collect(Collectors.toMap(OfficerCount::getOfficer, OfficerCount::getCount));
        assertThat(counts).containsEntry("alice", 2L).containsEntry("bob", 2L).hasSize(2);
    }

    @Test
    void notesPersistedAndQueriedByClaimId() {
        Claim claim = claimRepository.findByClaimNumber("CLM-1").orElseThrow();
        claimNoteRepository.save(ClaimNote.builder().claim(claim).note("first").build());
        claimNoteRepository.save(ClaimNote.builder().claim(claim).note("second").build());

        assertThat(claimNoteRepository.countByClaimId(claim.getId())).isEqualTo(2);
        assertThat(claimNoteRepository.findByClaimIdOrderByCreatedAtAscIdAsc(claim.getId()))
                .extracting(ClaimNote::getNote).containsExactly("first", "second");
    }

    private Claim claim(String number, ClaimStatus status, String officer, String exposure, String email) {
        return Claim.builder()
                .claimNumber(number)
                .claimantName("Test")
                .claimantEmail(email)
                .policyNumber("POL-1")
                .description("desc")
                .incidentDate(LocalDate.now().minusDays(1))
                .claimAmount(new BigDecimal("1000.00"))
                .liabilityExposure(new BigDecimal(exposure))
                .status(status)
                .assignedOfficer(officer)
                .build();
    }

    @Test
    void officerWorkload_countsOnlyThatOfficersOpenClaims() {
        assertThat(claimRepository.countOpenClaimsByOfficer("alice")).isEqualTo(2);
        assertThat(claimRepository.countOpenClaimsByOfficer("bob")).isEqualTo(2);
        assertThat(claimRepository.countOpenClaimsByOfficer("carol")).isZero();
        assertThat(claimRepository.countOpenClaimsByOfficer("nobody")).isZero();
    }

    @Test
    void officerWorkload_sumsExposureOfOpenClaimsOnly() {
        assertThat(claimRepository.sumOpenLiabilityExposureByOfficer("alice")).isEqualByComparingTo("350.50");
        assertThat(claimRepository.sumOpenLiabilityExposureByOfficer("bob")).isEqualByComparingTo("60.00");
        assertThat(claimRepository.sumOpenLiabilityExposureByOfficer("nobody")).isEqualByComparingTo("0");
    }

    @Test
    void officerWorkload_claimsByStatusIncludesNonOpenStatuses() {
        Map<ClaimStatus, Long> counts = claimRepository.countClaimsByStatusForOfficer("bob").stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));
        assertThat(counts).containsEntry(ClaimStatus.UNDER_REVIEW, 1L)
                .containsEntry(ClaimStatus.MORE_INFO_REQUIRED, 1L)
                .containsEntry(ClaimStatus.SETTLED, 1L)
                .hasSize(3);
    }
}
