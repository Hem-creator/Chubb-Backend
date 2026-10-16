package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.TransitionRequest;
import com.chubb.claims.dto.response.ClaimResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.BusinessException;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.kafka.ClaimEventProducer;
import com.chubb.claims.mapper.ClaimMapper;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.service.workflow.ClaimTransitionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimWorkflowServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimEventProducer eventProducer;
    @Spy
    private ClaimMapper claimMapper = new ClaimMapper();
    @Spy
    private ClaimTransitionValidator transitionValidator = new ClaimTransitionValidator();
    @InjectMocks
    private ClaimWorkflowServiceImpl service;

    private Claim claimIn(ClaimStatus status) {
        Claim claim = Claim.builder().id(1L).claimNumber("CLM-X").status(status).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        return claim;
    }

    private void stubSave() {
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void moveToReview_fromNew() {
        claimIn(ClaimStatus.NEW);
        stubSave();

        assertThat(service.moveToReview(1L, null).status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
    }

    @Test
    void moveToReview_alsoAllowedFromMoreInfoRequired() {
        claimIn(ClaimStatus.MORE_INFO_REQUIRED);
        stubSave();

        assertThat(service.moveToReview(1L, null).status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
    }

    @Test
    void requestMoreInfo_fromUnderReview() {
        claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();

        assertThat(service.requestMoreInfo(1L, null).status()).isEqualTo(ClaimStatus.MORE_INFO_REQUIRED);
    }

    @Test
    void approveClaim_fromUnderReview() {
        claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();
        assertThat(service.approveClaim(1L, null).status()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void rejectClaim_fromUnderReview() {
        claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();

        assertThat(service.rejectClaim(1L, null).status()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void settleClaim_fromApproved() {
        claimIn(ClaimStatus.APPROVED);
        stubSave();

        assertThat(service.settleClaim(1L, null).status()).isEqualTo(ClaimStatus.SETTLED);
    }

    @Test
    void invalidTransition_throwsBusinessExceptionAndSavesNothing() {
        claimIn(ClaimStatus.NEW);

        assertThatThrownBy(() -> service.approveClaim(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("NEW -> APPROVED");
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void settleBeforeApproval_isRejected() {
        claimIn(ClaimStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> service.settleClaim(1L, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void missingClaim_throwsNotFound() {
        when(claimRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveClaim(5L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void note_isStoredAgainstTheClaim() {
        Claim claim = claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();

        service.rejectClaim(1L, new TransitionRequest("  Policy lapsed  "));

        assertThat(claim.getNotes()).hasSize(1);
        assertThat(claim.getNotes().get(0).getNote()).isEqualTo("Policy lapsed");
        assertThat(claim.getNotes().get(0).getClaim()).isSameAs(claim);
    }

    @Test
    void blankNote_isIgnored() {
        Claim claim = claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();

        service.approveClaim(1L, new TransitionRequest("   "));

        assertThat(claim.getNotes()).isEmpty();
    }

    @Test
    void approve_publishesApprovedEvent() {
        claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();

        service.approveClaim(1L, null);

        verify(eventProducer).publishApproved(any(Claim.class));
    }

    @Test
    void reject_publishesRejectedEventWithReason() {
        claimIn(ClaimStatus.UNDER_REVIEW);
        stubSave();

        service.rejectClaim(1L, new TransitionRequest("Policy lapsed"));

        verify(eventProducer).publishRejected(any(Claim.class), eq("Policy lapsed"));
    }

    @Test
    void settle_publishesSettledEvent() {
        claimIn(ClaimStatus.APPROVED);
        stubSave();

        ClaimResponse response = service.settleClaim(1L, null);

        verify(eventProducer).publishSettled(any(Claim.class));
        assertThat(response.status()).isEqualTo(ClaimStatus.SETTLED);
    }

    @Test
    void reviewSteps_publishNoEvent() {
        claimIn(ClaimStatus.NEW);
        stubSave();

        service.moveToReview(1L, null);

        verifyNoInteractions(eventProducer);
    }

    @Test
    void additionalInfo_movesBackToReviewAndStoresNote() {
        Claim claim = claimIn(ClaimStatus.MORE_INFO_REQUIRED);
        stubSave();

        ClaimResponse response = service.submitAdditionalInfo(1L,
                new com.chubb.claims.dto.request.AdditionalInfoRequest("  Repair invoice attached  "));

        assertThat(response.status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(claim.getNotes()).hasSize(1);
        assertThat(claim.getNotes().get(0).getNote())
                .isEqualTo("Additional information from claimant: Repair invoice attached");
        verifyNoInteractions(eventProducer);
    }

    @Test
    void additionalInfo_rejectedUnlessMoreInfoWasRequested() {
        claimIn(ClaimStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> service.submitAdditionalInfo(1L,
                new com.chubb.claims.dto.request.AdditionalInfoRequest("info")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MORE_INFO_REQUIRED");
        verify(claimRepository, never()).save(any());
    }
}
