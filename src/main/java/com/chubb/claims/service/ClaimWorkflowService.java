package com.chubb.claims.service;

import com.chubb.claims.dto.request.AdditionalInfoRequest;
import com.chubb.claims.dto.request.TransitionRequest;
import com.chubb.claims.dto.response.ClaimResponse;

/** Claim lifecycle actions. Each throws BusinessException when the transition is not allowed. */
public interface ClaimWorkflowService {

    ClaimResponse moveToReview(Long id, TransitionRequest request);

    ClaimResponse requestMoreInfo(Long id, TransitionRequest request);

    ClaimResponse approveClaim(Long id, TransitionRequest request);

    ClaimResponse rejectClaim(Long id, TransitionRequest request);

    ClaimResponse settleClaim(Long id, TransitionRequest request);

    /** Claimant supplies requested information: MORE_INFO_REQUIRED -> UNDER_REVIEW, stored as a claim note. */
    ClaimResponse submitAdditionalInfo(Long id, AdditionalInfoRequest request);
}
