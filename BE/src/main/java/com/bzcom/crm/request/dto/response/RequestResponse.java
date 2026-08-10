package com.bzcom.crm.request.dto.response;

import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record RequestResponse(
        Long id,
        String title,
        @Schema(nullable = true) String description,
        RequestCategory category,
        RequestPriority priority,
        RequestStatus status,
        Long clientId,
        @Schema(nullable = true) Long assignedDeveloperId,
        Integer version,
        Instant createdAt,
        Instant updatedAt) {}
