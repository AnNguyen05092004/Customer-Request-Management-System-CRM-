package com.bzcom.crm.alert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findByTargetMemberId(Long targetMemberId, Pageable pageable);
    Page<Alert> findByTargetMemberIdAndIsRead(Long targetMemberId, boolean isRead, Pageable pageable);
}