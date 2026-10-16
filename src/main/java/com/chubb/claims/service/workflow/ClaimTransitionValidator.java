package com.chubb.claims.service.workflow;

import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.chubb.claims.entity.ClaimStatus.*;

/**
 * Single source of truth for the claim lifecycle. Every status change in the application
 * must be checked here; no other class encodes transition rules.
 */
@Component
public class ClaimTransitionValidator {

    private static final Map<ClaimStatus, Set<ClaimStatus>> TRANSITIONS = new EnumMap<>(ClaimStatus.class);

    static {
        TRANSITIONS.put(NEW, EnumSet.of(UNDER_REVIEW));
        TRANSITIONS.put(UNDER_REVIEW, EnumSet.of(MORE_INFO_REQUIRED, APPROVED, REJECTED));
        TRANSITIONS.put(MORE_INFO_REQUIRED, EnumSet.of(UNDER_REVIEW));
        TRANSITIONS.put(APPROVED, EnumSet.of(SETTLED));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(ClaimStatus.class));
        TRANSITIONS.put(SETTLED, EnumSet.noneOf(ClaimStatus.class));
    }

    public boolean canTransition(ClaimStatus from, ClaimStatus to) {
        return from != null && to != null && TRANSITIONS.get(from).contains(to);
    }

    public Set<ClaimStatus> allowedTransitions(ClaimStatus from) {
        return Set.copyOf(TRANSITIONS.get(from));
    }

    /** @throws BusinessException if the transition is not permitted */
    public void validate(ClaimStatus from, ClaimStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException("Invalid status transition: " + from + " -> " + to
                    + ". Allowed from " + from + ": " + TRANSITIONS.get(from));
        }
    }

    /** Terminal claims cannot change any more, so they are also read-only. */
    public boolean isFinal(ClaimStatus status) {
        return TRANSITIONS.get(status).isEmpty();
    }
}
