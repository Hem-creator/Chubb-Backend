package com.chubb.claims.kafka.event;

import com.chubb.claims.entity.ClaimStatus;

import java.time.Instant;

public record ClaimAssignedEvent(
        String eventId,
        Long claimId,
        String claimNumber,
        String assignedOfficer,
        ClaimStatus status,
        Instant occurredAt
) implements ClaimDomainEvent {
}
