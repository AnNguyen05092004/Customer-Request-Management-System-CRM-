package com.bzcom.crm.request.dto.response;

import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import java.time.Instant;

public record RequestResponse(
        Long id,
        String title,
        String description,
        RequestCategory category,
        RequestPriority priority,
        RequestStatus status,
        Long clientId,
        Long assignedDeveloperId,
        Integer version,
        Instant createdAt,
        Instant updatedAt) {}
