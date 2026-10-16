package com.chubb.claims.mapper;

import com.chubb.claims.entity.Claim;
import com.chubb.claims.kafka.event.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Builds the Kafka event payloads from a Claim. */
@Component
public class ClaimEventMapper {

    public ClaimCreatedEvent toCreated(Claim c) {
        return new ClaimCreatedEvent(newId(), c.getId(), c.getClaimNumber(), c.getPolicyNumber(),
                c.getClaimantName(), c.getClaimantEmail(), c.getClaimAmount(), c.getLiabilityExposure(),
                c.getStatus(), Instant.now());
    }

    public ClaimAssignedEvent toAssigned(Claim c) {
        return new ClaimAssignedEvent(newId(), c.getId(), c.getClaimNumber(), c.getAssignedOfficer(),
                c.getStatus(), Instant.now());
    }

    public ClaimApprovedEvent toApproved(Claim c) {
        return new ClaimApprovedEvent(newId(), c.getId(), c.getClaimNumber(), c.getPolicyNumber(),
                c.getClaimantEmail(), c.getClaimAmount(), c.getLiabilityExposure(), c.getAssignedOfficer(),
                Instant.now());
    }

    public ClaimRejectedEvent toRejected(Claim c, String reason) {
        return new ClaimRejectedEvent(newId(), c.getId(), c.getClaimNumber(), c.getPolicyNumber(),
                c.getClaimantEmail(), c.getAssignedOfficer(), reason, Instant.now());
    }

    public ClaimSettledEvent toSettled(Claim c) {
        return new ClaimSettledEvent(newId(), c.getId(), c.getClaimNumber(), c.getPolicyNumber(),
                c.getClaimantEmail(), c.getClaimAmount(), c.getAssignedOfficer(), Instant.now());
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}
