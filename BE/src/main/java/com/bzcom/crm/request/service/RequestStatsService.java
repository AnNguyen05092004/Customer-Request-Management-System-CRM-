package com.bzcom.crm.request.service;

import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.response.StatsResponse;
import com.bzcom.crm.request.repository.RequestRepository;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestStatsService {

    private final RequestRepository requestRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        long total = requestRepository.count();
        long completed = requestRepository.countByStatus(RequestStatus.DONE);
        double completionRate = total == 0 ? 0.0 : (double) completed / total;

        Map<RequestCategory, Long> byCategory = new EnumMap<>(RequestCategory.class);
        for (RequestCategory category : RequestCategory.values()) {
            byCategory.put(category, 0L);
        }
        for (RequestRepository.CategoryCount row : requestRepository.countGroupedByCategory()) {
            byCategory.put(row.getCategory(), row.getTotal());
        }

        var byDeveloper = memberRepository.findAllByRoleOrderByIdAsc(MemberRole.DEVELOPER).stream()
                .map(this::toDeveloperStat)
                .toList();

        return new StatsResponse(total, completed, completionRate, byCategory, byDeveloper);
    }

    private StatsResponse.DeveloperStat toDeveloperStat(Member developer) {
        long assignedCount =
                requestRepository.countByAssignedDeveloperIdAndStatusNot(developer.getId(), RequestStatus.DONE);
        long doneCount = requestRepository.countByAssignedDeveloperIdAndStatus(developer.getId(), RequestStatus.DONE);
        return new StatsResponse.DeveloperStat(developer.getId(), developer.getName(), assignedCount, doneCount);
    }
}
