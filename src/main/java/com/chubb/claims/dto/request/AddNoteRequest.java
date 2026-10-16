package com.chubb.claims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(
        @NotBlank(message = "note is mandatory")
        @Size(max = 4000, message = "note must be at most 4000 characters") String note
) {
}
