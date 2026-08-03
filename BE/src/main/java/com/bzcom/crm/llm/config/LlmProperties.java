package com.bzcom.crm.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(boolean enabled, String provider, String model, long timeoutMs, String apiKey) {

    public LlmProperties {
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
    }
}
