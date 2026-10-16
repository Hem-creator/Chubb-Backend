package com.chubb.claims.dashboard;

import com.chubb.claims.entity.ClaimStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Test
    void workload_returnsDashboardJson() throws Exception {
        Map<String, Long> officers = new LinkedHashMap<>();
        officers.put("John", 5L);
        officers.put("Mike", 4L);
        when(dashboardService.getWorkload()).thenReturn(new DashboardResponse(15, new BigDecimal("100000"),
                Map.of(ClaimStatus.NEW, 4L, ClaimStatus.UNDER_REVIEW, 5L), officers));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboardService)).build();

        mockMvc.perform(get("/api/dashboard/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOpenClaims").value(15))
                .andExpect(jsonPath("$.totalLiabilityExposure").value(100000))
                .andExpect(jsonPath("$.claimsByStatus.NEW").value(4))
                .andExpect(jsonPath("$.claimsByStatus.UNDER_REVIEW").value(5))
                .andExpect(jsonPath("$.claimsPerOfficer.John").value(5))
                .andExpect(jsonPath("$.claimsPerOfficer.Mike").value(4));
    }

    @Test
    void officerWorkload_returnsJson() throws Exception {
        when(dashboardService.getOfficerWorkload("John Smith")).thenReturn(new OfficerWorkloadResponse(
                "John Smith", 4, new BigDecimal("12000"), Map.of(ClaimStatus.UNDER_REVIEW, 4L)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OfficerWorkloadController(dashboardService)).build();

        mockMvc.perform(get("/api/officers/John Smith/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officer").value("John Smith"))
                .andExpect(jsonPath("$.openClaims").value(4))
                .andExpect(jsonPath("$.totalLiabilityExposure").value(12000))
                .andExpect(jsonPath("$.claimsByStatus.UNDER_REVIEW").value(4));
    }
}
