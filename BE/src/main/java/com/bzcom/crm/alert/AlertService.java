package com.bzcom.crm.alert;

import com.bzcom.crm.alert.dto.AlertResponse;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional
    public void create(Long targetMemberId, Long requestId, AlertType type, String message) {
        alertRepository.save(new Alert(requestId, targetMemberId, type, message));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getOwnAlerts(Long memberId, Boolean isRead) {
        List<Alert> alerts = isRead == null
                ? alertRepository.findByTargetMemberIdOrderByCreatedAtDesc(memberId)
                : alertRepository.findByTargetMemberIdAndIsReadOrderByCreatedAtDesc(memberId, isRead);
        return alerts.stream().map(AlertResponse::from).toList();
    }

    @Transactional
    public void markRead(Long alertId, Long memberId) {
        Alert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Alert not found"));
        if (!alert.getTargetMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        alert.markRead();
    }
}
