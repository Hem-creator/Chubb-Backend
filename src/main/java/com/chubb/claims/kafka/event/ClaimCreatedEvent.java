package com.chubb.claims.kafka.event;

import com.chubb.claims.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimCreatedEvent(
        String eventId,
        Long claimId,
        String claimNumber,
        String policyNumber,
        String claimantName,
        String claimantEmail,
        BigDecimal claimAmount,
        BigDecimal liabilityExposure,
        ClaimStatus status,
        Instant occurredAt
) implements ClaimDomainEvent {
}
