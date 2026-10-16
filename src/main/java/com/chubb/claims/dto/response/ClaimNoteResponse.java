package com.chubb.claims.dto.response;

import java.time.LocalDateTime;

public record ClaimNoteResponse(
        Long id,
        Long claimId,
        String note,
        LocalDateTime createdAt
) {
}
