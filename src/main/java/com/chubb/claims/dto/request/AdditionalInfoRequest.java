package com.chubb.claims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdditionalInfoRequest(
        @NotBlank(message = "information is mandatory")
        @Size(max = 3900, message = "information must be at most 3900 characters") String information
) {
}
