package com.chubb.claims.kafka;

import com.chubb.claims.config.KafkaTopicsProperties;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.kafka.event.ClaimDomainEvent;
import com.chubb.claims.mapper.ClaimEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes claim lifecycle events, one topic per event type, keyed by claim number so all events
 * of a claim land on the same partition and stay ordered.
 * <p>
 * Inside a transaction the message is sent only after commit, so consumers never see an event for
 * data that was rolled back. Sending is best-effort: a broker outage is logged, never propagated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ClaimEventMapper eventMapper;

    public void publishCreated(Claim claim) {
        send(topics.created(), eventMapper.toCreated(claim));
    }

    public void publishAssigned(Claim claim) {
        send(topics.assigned(), eventMapper.toAssigned(claim));
    }

    public void publishApproved(Claim claim) {
        send(topics.approved(), eventMapper.toApproved(claim));
    }

    public void publishRejected(Claim claim, String reason) {
        send(topics.rejected(), eventMapper.toRejected(claim, reason));
    }

    public void publishSettled(Claim claim) {
        send(topics.settled(), eventMapper.toSettled(claim));
    }

    private void send(String topic, ClaimDomainEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(topic, event);
                }
            });
        } else {
            doSend(topic, event);
        }
    }

    private void doSend(String topic, ClaimDomainEvent event) {
        try {
            kafkaTemplate.send(topic, event.claimNumber(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} to {} for claim {}",
                            event.getClass().getSimpleName(), topic, event.claimNumber(), ex);
                } else {
                    log.debug("Published {} to {}-{} for claim {}", event.getClass().getSimpleName(), topic,
                            result.getRecordMetadata().partition(), event.claimNumber());
                }
            });
        } catch (Exception ex) {
            log.error("Failed to publish {} to {} for claim {}",
                    event.getClass().getSimpleName(), topic, event.claimNumber(), ex);
        }
    }
}
