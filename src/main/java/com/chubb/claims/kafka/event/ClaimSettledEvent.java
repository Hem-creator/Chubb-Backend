package com.chubb.claims.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimSettledEvent(
        String eventId,
        Long claimId,
        String claimNumber,
        String policyNumber,
        String claimantEmail,
        BigDecimal claimAmount,
        String assignedOfficer,
        Instant occurredAt
) implements ClaimDomainEvent {
}
