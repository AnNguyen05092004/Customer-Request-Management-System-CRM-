package com.bzcom.crm.workflow.service;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.entity.Request;
import com.bzcom.crm.request.repository.RequestRepository;
import com.bzcom.crm.request.security.RequestAccessPolicy;
import com.bzcom.crm.workflow.dto.response.HistoryResponse;
import com.bzcom.crm.workflow.entity.RequestHistory;
import com.bzcom.crm.workflow.repository.RequestHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final RequestHistoryRepository historyRepository;
    private final RequestRepository requestRepository;

    @Transactional
    public void record(Request request, Long changedBy, RequestStatus fromStatus, RequestStatus toStatus, String memo) {
        historyRepository.save(new RequestHistory(request.getId(), changedBy, fromStatus, toStatus, memo));
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(Long requestId, CurrentUser currentUser) {
        Request request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found: " + requestId));

        RequestAccessPolicy.assertCanRead(request, currentUser);

        return historyRepository.findByRequestIdOrderByChangedAtAscIdAsc(requestId).stream()
                .map(HistoryResponse::from)
                .toList();
    }
}
