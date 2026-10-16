package com.chubb.claims.dashboard;

import com.chubb.claims.entity.ClaimStatus;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardResponse(
        long totalOpenClaims,
        BigDecimal totalLiabilityExposure,
        Map<ClaimStatus, Long> claimsByStatus,
        Map<String, Long> claimsPerOfficer
) {
}
