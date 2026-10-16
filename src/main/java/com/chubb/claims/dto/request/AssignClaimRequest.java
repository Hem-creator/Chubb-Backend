package com.chubb.claims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignClaimRequest(
        @NotBlank(message = "officer is mandatory")
        @Size(max = 150, message = "officer must be at most 150 characters") String officer
) {
}
