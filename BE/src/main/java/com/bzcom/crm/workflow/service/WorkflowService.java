package com.bzcom.crm.workflow.service;

import com.bzcom.crm.alert.AlertService;
import com.bzcom.crm.alert.AlertType;
import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.entity.Request;
import com.bzcom.crm.request.mapper.RequestMapper;
import com.bzcom.crm.request.repository.RequestRepository;
import com.bzcom.crm.workflow.dto.request.AssignRequest;
import com.bzcom.crm.workflow.dto.request.StatusUpdateRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RequestRepository requestRepository;
    private final MemberRepository memberRepository;
    private final HistoryService historyService;
    private final AlertService alertService;
    private final RequestMapper requestMapper;

    @Transactional
    public RequestResponse assign(Long requestId, AssignRequest dto, CurrentUser currentUser) {
        Request request = requestRepository
                .findById(requestId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found: " + requestId));

        if (!request.getVersion().equals(dto.expectedVersion())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Request version conflict: expected " + dto.expectedVersion() + " but found "
                            + request.getVersion());
        }

        Member chosenDev;
        String memo;

        if (Boolean.TRUE.equals(dto.auto())) {
            List<Member> developers = memberRepository.findAllByRoleForUpdate(MemberRole.DEVELOPER);
            if (developers.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.NO_DEVELOPER_AVAILABLE, "No developer available for auto-assignment");
            }

            chosenDev = selectAutoAssignDeveloper(developers);
            memo = "auto-assigned to " + chosenDev.getName();
        } else {
            if (dto.developerId() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "developerId is required when auto is false");
            }
            chosenDev = memberRepository
                    .findById(dto.developerId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND, "Developer not found: " + dto.developerId()));
            if (chosenDev.getRole() != MemberRole.DEVELOPER) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Member is not a DEVELOPER");
            }
            memo = "assigned to " + chosenDev.getName() + " by Admin";
        }

        request.assignDeveloper(chosenDev.getId());
        Request saved = requestRepository.saveAndFlush(request);

        historyService.record(saved, currentUser.memberId(), null, null, memo);
        alertService.create(
                chosenDev.getId(),
                saved.getId(),
                AlertType.ASSIGNED,
                "You were assigned to request: " + saved.getTitle());

        return requestMapper.toResponse(saved);
    }

    @Transactional
    public RequestResponse updateStatus(Long requestId, StatusUpdateRequest dto, CurrentUser currentUser) {
        Request request = requestRepository
                .findById(requestId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found: " + requestId));

        if (currentUser.role() != MemberRole.ADMIN
                && !currentUser.memberId().equals(request.getAssignedDeveloperId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (!request.getVersion().equals(dto.expectedVersion())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Request version conflict: expected " + dto.expectedVersion() + " but found "
                            + request.getVersion());
        }

        if (request.getAssignedDeveloperId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Request is not assigned to any developer");
        }

        if (!request.getStatus().canTransitionTo(dto.status())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "Invalid status transition: " + request.getStatus() + " -> " + dto.status());
        }

        RequestStatus oldStatus = request.getStatus();
        request.updateStatus(dto.status());
        Request saved = requestRepository.saveAndFlush(request);

        if (dto.status() == RequestStatus.DONE) {
            Member assignedDev = memberRepository
                    .findById(request.getAssignedDeveloperId())
                    .orElseThrow(
                            () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Assigned developer not found"));
            assignedDev.recordCompletion(Instant.now());
            memberRepository.save(assignedDev);
        }

        historyService.record(saved, currentUser.memberId(), oldStatus, dto.status(), dto.memo());
        alertService.create(
                saved.getClientId(),
                saved.getId(),
                AlertType.STATUS_CHANGED,
                "Request status updated to " + dto.status() + ": " + saved.getTitle());

        return requestMapper.toResponse(saved);
    }

    private Member selectAutoAssignDeveloper(List<Member> developers) {
        record DevTaskCount(Member member, long taskCount) {}

        List<DevTaskCount> counts = developers.stream()
                .map(dev -> new DevTaskCount(
                        dev, requestRepository.countByAssignedDeveloperIdAndStatusNot(dev.getId(), RequestStatus.DONE)))
                .toList();

        long minCount = counts.stream().mapToLong(DevTaskCount::taskCount).min().orElse(0L);

        List<Member> tied = counts.stream()
                .filter(dtc -> dtc.taskCount() == minCount)
                .map(DevTaskCount::member)
                .toList();

        if (tied.size() == 1) {
            return tied.get(0);
        }

        return tied.stream()
                .sorted(Comparator.comparing(
                                Member::getLastCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Member::getId))
                .findFirst()
                .orElseThrow();
    }
}
