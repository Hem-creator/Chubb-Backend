package com.chubb.claims.dashboard;

import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.repository.OfficerCount;
import com.chubb.claims.repository.StatusCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ClaimRepository claimRepository;

    @Override
    public DashboardResponse getWorkload() {
        return new DashboardResponse(
                claimRepository.countOpenClaims(),
                claimRepository.sumOpenLiabilityExposure(),
                withAllStatuses(claimRepository.countClaimsByStatus()),
                claimsPerOfficer());
    }

    @Override
    public OfficerWorkloadResponse getOfficerWorkload(String officer) {
        return new OfficerWorkloadResponse(
                officer,
                claimRepository.countOpenClaimsByOfficer(officer),
                claimRepository.sumOpenLiabilityExposureByOfficer(officer),
                withAllStatuses(claimRepository.countClaimsByStatusForOfficer(officer)));
    }

    /** Every status is present (zero when empty) so clients never have to handle missing keys. */
    private Map<ClaimStatus, Long> withAllStatuses(List<StatusCount> rows) {
        Map<ClaimStatus, Long> result = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            result.put(status, 0L);
        }
        for (StatusCount row : rows) {
            result.put(row.getStatus(), row.getCount());
        }
        return result;
    }

    /** Open claims per officer, busiest first (the query already sorts). */
    private Map<String, Long> claimsPerOfficer() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (OfficerCount row : claimRepository.countOpenClaimsByOfficer()) {
            result.put(row.getOfficer(), row.getCount());
        }
        return result;
    }
}
