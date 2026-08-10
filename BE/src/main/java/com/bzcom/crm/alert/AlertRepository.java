package com.bzcom.crm.alert;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByTargetMemberIdOrderByCreatedAtDesc(Long targetMemberId);

    List<Alert> findByTargetMemberIdAndIsReadOrderByCreatedAtDesc(Long targetMemberId, boolean isRead);
}
