package com.bzcom.crm.request.repository;

import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

    long countByAssignedDeveloperIdAndStatusNot(Long assignedDeveloperId, RequestStatus status);
}
