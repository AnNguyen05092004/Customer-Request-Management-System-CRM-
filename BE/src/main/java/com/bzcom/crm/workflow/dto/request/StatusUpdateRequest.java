package com.bzcom.crm.workflow.dto.request;

import com.bzcom.crm.request.domain.RequestStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull RequestStatus status, String memo, @NotNull @Min(0) Integer expectedVersion) {}
