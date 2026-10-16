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
import com.chubb.claims.service.workflow.ClaimTransitionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimEventProducer eventProducer;
    @Spy
    private ClaimTransitionValidator transitionValidator = new ClaimTransitionValidator();
    @Spy
    private ClaimMapper claimMapper = new ClaimMapper();
    @InjectMocks
    private ClaimServiceImpl service;

    private final CreateClaimRequest create = new CreateClaimRequest("Jane Doe", "jane@example.com", "POL-1",
            "Rear-end collision", LocalDate.now().minusDays(2), new BigDecimal("1500.00"), new BigDecimal("1000.00"));
    private final UpdateClaimRequest update = new UpdateClaimRequest("Jane Smith", "jane@example.com",
            "Updated description", LocalDate.now().minusDays(1), new BigDecimal("2000.00"), new BigDecimal("1200.00"));

    @Test
    void createClaim_setsStatusNewAndGeneratesClaimNumber() {
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        ClaimResponse response = service.createClaim(create);

        assertThat(response.status()).isEqualTo(ClaimStatus.NEW);
        assertThat(response.claimNumber()).startsWith("CLM-");
        assertThat(response.assignedOfficer()).isNull();
        verify(eventProducer).publishCreated(any(Claim.class));
    }

    @Test
    void getClaimById_throwsWhenMissing() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClaimById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getClaimByClaimNumber_returnsClaim() {
        Claim claim = Claim.builder().id(1L).claimNumber("CLM-X").status(ClaimStatus.NEW).build();
        when(claimRepository.findByClaimNumber("CLM-X")).thenReturn(Optional.of(claim));

        assertThat(service.getClaimByClaimNumber("CLM-X").id()).isEqualTo(1L);
    }

    @Test
    void getClaimByClaimNumber_throwsWhenMissing() {
        when(claimRepository.findByClaimNumber("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClaimByClaimNumber("nope")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateClaim_appliesChangesAndKeepsStatus() {
        Claim claim = Claim.builder().id(1L).claimNumber("CLM-X").policyNumber("POL-1")
                .status(ClaimStatus.UNDER_REVIEW).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(claim)).thenReturn(claim);

        ClaimResponse response = service.updateClaim(1L, update);

        assertThat(response.claimantName()).isEqualTo("Jane Smith");
        assertThat(response.claimAmount()).isEqualByComparingTo("2000.00");
        assertThat(response.status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(response.policyNumber()).isEqualTo("POL-1");
    }

    @Test
    void updateClaim_rejectedForSettledClaim() {
        Claim claim = Claim.builder().id(1L).status(ClaimStatus.SETTLED).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.updateClaim(1L, update)).isInstanceOf(BusinessException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void deleteClaim_deletesExistingClaim() {
        Claim claim = Claim.builder().id(1L).status(ClaimStatus.NEW).build();
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        service.deleteClaim(1L);

        verify(claimRepository).delete(claim);
    }
}
