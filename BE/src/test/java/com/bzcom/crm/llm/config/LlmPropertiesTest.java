package com.bzcom.crm.llm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmPropertiesTest {

    @Test
    void defaultsNonPositiveTimeoutToFiveSeconds() {
        LlmProperties properties = new LlmProperties(true, "openai", "gpt-4o-mini", 0, "key");
        assertThat(properties.timeoutMs()).isEqualTo(5000);
    }

    @Test
    void keepsPositiveTimeoutAsGiven() {
        LlmProperties properties = new LlmProperties(true, "openai", "gpt-4o-mini", 8000, "key");
        assertThat(properties.timeoutMs()).isEqualTo(8000);
    }
}
