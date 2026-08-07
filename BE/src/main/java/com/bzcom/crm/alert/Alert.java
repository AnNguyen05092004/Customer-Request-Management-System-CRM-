package com.bzcom.crm.alert;

import com.bzcom.crm.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alerts")
@Getter
@NoArgsConstructor
public class Alert extends BaseTimeEntity {

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

    public Alert(Long requestId, Long targetMemberId, AlertType alertType, String message) {
        this.requestId = requestId;
        this.targetMemberId = targetMemberId;
        this.alertType = alertType;
        this.message = message;
    }

    public void markRead() {
        this.isRead = true;
    }
}