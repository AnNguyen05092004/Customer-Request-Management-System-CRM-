package com.bzcom.crm.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void okCreatesCanonicalSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("payload");
    }

    @Test
    void errorKeepsDataFieldNull() {
        ApiResponse<Void> response = ApiResponse.error(org.springframework.http.HttpStatus.BAD_REQUEST, "invalid");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("invalid");
        assertThat(response.data()).isNull();
    }
}
