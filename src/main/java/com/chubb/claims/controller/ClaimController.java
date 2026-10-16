package com.chubb.claims.controller;

import com.chubb.claims.dto.request.CreateClaimRequest;
import com.chubb.claims.dto.request.UpdateClaimRequest;
import com.chubb.claims.dto.response.ApiResponse;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.dto.response.PageResponse;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Claims")
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @Operation(summary = "Create a claim (status NEW, claim number auto-generated)")
    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> create(@Valid @RequestBody CreateClaimRequest request) {
        ClaimResponse created = claimService.createClaim(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.ok("Claim created successfully", created));
    }

    @Operation(summary = "List claims (paged), optionally filtered by status, officer or policy")
    @GetMapping
    public ApiResponse<PageResponse<ClaimResponse>> list(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) String assignedOfficer,
            @RequestParam(required = false) String policyNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var page = claimService.getClaims(status, assignedOfficer, policyNumber, pageable);
        return ApiResponse.ok("Claims retrieved successfully", PageResponse.from(page));
    }

    @Operation(summary = "Get a claim by id")
    @GetMapping("/{id}")
    public ApiResponse<ClaimResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Claim retrieved successfully", claimService.getClaimById(id));
    }

    @Operation(summary = "Get a claim by claim number")
    @GetMapping("/claim-number/{claimNumber}")
    public ApiResponse<ClaimResponse> getByClaimNumber(@PathVariable String claimNumber) {
        return ApiResponse.ok("Claim retrieved successfully", claimService.getClaimByClaimNumber(claimNumber));
    }

    @Operation(summary = "Update a claim")
    @PutMapping("/{id}")
    public ApiResponse<ClaimResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateClaimRequest request) {
        return ApiResponse.ok("Claim updated successfully", claimService.updateClaim(id, request));
    }

    @Operation(summary = "Delete a claim and its notes")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return ApiResponse.ok("Claim deleted successfully", null);
    }
}
