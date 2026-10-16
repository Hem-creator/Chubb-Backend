package com.chubb.claims.mapper;

import com.chubb.claims.dto.request.CreateClaimRequest;
import com.chubb.claims.dto.request.UpdateClaimRequest;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class ClaimMapper {

    public Claim toEntity(CreateClaimRequest request, String claimNumber) {
        return Claim.builder()
                .claimNumber(claimNumber)
                .claimantName(request.claimantName())
                .claimantEmail(request.claimantEmail())
                .policyNumber(request.policyNumber())
                .description(request.description())
                .incidentDate(request.incidentDate())
                .claimAmount(request.claimAmount())
                .liabilityExposure(request.liabilityExposure())
                .status(ClaimStatus.NEW)
                .build();
    }

    public void updateEntity(Claim claim, UpdateClaimRequest request) {
        claim.setClaimantName(request.claimantName());
        claim.setClaimantEmail(request.claimantEmail());
        claim.setDescription(request.description());
        claim.setIncidentDate(request.incidentDate());
        claim.setClaimAmount(request.claimAmount());
        claim.setLiabilityExposure(request.liabilityExposure());
    }

    public ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getClaimantName(),
                claim.getClaimantEmail(),
                claim.getPolicyNumber(),
                claim.getDescription(),
                claim.getIncidentDate(),
                claim.getClaimAmount(),
                claim.getLiabilityExposure(),
                claim.getStatus(),
                claim.getAssignedOfficer(),
                claim.getCreatedAt(),
                claim.getUpdatedAt());
    }
}
