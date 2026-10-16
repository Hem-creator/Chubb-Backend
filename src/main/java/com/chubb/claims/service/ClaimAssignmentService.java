package com.chubb.claims.service;

import com.chubb.claims.dto.request.AssignClaimRequest;
import com.chubb.claims.dto.response.ClaimResponse;

public interface ClaimAssignmentService {

    /** Sets (or changes) the officer responsible for the claim. Not allowed on REJECTED or SETTLED claims. */
    ClaimResponse assignClaim(Long id, AssignClaimRequest request);
}
