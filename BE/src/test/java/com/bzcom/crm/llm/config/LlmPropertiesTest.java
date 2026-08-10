package com.bzcom.crm.llm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LlmPropertiesTest {

    @Test
    void defaultsNonPositiveTimeoutToFiveSeconds() {
        LlmProperties properties = new LlmProperties(true, "gemini", "gemini-2.5-flash", 0, "key");
        assertThat(properties.timeoutMs()).isEqualTo(5000);
    }

    @Test
    void keepsPositiveTimeoutAsGiven() {
        LlmProperties properties = new LlmProperties(true, "gemini", "gemini-2.5-flash", 8000, "key");
        assertThat(properties.timeoutMs()).isEqualTo(8000);
    }

    @Test
    void rejectsMissingModelWhenEnabled() {
        assertThatThrownBy(() -> new LlmProperties(true, "gemini", " ", 5000, "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM model must be configured when LLM is enabled");
    }

    @Test
    void rejectsMissingApiKeyWhenEnabled() {
        assertThatThrownBy(() -> new LlmProperties(true, "gemini", "gemini-2.5-flash", 5000, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("GEMINI_API_KEY must be configured when LLM is enabled");
    }
}
