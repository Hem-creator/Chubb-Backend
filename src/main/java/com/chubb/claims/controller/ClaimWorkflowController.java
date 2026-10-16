package com.chubb.claims.controller;

import com.chubb.claims.dto.request.AdditionalInfoRequest;
import com.chubb.claims.dto.request.TransitionRequest;
import com.chubb.claims.dto.response.ApiResponse;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.service.ClaimWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Claim Workflow")
@RestController
@RequestMapping("/api/claims/{id}")
@RequiredArgsConstructor
public class ClaimWorkflowController {

    private final ClaimWorkflowService workflowService;

    @Operation(summary = "NEW or MORE_INFO_REQUIRED -> UNDER_REVIEW")
    @PutMapping("/under-review")
    public ApiResponse<ClaimResponse> underReview(@PathVariable Long id,
                                                  @RequestBody(required = false) @Valid TransitionRequest request) {
        return ApiResponse.ok("Claim moved to review", workflowService.moveToReview(id, request));
    }

    @Operation(summary = "UNDER_REVIEW -> MORE_INFO_REQUIRED")
    @PutMapping("/request-more-info")
    public ApiResponse<ClaimResponse> requestMoreInfo(@PathVariable Long id,
                                                      @RequestBody(required = false) @Valid TransitionRequest request) {
        return ApiResponse.ok("More information requested", workflowService.requestMoreInfo(id, request));
    }

    @Operation(summary = "UNDER_REVIEW -> APPROVED")
    @PutMapping("/approve")
    public ApiResponse<ClaimResponse> approve(@PathVariable Long id,
                                              @RequestBody(required = false) @Valid TransitionRequest request) {
        return ApiResponse.ok("Claim approved", workflowService.approveClaim(id, request));
    }

    @Operation(summary = "UNDER_REVIEW -> REJECTED")
    @PutMapping("/reject")
    public ApiResponse<ClaimResponse> reject(@PathVariable Long id,
                                             @RequestBody(required = false) @Valid TransitionRequest request) {
        return ApiResponse.ok("Claim rejected", workflowService.rejectClaim(id, request));
    }

    @Operation(summary = "Claimant submits requested information: MORE_INFO_REQUIRED -> UNDER_REVIEW")
    @PostMapping("/additional-info")
    public ApiResponse<ClaimResponse> additionalInfo(@PathVariable Long id,
                                                     @RequestBody @Valid AdditionalInfoRequest request) {
        return ApiResponse.ok("Additional information received, claim is back under review",
                workflowService.submitAdditionalInfo(id, request));
    }

    @Operation(summary = "APPROVED -> SETTLED")
    @PutMapping("/settle")
    public ApiResponse<ClaimResponse> settle(@PathVariable Long id,
                                             @RequestBody(required = false) @Valid TransitionRequest request) {
        return ApiResponse.ok("Claim settled", workflowService.settleClaim(id, request));
    }
}
