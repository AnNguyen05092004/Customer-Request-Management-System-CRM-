package com.bzcom.crm.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record LoginRequest(@NotBlank @Email @Size(max = 255) String email, @NotBlank @Size(max = 72) String password) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
