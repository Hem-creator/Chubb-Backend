package com.chubb.claims.controller;

import com.chubb.claims.dto.request.AddNoteRequest;
import com.chubb.claims.dto.request.AssignClaimRequest;
import com.chubb.claims.dto.response.ApiResponse;
import com.chubb.claims.dto.response.ClaimNoteResponse;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.service.ClaimAssignmentService;
import com.chubb.claims.service.ClaimNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Claims Officer")
@RestController
@RequestMapping("/api/claims/{id}")
@RequiredArgsConstructor
public class ClaimOfficerController {

    private final ClaimAssignmentService assignmentService;
    private final ClaimNoteService noteService;

    @Operation(summary = "Assign (or reassign) the claim to an officer")
    @PutMapping("/assign")
    public ApiResponse<ClaimResponse> assign(@PathVariable Long id, @Valid @RequestBody AssignClaimRequest request) {
        return ApiResponse.ok("Claim assigned successfully", assignmentService.assignClaim(id, request));
    }

    @Operation(summary = "Add a note to the claim")
    @PostMapping("/notes")
    public ResponseEntity<ApiResponse<ClaimNoteResponse>> addNote(@PathVariable Long id,
                                                                  @Valid @RequestBody AddNoteRequest request) {
        ClaimNoteResponse created = noteService.addNote(id, request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(ApiResponse.ok("Note added successfully", created));
    }

    @Operation(summary = "List the claim's notes, oldest first")
    @GetMapping("/notes")
    public ApiResponse<List<ClaimNoteResponse>> getNotes(@PathVariable Long id) {
        return ApiResponse.ok("Notes retrieved successfully", noteService.getNotes(id));
    }
}
