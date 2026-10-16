package com.chubb.claims.dto.response;

import com.chubb.claims.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimResponse(
        Long id,
        String claimNumber,
        String claimantName,
        String claimantEmail,
        String policyNumber,
        String description,
        LocalDate incidentDate,
        BigDecimal claimAmount,
        BigDecimal liabilityExposure,
        ClaimStatus status,
        String assignedOfficer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
