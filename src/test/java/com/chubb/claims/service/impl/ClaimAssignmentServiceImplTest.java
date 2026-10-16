package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.AssignClaimRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimAssignmentServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimEventProducer eventProducer;
    @Spy
    private ClaimMapper claimMapper = new ClaimMapper();
    @Spy
    private ClaimTransitionValidator transitionValidator = new ClaimTransitionValidator();
    @InjectMocks
    private ClaimAssignmentServiceImpl service;

    private Claim claimIn(ClaimStatus status) {
        Claim claim = Claim.builder().id(1L).claimNumber("CLM-X").status(status).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        return claim;
    }

    @Test
    void assign_setsOfficerAndPublishesEvent() {
        claimIn(ClaimStatus.NEW);
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.assignClaim(1L, new AssignClaimRequest("  John Smith "));

        assertThat(response.assignedOfficer()).isEqualTo("John Smith");
        verify(eventProducer).publishAssigned(any(Claim.class));
    }

    @Test
    void assign_allowsReassignment() {
        Claim claim = claimIn(ClaimStatus.UNDER_REVIEW);
        claim.setAssignedOfficer("Old Officer");
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.assignClaim(1L, new AssignClaimRequest("New Officer")).assignedOfficer())
                .isEqualTo("New Officer");
    }

    @Test
    void assign_rejectedForFinalClaims() {
        claimIn(ClaimStatus.SETTLED);

        assertThatThrownBy(() -> service.assignClaim(1L, new AssignClaimRequest("John")))
                .isInstanceOf(BusinessException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void assign_throwsNotFound() {
        when(claimRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignClaim(7L, new AssignClaimRequest("John")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
