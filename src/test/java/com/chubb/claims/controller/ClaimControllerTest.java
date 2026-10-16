package com.chubb.claims.controller;

import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.GlobalExceptionHandler;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimService claimService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClaimController(claimService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private static final String VALID_BODY = """
            {"claimantName":"Jane Doe","claimantEmail":"jane@example.com","policyNumber":"POL-1",
             "description":"Collision","incidentDate":"%s","claimAmount":1500.00,"liabilityExposure":1000.00}
            """.formatted(LocalDate.now().minusDays(1));

    private ClaimResponse sample() {
        return new ClaimResponse(1L, "CLM-ABC", "Jane Doe", "jane@example.com", "POL-1", "Collision",
                LocalDate.now().minusDays(1), new BigDecimal("1500.00"), new BigDecimal("1000.00"),
                ClaimStatus.NEW, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_returns201WithEnvelope() throws Exception {
        when(claimService.createClaim(any())).thenReturn(sample());

        mockMvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Claim created successfully"))
                .andExpect(jsonPath("$.data.claimNumber").value("CLM-ABC"))
                .andExpect(jsonPath("$.data.status").value("NEW"));
    }

    @Test
    void create_rejectsInvalidInput() throws Exception {
        String body = """
                {"claimantName":"","claimantEmail":"not-an-email","policyNumber":"POL-1","description":"x",
                 "incidentDate":"%s","claimAmount":0,"liabilityExposure":-5}
                """.formatted(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.claimantName").exists())
                .andExpect(jsonPath("$.data.claimantEmail").exists())
                .andExpect(jsonPath("$.data.claimAmount").value("claimAmount must be greater than 0"))
                .andExpect(jsonPath("$.data.liabilityExposure").value("liabilityExposure must be greater than 0"));
    }

    @Test
    void getById_returnsClaim() throws Exception {
        when(claimService.getClaimById(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        when(claimService.getClaimById(9L)).thenThrow(new ResourceNotFoundException("Claim not found with id: 9"));

        mockMvc.perform(get("/api/claims/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Claim not found with id: 9"));
    }

    @Test
    void getByClaimNumber_returnsClaim() throws Exception {
        when(claimService.getClaimByClaimNumber("CLM-ABC")).thenReturn(sample());

        mockMvc.perform(get("/api/claims/claim-number/CLM-ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimNumber").value("CLM-ABC"));
    }

    @Test
    void update_returnsUpdatedClaim() throws Exception {
        when(claimService.updateClaim(any(), any())).thenReturn(sample());
        String body = """
                {"claimantName":"Jane Doe","claimantEmail":"jane@example.com","description":"Collision",
                 "incidentDate":"%s","claimAmount":1500.00,"liabilityExposure":1000.00}
                """.formatted(LocalDate.now().minusDays(1));

        mockMvc.perform(put("/api/claims/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Claim updated successfully"));
    }

    @Test
    void delete_returnsEnvelope() throws Exception {
        mockMvc.perform(delete("/api/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Claim deleted successfully"));
    }
}
