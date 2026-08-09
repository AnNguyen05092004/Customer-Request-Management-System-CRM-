package com.bzcom.crm.request.dto.response;

import com.bzcom.crm.request.domain.RequestCategory;
import java.util.List;
import java.util.Map;

public record StatsResponse(
        long total,
        long completed,
        double completionRate,
        Map<RequestCategory, Long> byCategory,
        List<DeveloperStat> byDeveloper) {

    public record DeveloperStat(Long developerId, String developerName, long assignedCount, long doneCount) {}
}
