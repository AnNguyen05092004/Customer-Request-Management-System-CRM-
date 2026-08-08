package com.bzcom.crm.workflow.entity;

import com.bzcom.crm.request.domain.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "request_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private RequestStatus toStatus;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public RequestHistory(
            Long requestId,
            Long changedBy,
            RequestStatus fromStatus,
            RequestStatus toStatus,
            String memo) {
        this.requestId = requestId;
        this.changedBy = changedBy;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.memo = memo;
        this.changedAt = Instant.now();
    }
}
