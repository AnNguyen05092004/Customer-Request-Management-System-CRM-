package com.bzcom.crm.llm;

import com.bzcom.crm.llm.dto.ClassifyResult;
import com.bzcom.crm.llm.dto.PriorityResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "true")
public class OpenAiLlmService implements LlmService {

    private final RestClient restClient;
    private final String model;

    public OpenAiLlmService(@Value("${llm.openai.api-key}") String apiKey,
                             @Value("${llm.openai.model:gpt-4o-mini}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public ClassifyResult classify(String description) {
        String prompt = """
            Phân loại yêu cầu sau vào một trong 3 loại: BUG, FEATURE, INQUIRY.
            Trả lời CHỈ bằng JSON: {"category": "...", "confidence": 0.0, "reasoning": "..."}
            Mô tả: %s
            """.formatted(description);
        return callAndParse(prompt, ClassifyResult.class);
    }

    @Override
    public PriorityResult suggestPriority(String description) {
        String prompt = """
            Đánh giá mức độ ưu tiên của yêu cầu sau: HIGH, MEDIUM, hoặc LOW.
            Trả lời CHỈ bằng JSON: {"priority": "...", "confidence": 0.0, "reasoning": "..."}
            Mô tả: %s
            """.formatted(description);
        return callAndParse(prompt, PriorityResult.class);
    }

    @Override
    public String summarize(String description) {
        String prompt = "Tóm tắt yêu cầu sau trong 1-2 câu ngắn gọn: " + description;
        return callChat(prompt);
    }

    private String callChat(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", java.util.List.of(Map.of("role", "user", "content", prompt))
        );
        Map<?, ?> response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(Map.class);
        var choices = (java.util.List<?>) response.get("choices");
        var message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        return (String) message.get("content");
    }

    @SuppressWarnings("unchecked")
    private <T> T callAndParse(String prompt, Class<T> type) {
        String raw = callChat(prompt);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(raw, type);
        } catch (Exception e) {
            throw new IllegalStateException("Không parse được phản hồi LLM: " + raw, e);
        }
    }
}