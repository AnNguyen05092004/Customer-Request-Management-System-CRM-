package com.bzcom.crm.workflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull Boolean auto,
        Long developerId,
        @NotNull @Min(0) Integer expectedVersion) {}
