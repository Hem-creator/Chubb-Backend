package com.chubb.claims.dashboard;

import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.repository.OfficerCount;
import com.chubb.claims.repository.StatusCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @InjectMocks
    private DashboardServiceImpl service;

    private static StatusCount status(ClaimStatus s, long n) {
        return new StatusCount() {
            public ClaimStatus getStatus() { return s; }
            public long getCount() { return n; }
        };
    }

    private static OfficerCount officer(String name, long n) {
        return new OfficerCount() {
            public String getOfficer() { return name; }
            public long getCount() { return n; }
        };
    }

    @Test
    void workload_assemblesAllMetrics() {
        when(claimRepository.countOpenClaims()).thenReturn(9L);
        when(claimRepository.sumOpenLiabilityExposure()).thenReturn(new BigDecimal("100000.00"));
        when(claimRepository.countClaimsByStatus()).thenReturn(List.of(
                status(ClaimStatus.NEW, 4), status(ClaimStatus.UNDER_REVIEW, 5), status(ClaimStatus.APPROVED, 3)));
        when(claimRepository.countOpenClaimsByOfficer()).thenReturn(List.of(officer("John", 5), officer("Mike", 4)));

        DashboardResponse response = service.getWorkload();

        assertThat(response.totalOpenClaims()).isEqualTo(9);
        assertThat(response.totalLiabilityExposure()).isEqualByComparingTo("100000");
        assertThat(response.claimsByStatus())
                .containsEntry(ClaimStatus.NEW, 4L)
                .containsEntry(ClaimStatus.UNDER_REVIEW, 5L)
                .containsEntry(ClaimStatus.APPROVED, 3L);
        assertThat(response.claimsPerOfficer()).containsExactly(
                org.assertj.core.api.Assertions.entry("John", 5L), org.assertj.core.api.Assertions.entry("Mike", 4L));
    }

    @Test
    void workload_fillsMissingStatusesWithZero() {
        when(claimRepository.countOpenClaims()).thenReturn(0L);
        when(claimRepository.sumOpenLiabilityExposure()).thenReturn(BigDecimal.ZERO);
        when(claimRepository.countClaimsByStatus()).thenReturn(List.of());
        when(claimRepository.countOpenClaimsByOfficer()).thenReturn(List.of());

        DashboardResponse response = service.getWorkload();

        assertThat(response.claimsByStatus()).hasSize(ClaimStatus.values().length)
                .containsEntry(ClaimStatus.SETTLED, 0L);
        assertThat(response.claimsPerOfficer()).isEmpty();
    }

    @Test
    void officerWorkload_assemblesMetricsAndFillsStatuses() {
        when(claimRepository.countOpenClaimsByOfficer("John")).thenReturn(3L);
        when(claimRepository.sumOpenLiabilityExposureByOfficer("John")).thenReturn(new BigDecimal("4500.00"));
        when(claimRepository.countClaimsByStatusForOfficer("John")).thenReturn(List.of(
                status(ClaimStatus.UNDER_REVIEW, 2), status(ClaimStatus.SETTLED, 1)));

        OfficerWorkloadResponse response = service.getOfficerWorkload("John");

        assertThat(response.officer()).isEqualTo("John");
        assertThat(response.openClaims()).isEqualTo(3);
        assertThat(response.totalLiabilityExposure()).isEqualByComparingTo("4500");
        assertThat(response.claimsByStatus()).hasSize(ClaimStatus.values().length)
                .containsEntry(ClaimStatus.UNDER_REVIEW, 2L)
                .containsEntry(ClaimStatus.SETTLED, 1L)
                .containsEntry(ClaimStatus.NEW, 0L);
    }
}
