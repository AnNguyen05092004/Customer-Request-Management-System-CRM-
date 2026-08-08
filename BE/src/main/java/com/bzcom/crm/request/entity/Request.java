package com.bzcom.crm.request.entity;

import com.bzcom.crm.common.entity.BaseTimeEntity;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "requests")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Request extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "assigned_developer_id")
    private Long assignedDeveloperId;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Request(
            String title, String description, RequestCategory category, RequestPriority priority, Long clientId) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = RequestStatus.PENDING;
        this.clientId = clientId;
    }

    public void assignDeveloper(Long developerId) {
        this.assignedDeveloperId = developerId;
    }

    public void updateStatus(RequestStatus status) {
        this.status = status;
    }
}
