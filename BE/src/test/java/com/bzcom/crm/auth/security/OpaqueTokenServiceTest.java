package com.bzcom.crm.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpaqueTokenServiceTest {

    private final OpaqueTokenService tokenService = new OpaqueTokenService();

    @Test
    void generatesUrlSafeHighEntropyTokens() {
        String first = tokenService.generate();
        String second = tokenService.generate();

        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void hashesTokensToDeterministicSha256HexWithoutKeepingRawValue() {
        String rawToken = "refresh-token-value";

        String hash = tokenService.hash(rawToken);

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(rawToken);
        assertThat(tokenService.hash(rawToken)).isEqualTo(hash);
    }
}
