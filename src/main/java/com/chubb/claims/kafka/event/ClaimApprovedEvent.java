package com.chubb.claims.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimApprovedEvent(
        String eventId,
        Long claimId,
        String claimNumber,
        String policyNumber,
        String claimantEmail,
        BigDecimal claimAmount,
        BigDecimal liabilityExposure,
        String assignedOfficer,
        Instant occurredAt
) implements ClaimDomainEvent {
}
