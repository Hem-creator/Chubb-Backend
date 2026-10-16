package com.chubb.claims.kafka.event;

import java.time.Instant;

public record ClaimRejectedEvent(
        String eventId,
        Long claimId,
        String claimNumber,
        String policyNumber,
        String claimantEmail,
        String assignedOfficer,
        String reason,
        Instant occurredAt
) implements ClaimDomainEvent {
}
