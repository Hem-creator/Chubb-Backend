package com.chubb.claims.service;

import com.chubb.claims.dto.request.CreateClaimRequest;
import com.chubb.claims.dto.request.UpdateClaimRequest;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClaimService {

    ClaimResponse createClaim(CreateClaimRequest request);

    ClaimResponse getClaimById(Long id);

    ClaimResponse getClaimByClaimNumber(String claimNumber);

    Page<ClaimResponse> getClaims(ClaimStatus status, String assignedOfficer, String policyNumber, Pageable pageable);

    ClaimResponse updateClaim(Long id, UpdateClaimRequest request);

    void deleteClaim(Long id);
}
