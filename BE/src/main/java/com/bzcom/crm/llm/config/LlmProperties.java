package com.bzcom.crm.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(boolean enabled, String provider, String model, long timeoutMs, String apiKey) {

    public LlmProperties {
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
        if (enabled && (model == null || model.isBlank())) {
            throw new IllegalArgumentException("LLM model must be configured when LLM is enabled");
        }
        if (enabled && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException("GEMINI_API_KEY must be configured when LLM is enabled");
        }
    }
}
