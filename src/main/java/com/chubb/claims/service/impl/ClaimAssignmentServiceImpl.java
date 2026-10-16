package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.AssignClaimRequest;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.exception.BusinessException;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.kafka.ClaimEventProducer;
import com.chubb.claims.mapper.ClaimMapper;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.service.ClaimAssignmentService;
import com.chubb.claims.service.workflow.ClaimTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClaimAssignmentServiceImpl implements ClaimAssignmentService {

    private final ClaimRepository claimRepository;
    private final ClaimMapper claimMapper;
    private final ClaimTransitionValidator transitionValidator;
    private final ClaimEventProducer eventProducer;

    @Override
    public ClaimResponse assignClaim(Long id, AssignClaimRequest request) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
        if (transitionValidator.isFinal(claim.getStatus())) {
            throw new BusinessException(
                    "Claim " + id + " cannot be assigned because it is " + claim.getStatus());
        }

        String previous = claim.getAssignedOfficer();
        claim.setAssignedOfficer(request.officer().trim());
        Claim saved = claimRepository.save(claim);

        log.info("Claim {} assigned: {} -> {}", id, previous, saved.getAssignedOfficer());
        eventProducer.publishAssigned(saved);
        return claimMapper.toResponse(saved);
    }
}
