package com.chubb.claims.kafka;

import com.chubb.claims.config.KafkaTopicsProperties;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.kafka.event.*;
import com.chubb.claims.mapper.ClaimEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ClaimEventProducer producer;
    private final Claim claim = Claim.builder().id(7L).claimNumber("CLM-ABC").policyNumber("POL-1")
            .claimantName("Jane").claimantEmail("jane@example.com").claimAmount(new BigDecimal("1500.00"))
            .liabilityExposure(new BigDecimal("1000.00")).status(ClaimStatus.NEW).assignedOfficer("John").build();

    @BeforeEach
    void setUp() {
        producer = new ClaimEventProducer(kafkaTemplate, new KafkaTopicsProperties(
                "claim-created", "claim-assigned", "claim-approved", "claim-rejected", "claim-settled"),
                new ClaimEventMapper());
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
    }

    @Test
    void eachEventGoesToItsOwnTopicKeyedByClaimNumber() {
        producer.publishCreated(claim);
        producer.publishAssigned(claim);
        producer.publishApproved(claim);
        producer.publishRejected(claim, "Policy lapsed");
        producer.publishSettled(claim);

        verify(kafkaTemplate).send(eq("claim-created"), eq("CLM-ABC"), any(ClaimCreatedEvent.class));
        verify(kafkaTemplate).send(eq("claim-assigned"), eq("CLM-ABC"), any(ClaimAssignedEvent.class));
        verify(kafkaTemplate).send(eq("claim-approved"), eq("CLM-ABC"), any(ClaimApprovedEvent.class));
        verify(kafkaTemplate).send(eq("claim-rejected"), eq("CLM-ABC"), any(ClaimRejectedEvent.class));
        verify(kafkaTemplate).send(eq("claim-settled"), eq("CLM-ABC"), any(ClaimSettledEvent.class));
    }

    @Test
    void eventsCarryClaimData() {
        producer.publishRejected(claim, "Policy lapsed");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("claim-rejected"), eq("CLM-ABC"), captor.capture());
        ClaimRejectedEvent event = (ClaimRejectedEvent) captor.getValue();
        assertThat(event.claimId()).isEqualTo(7L);
        assertThat(event.claimantEmail()).isEqualTo("jane@example.com");
        assertThat(event.reason()).isEqualTo("Policy lapsed");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void brokerFailureNeverPropagates() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new IllegalStateException("broker down"));

        producer.publishCreated(claim);
    }

    @Test
    void insideTransaction_sendsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            producer.publishApproved(claim);
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());

            List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
            syncs.forEach(TransactionSynchronization::afterCommit);

            verify(kafkaTemplate).send(eq("claim-approved"), eq("CLM-ABC"), any(ClaimApprovedEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void insideRolledBackTransaction_sendsNothing() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            producer.publishApproved(claim);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
