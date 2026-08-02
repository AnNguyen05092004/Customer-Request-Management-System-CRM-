package com.bzcom.crm.auth.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshTokenPropertiesTest {

    @Test
    void rejectsMissingOrNonPositiveTtl() {
        assertThatThrownBy(() -> new RefreshTokenProperties(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ofDays(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
