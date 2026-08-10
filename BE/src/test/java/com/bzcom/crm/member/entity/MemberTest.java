package com.bzcom.crm.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bzcom.crm.member.domain.MemberRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void recordsCompletionOnlyForDeveloper() {
        Instant completedAt = Instant.parse("2026-08-02T02:00:00Z");
        Member developer = new Member("dev@example.com", "hash", "Dev", MemberRole.DEVELOPER);
        Member client = new Member("client@example.com", "hash", "Client", MemberRole.CLIENT);

        developer.recordCompletion(completedAt);

        assertThat(developer.getLastCompletedAt()).isEqualTo(completedAt);
        assertThatThrownBy(() -> client.recordCompletion(completedAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a developer can record request completion");
        assertThatThrownBy(() -> developer.recordCompletion(null)).isInstanceOf(NullPointerException.class);
    }
}
