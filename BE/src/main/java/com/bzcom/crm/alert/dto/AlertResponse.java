package com.bzcom.crm.alert.dto;

import com.bzcom.crm.alert.Alert;
import com.bzcom.crm.alert.AlertType;
import java.time.Instant;

public record AlertResponse(
        Long id, Long requestId, AlertType alertType, String message, boolean isRead, Instant createdAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getRequestId(),
                alert.getAlertType(),
                alert.getMessage(),
                alert.isRead(),
                alert.getCreatedAt());
    }
}
