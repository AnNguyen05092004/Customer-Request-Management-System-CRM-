package com.bzcom.crm.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record RefreshTokenRequest(@NotBlank @Size(min = 32, max = 512) String refreshToken) {}
