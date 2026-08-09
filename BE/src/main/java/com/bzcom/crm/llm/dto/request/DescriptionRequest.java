package com.bzcom.crm.llm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DescriptionRequest(@NotBlank String description) {}
