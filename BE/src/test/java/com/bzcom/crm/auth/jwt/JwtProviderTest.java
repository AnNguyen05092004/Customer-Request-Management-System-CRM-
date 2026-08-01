package com.bzcom.crm.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.member.domain.MemberRole;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private final JwtProvider provider = new JwtProvider(
            new JwtProperties("unit-test-secret-with-at-least-thirty-two-characters", Duration.ofMinutes(15)));

    @Test
    void roundTripsMemberIdentityAndRole() {
        String token = provider.generateAccessToken(42L, MemberRole.ADMIN);

        CurrentUser principal = provider.parseAccessToken(token);

        assertThat(principal.memberId()).isEqualTo(42L);
        assertThat(principal.role()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void rejectsTamperedToken() {
        String token = provider.generateAccessToken(42L, MemberRole.CLIENT);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> provider.parseAccessToken(tampered)).isInstanceOf(JwtException.class);
    }
}
