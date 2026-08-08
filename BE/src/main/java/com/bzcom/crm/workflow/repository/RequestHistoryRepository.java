package com.bzcom.crm.workflow.repository;

import com.bzcom.crm.workflow.entity.RequestHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    List<RequestHistory> findByRequestIdOrderByChangedAtAscIdAsc(Long requestId);
}
