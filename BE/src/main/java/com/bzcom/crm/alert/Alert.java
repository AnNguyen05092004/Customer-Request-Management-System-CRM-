package com.bzcom.crm.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "alerts")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Alert {

    private static final int MAX_MESSAGE_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "target_member_id", nullable = false)
    private Long targetMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Alert(Long requestId, Long targetMemberId, AlertType alertType, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Alert message must not be blank");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Alert message must not exceed 255 characters");
        }
        this.requestId = requestId;
        this.targetMemberId = targetMemberId;
        this.alertType = alertType;
        this.message = message;
    }

    public void markRead() {
        isRead = true;
    }
}
