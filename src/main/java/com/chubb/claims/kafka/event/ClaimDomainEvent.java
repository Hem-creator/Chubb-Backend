package com.chubb.claims.kafka.event;

import java.time.Instant;

/** Common contract of every event published to Kafka. The claim number is the message key. */
public sealed interface ClaimDomainEvent
        permits ClaimCreatedEvent, ClaimAssignedEvent, ClaimApprovedEvent, ClaimRejectedEvent, ClaimSettledEvent {

    String eventId();

    String claimNumber();

    Instant occurredAt();
}
