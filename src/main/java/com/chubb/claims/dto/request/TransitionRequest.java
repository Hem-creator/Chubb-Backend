package com.chubb.claims.dto.request;

import jakarta.validation.constraints.Size;

/** Optional body for lifecycle actions; a non-blank note is stored as a ClaimNote (e.g. a rejection reason). */
public record TransitionRequest(
        @Size(max = 4000, message = "note must be at most 4000 characters") String note
) {
}
