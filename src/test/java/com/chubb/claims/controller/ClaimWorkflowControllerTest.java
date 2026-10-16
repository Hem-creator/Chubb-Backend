package com.chubb.claims.controller;

import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.BusinessException;
import com.chubb.claims.exception.GlobalExceptionHandler;
import com.chubb.claims.service.ClaimWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClaimWorkflowControllerTest {

    @Mock
    private ClaimWorkflowService workflowService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClaimWorkflowController(workflowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ClaimResponse response(ClaimStatus status) {
        return new ClaimResponse(1L, "CLM-ABC", "Jane", "jane@example.com", "POL-1", "desc",
                LocalDate.now().minusDays(1), new BigDecimal("100.00"), new BigDecimal("50.00"),
                status, null, null, null);
    }

    @Test
    void approve_withoutBody_returnsApprovedClaim() throws Exception {
        when(workflowService.approveClaim(eq(1L), any())).thenReturn(response(ClaimStatus.APPROVED));

        mockMvc.perform(put("/api/claims/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Claim approved"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void reject_withNote_passesBodyToService() throws Exception {
        when(workflowService.rejectClaim(eq(1L), any())).thenReturn(response(ClaimStatus.REJECTED));

        mockMvc.perform(put("/api/claims/1/reject").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Policy lapsed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void underReview_requestMoreInfo_settle_routesExist() throws Exception {
        when(workflowService.moveToReview(eq(1L), any())).thenReturn(response(ClaimStatus.UNDER_REVIEW));
        when(workflowService.requestMoreInfo(eq(1L), any())).thenReturn(response(ClaimStatus.MORE_INFO_REQUIRED));
        when(workflowService.settleClaim(eq(1L), any())).thenReturn(response(ClaimStatus.SETTLED));

        mockMvc.perform(put("/api/claims/1/under-review")).andExpect(status().isOk());
        mockMvc.perform(put("/api/claims/1/request-more-info")).andExpect(status().isOk());
        mockMvc.perform(put("/api/claims/1/settle")).andExpect(status().isOk());
    }

    @Test
    void invalidTransition_returns409WithEnvelope() throws Exception {
        when(workflowService.settleClaim(eq(1L), any()))
                .thenThrow(new BusinessException("Invalid status transition: NEW -> SETTLED"));

        mockMvc.perform(put("/api/claims/1/settle"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid status transition: NEW -> SETTLED"));
    }

    @Test
    void tooLongNote_returns400() throws Exception {
        String body = "{\"note\":\"" + "x".repeat(4001) + "\"}";

        mockMvc.perform(put("/api/claims/1/approve").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.note").exists());
    }

    @Test
    void additionalInfo_returnsClaimBackUnderReview() throws Exception {
        when(workflowService.submitAdditionalInfo(eq(1L), any())).thenReturn(response(ClaimStatus.UNDER_REVIEW));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/claims/1/additional-info")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"information\":\"Repair invoice attached\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
    }

    @Test
    void additionalInfo_requiresInformation() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/claims/1/additional-info")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"information\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.information").value("information is mandatory"));
    }

    @Test
    void additionalInfo_wrongState_returns409() throws Exception {
        when(workflowService.submitAdditionalInfo(eq(1L), any()))
                .thenThrow(new BusinessException("Additional information can only be submitted while the claim is MORE_INFO_REQUIRED (current: NEW)"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/claims/1/additional-info")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"information\":\"x\"}"))
                .andExpect(status().isConflict());
    }
}
