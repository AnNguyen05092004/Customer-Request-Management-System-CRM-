package com.bzcom.crm.workflow.dto.response;

import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.workflow.entity.RequestHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record HistoryResponse(
        Long id,
        Long requestId,
        Long changedBy,
        @Schema(nullable = true) RequestStatus fromStatus,
        @Schema(nullable = true) RequestStatus toStatus,
        @Schema(nullable = true) String memo,
        Instant changedAt) {

    public static HistoryResponse from(RequestHistory history) {
        return new HistoryResponse(
                history.getId(),
                history.getRequestId(),
                history.getChangedBy(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getMemo(),
                history.getChangedAt());
    }
}
