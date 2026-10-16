package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.AdditionalInfoRequest;
import com.chubb.claims.dto.request.TransitionRequest;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimNote;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.BusinessException;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.kafka.ClaimEventProducer;
import com.chubb.claims.mapper.ClaimMapper;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.service.ClaimWorkflowService;
import com.chubb.claims.service.workflow.ClaimTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClaimWorkflowServiceImpl implements ClaimWorkflowService {

    private static final String ADDITIONAL_INFO_PREFIX = "Additional information from claimant: ";

    private final ClaimRepository claimRepository;
    private final ClaimMapper claimMapper;
    private final ClaimTransitionValidator transitionValidator;
    private final ClaimEventProducer eventProducer;

    @Override
    public ClaimResponse moveToReview(Long id, TransitionRequest request) {
        return transition(id, ClaimStatus.UNDER_REVIEW, request);
    }

    @Override
    public ClaimResponse requestMoreInfo(Long id, TransitionRequest request) {
        return transition(id, ClaimStatus.MORE_INFO_REQUIRED, request);
    }

    @Override
    public ClaimResponse approveClaim(Long id, TransitionRequest request) {
        return transition(id, ClaimStatus.APPROVED, request);
    }

    @Override
    public ClaimResponse rejectClaim(Long id, TransitionRequest request) {
        return transition(id, ClaimStatus.REJECTED, request);
    }

    @Override
    public ClaimResponse settleClaim(Long id, TransitionRequest request) {
        return transition(id, ClaimStatus.SETTLED, request);
    }

    @Override
    public ClaimResponse submitAdditionalInfo(Long id, AdditionalInfoRequest request) {
        TransitionRequest note = new TransitionRequest(ADDITIONAL_INFO_PREFIX + request.information().trim());
        return transition(id, ClaimStatus.UNDER_REVIEW, note, ClaimStatus.MORE_INFO_REQUIRED);
    }

    private ClaimResponse transition(Long id, ClaimStatus target, TransitionRequest request) {
        return transition(id, target, request, null);
    }

    /**
     * The one place a claim's status is changed: validate, apply, record note, publish event.
     * @param requiredCurrent if not null, the claim must currently be in this status
     */
    private ClaimResponse transition(Long id, ClaimStatus target, TransitionRequest request,
                                     ClaimStatus requiredCurrent) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
        ClaimStatus current = claim.getStatus();

        if (requiredCurrent != null && current != requiredCurrent) {
            throw new BusinessException("Additional information can only be submitted while the claim is "
                    + requiredCurrent + " (current: " + current + ")");
        }
        transitionValidator.validate(current, target);

        claim.setStatus(target);
        String note = (request == null || request.note() == null || request.note().isBlank())
                ? null : request.note().trim();
        if (note != null) {
            claim.addNote(ClaimNote.builder().note(note).build());
        }
        Claim saved = claimRepository.save(claim);

        log.info("Claim {} ({}) status {} -> {}", id, saved.getClaimNumber(), current, target);
        publishEvent(saved, target, note);
        return claimMapper.toResponse(saved);
    }

    /** Only the decisions other systems care about are published; review steps stay internal. */
    private void publishEvent(Claim claim, ClaimStatus target, String note) {
        switch (target) {
            case APPROVED -> eventProducer.publishApproved(claim);
            case REJECTED -> eventProducer.publishRejected(claim, note);
            case SETTLED -> eventProducer.publishSettled(claim);
            default -> { }
        }
    }
}
