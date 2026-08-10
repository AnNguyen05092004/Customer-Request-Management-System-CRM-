package com.bzcom.crm.request.repository;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.entity.Request;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class RequestSpecification {

    private RequestSpecification() {}

    public static Specification<Request> build(
            CurrentUser currentUser,
            RequestStatus status,
            RequestCategory category,
            RequestPriority priority,
            String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Role-based scoping
            if (currentUser.role() == MemberRole.CLIENT) {
                predicates.add(cb.equal(root.get("clientId"), currentUser.memberId()));
            } else if (currentUser.role() == MemberRole.DEVELOPER) {
                predicates.add(cb.equal(root.get("assignedDeveloperId"), currentUser.memberId()));
            }

            // 2. Filters
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
