package com.bzcom.crm.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.response.StatsResponse;
import com.bzcom.crm.request.repository.RequestRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestStatsServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    void shouldReturnZeroCompletionRateWhenThereAreNoRequests() {
        when(requestRepository.count()).thenReturn(0L);
        when(requestRepository.countByStatus(RequestStatus.DONE)).thenReturn(0L);
        when(requestRepository.countGroupedByCategory()).thenReturn(List.of());
        when(memberRepository.findAllByRoleOrderByIdAsc(MemberRole.DEVELOPER)).thenReturn(List.of());

        StatsResponse stats = new RequestStatsService(requestRepository, memberRepository).getStats();

        assertThat(stats.total()).isZero();
        assertThat(stats.completionRate()).isZero();
        assertThat(stats.byCategory()).containsEntry(RequestCategory.BUG, 0L);
    }

    @Test
    void shouldAggregateTotalsByCategoryAndByDeveloper() {
        Member developer = new Member("dev@bzcom.com", "hash", "Dev One", MemberRole.DEVELOPER);
        setId(developer, 5L);

        when(requestRepository.count()).thenReturn(10L);
        when(requestRepository.countByStatus(RequestStatus.DONE)).thenReturn(4L);
        when(requestRepository.countGroupedByCategory())
                .thenReturn(
                        List.of(categoryCount(RequestCategory.BUG, 6L), categoryCount(RequestCategory.FEATURE, 4L)));
        when(memberRepository.findAllByRoleOrderByIdAsc(MemberRole.DEVELOPER)).thenReturn(List.of(developer));
        when(requestRepository.countByAssignedDeveloperIdAndStatusNot(5L, RequestStatus.DONE))
                .thenReturn(3L);
        when(requestRepository.countByAssignedDeveloperIdAndStatus(5L, RequestStatus.DONE))
                .thenReturn(2L);

        StatsResponse stats = new RequestStatsService(requestRepository, memberRepository).getStats();

        assertThat(stats.total()).isEqualTo(10L);
        assertThat(stats.completed()).isEqualTo(4L);
        assertThat(stats.completionRate()).isEqualTo(0.4);
        assertThat(stats.byCategory())
                .containsEntry(RequestCategory.BUG, 6L)
                .containsEntry(RequestCategory.INQUIRY, 0L);
        assertThat(stats.byDeveloper()).singleElement().satisfies(developerStat -> {
            assertThat(developerStat.developerId()).isEqualTo(5L);
            assertThat(developerStat.developerName()).isEqualTo("Dev One");
            assertThat(developerStat.assignedCount()).isEqualTo(3L);
            assertThat(developerStat.doneCount()).isEqualTo(2L);
        });
    }

    private static RequestRepository.CategoryCount categoryCount(RequestCategory category, long total) {
        return new RequestRepository.CategoryCount() {
            @Override
            public RequestCategory getCategory() {
                return category;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static void setId(Member member, Long id) {
        try {
            var field = Member.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
