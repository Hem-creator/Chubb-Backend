package com.chubb.claims.controller;

import com.chubb.claims.dto.response.ClaimNoteResponse;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.GlobalExceptionHandler;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.service.ClaimAssignmentService;
import com.chubb.claims.service.ClaimNoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimOfficerControllerTest {

    @Mock
    private ClaimAssignmentService assignmentService;
    @Mock
    private ClaimNoteService noteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClaimOfficerController(assignmentService, noteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void assign_returnsClaimWithOfficer() throws Exception {
        when(assignmentService.assignClaim(eq(1L), any())).thenReturn(new ClaimResponse(1L, "CLM-ABC", "Jane",
                "jane@example.com", "POL-1", "desc", LocalDate.now().minusDays(1), new BigDecimal("100.00"),
                new BigDecimal("50.00"), ClaimStatus.NEW, "John Smith", null, null));

        mockMvc.perform(put("/api/claims/1/assign").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officer\":\"John Smith\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedOfficer").value("John Smith"));
    }

    @Test
    void assign_requiresOfficer() throws Exception {
        mockMvc.perform(put("/api/claims/1/assign").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officer\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.officer").value("officer is mandatory"));
    }

    @Test
    void addNote_returns201() throws Exception {
        when(noteService.addNote(eq(1L), any()))
                .thenReturn(new ClaimNoteResponse(5L, 1L, "Requested repair invoice.", LocalDateTime.now()));

        mockMvc.perform(post("/api/claims/1/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Requested repair invoice.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.claimId").value(1))
                .andExpect(jsonPath("$.data.note").value("Requested repair invoice."));
    }

    @Test
    void addNote_rejectsBlankNote() throws Exception {
        mockMvc.perform(post("/api/claims/1/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.note").value("note is mandatory"));
    }

    @Test
    void getNotes_returnsList() throws Exception {
        LocalDateTime t = LocalDateTime.now();
        when(noteService.getNotes(1L)).thenReturn(List.of(
                new ClaimNoteResponse(1L, 1L, "first", t), new ClaimNoteResponse(2L, 1L, "second", t.plusSeconds(1))));

        mockMvc.perform(get("/api/claims/1/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].note").value("first"))
                .andExpect(jsonPath("$.data[1].note").value("second"));
    }

    @Test
    void getNotes_returns404ForUnknownClaim() throws Exception {
        when(noteService.getNotes(9L)).thenThrow(new ResourceNotFoundException("Claim not found with id: 9"));

        mockMvc.perform(get("/api/claims/9/notes"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
