package com.bzcom.crm.request.dto.request;

import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull RequestCategory category,
        @NotNull RequestPriority priority) {}
