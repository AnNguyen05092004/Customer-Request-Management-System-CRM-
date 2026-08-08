package com.bzcom.crm.request.service;

import com.bzcom.crm.alert.AlertService;
import com.bzcom.crm.alert.AlertType;
import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import com.bzcom.crm.common.response.PageResponse;
import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.request.RequestCreateRequest;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.entity.Request;
import com.bzcom.crm.request.mapper.RequestMapper;
import com.bzcom.crm.request.repository.RequestRepository;
import com.bzcom.crm.request.repository.RequestSpecification;
import com.bzcom.crm.request.security.RequestAccessPolicy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final MemberRepository memberRepository;
    private final AlertService alertService;
    private final RequestMapper requestMapper;

    @Transactional
    public RequestResponse create(RequestCreateRequest dto, CurrentUser currentUser) {
        Request request = new Request(
                dto.title(),
                dto.description(),
                dto.category(),
                dto.priority(),
                currentUser.memberId());
        Request saved = requestRepository.save(request);

        if (saved.getPriority() == RequestPriority.HIGH) {
            List<Member> admins = memberRepository.findAllByRoleOrderByIdAsc(MemberRole.ADMIN);
            for (Member admin : admins) {
                alertService.create(
                        admin.getId(),
                        saved.getId(),
                        AlertType.HIGH_PRIORITY_REGISTERED,
                        "New HIGH priority request: " + saved.getTitle());
            }
        }

        return requestMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> getRequests(
            Pageable pageable,
            RequestStatus status,
            RequestCategory category,
            RequestPriority priority,
            String keyword,
            CurrentUser currentUser) {
        Specification<Request> spec = RequestSpecification.build(currentUser, status, category, priority, keyword);
        Page<Request> page = requestRepository.findAll(spec, pageable);
        return PageResponse.from(page, requestMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RequestResponse getRequestDetail(Long id, CurrentUser currentUser) {
        Request request = requestRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found: " + id));
        RequestAccessPolicy.assertCanRead(request, currentUser);
        return requestMapper.toResponse(request);
    }
}
