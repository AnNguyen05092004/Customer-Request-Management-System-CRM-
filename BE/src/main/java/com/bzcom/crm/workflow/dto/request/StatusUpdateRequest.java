package com.bzcom.crm.workflow.dto.request;

import com.bzcom.crm.request.domain.RequestStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull RequestStatus status, @Size(max = 255) String memo, @NotNull @Min(0) Integer expectedVersion) {}
