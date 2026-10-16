package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.CreateClaimRequest;
import com.chubb.claims.dto.request.UpdateClaimRequest;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.BusinessException;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.kafka.ClaimEventProducer;
import com.chubb.claims.mapper.ClaimMapper;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.service.ClaimService;
import com.chubb.claims.service.workflow.ClaimTransitionValidator;
import com.chubb.claims.util.ClaimNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimMapper claimMapper;
    private final ClaimEventProducer eventProducer;
    private final ClaimTransitionValidator transitionValidator;

    @Override
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        Claim saved = claimRepository.save(claimMapper.toEntity(request, ClaimNumberGenerator.generate()));
        log.info("Created claim {} (id={}) for policy {}", saved.getClaimNumber(), saved.getId(), saved.getPolicyNumber());
        eventProducer.publishCreated(saved);
        return claimMapper.toResponse(saved);
    }

    @Override
    public ClaimResponse getClaimById(Long id) {
        return claimMapper.toResponse(findById(id));
    }

    @Override
    public ClaimResponse getClaimByClaimNumber(String claimNumber) {
        return claimRepository.findByClaimNumber(claimNumber)
                .map(claimMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with claim number: " + claimNumber));
    }

    @Override
    public Page<ClaimResponse> getClaims(ClaimStatus status, String assignedOfficer, String policyNumber,
                                         Pageable pageable) {
        Page<Claim> page;
        if (status != null) {
            page = claimRepository.findByStatus(status, pageable);
        } else if (hasText(assignedOfficer)) {
            page = claimRepository.findByAssignedOfficer(assignedOfficer, pageable);
        } else if (hasText(policyNumber)) {
            page = claimRepository.findByPolicyNumber(policyNumber, pageable);
        } else {
            page = claimRepository.findAll(pageable);
        }
        return page.map(claimMapper::toResponse);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaim(Long id, UpdateClaimRequest request) {
        Claim claim = findById(id);
        if (transitionValidator.isFinal(claim.getStatus())) {
            throw new BusinessException(
                    "Claim " + id + " can no longer be edited (status: " + claim.getStatus() + ")");
        }
        claimMapper.updateEntity(claim, request);
        Claim saved = claimRepository.save(claim);
        log.info("Updated claim {}", id);
        return claimMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteClaim(Long id) {
        claimRepository.delete(findById(id));
        log.info("Deleted claim {}", id);
    }

    private Claim findById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
