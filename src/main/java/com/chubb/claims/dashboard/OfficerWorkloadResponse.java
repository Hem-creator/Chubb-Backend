package com.chubb.claims.dashboard;

import com.chubb.claims.entity.ClaimStatus;

import java.math.BigDecimal;
import java.util.Map;

/** One officer's workload: open claims, exposure on them, and all their claims by status. */
public record OfficerWorkloadResponse(
        String officer,
        long openClaims,
        BigDecimal totalLiabilityExposure,
        Map<ClaimStatus, Long> claimsByStatus
) {
}
