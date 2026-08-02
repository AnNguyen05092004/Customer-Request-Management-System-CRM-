package com.bzcom.crm.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record MemberCreateRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 4, max = 72) String password,
        @NotBlank @Size(max = 100) String name) {

    public MemberCreateRequest {
        email = email == null ? null : email.trim();
        name = name == null ? null : name.trim();
    }
}
