package com.bzcom.crm.request.repository;

import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.entity.Request;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

    long countByAssignedDeveloperIdAndStatusNot(Long assignedDeveloperId, RequestStatus status);

    long countByStatus(RequestStatus status);

    long countByAssignedDeveloperIdAndStatus(Long assignedDeveloperId, RequestStatus status);

    @Query("select r.category as category, count(r) as total from Request r group by r.category")
    List<CategoryCount> countGroupedByCategory();

    interface CategoryCount {
        RequestCategory getCategory();

        long getTotal();
    }
}
