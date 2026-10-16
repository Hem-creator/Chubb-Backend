package com.chubb.claims.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClaimRequest(
        @NotBlank(message = "claimantName is mandatory")
        @Size(max = 150) String claimantName,

        @NotBlank(message = "claimantEmail is mandatory")
        @Email(message = "claimantEmail must be a valid email address")
        @Size(max = 150) String claimantEmail,

        @NotBlank(message = "policyNumber is mandatory")
        @Size(max = 50) String policyNumber,

        @NotBlank(message = "description is mandatory")
        @Size(max = 2000) String description,

        @NotNull(message = "incidentDate is mandatory")
        @PastOrPresent(message = "incidentDate cannot be in the future") LocalDate incidentDate,

        @NotNull(message = "claimAmount is mandatory")
        @DecimalMin(value = "0.0", inclusive = false, message = "claimAmount must be greater than 0")
        @Digits(integer = 13, fraction = 2) BigDecimal claimAmount,

        @NotNull(message = "liabilityExposure is mandatory")
        @DecimalMin(value = "0.0", inclusive = false, message = "liabilityExposure must be greater than 0")
        @Digits(integer = 13, fraction = 2) BigDecimal liabilityExposure
) {
}
