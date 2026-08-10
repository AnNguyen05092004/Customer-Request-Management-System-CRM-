package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.config.LlmProperties;
import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.prompt.ClassifyPrompt;
import com.bzcom.crm.llm.prompt.PriorityPrompt;
import com.bzcom.crm.llm.prompt.SummaryPrompt;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "true")
public class GeminiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public GeminiLlmService(RestClient restClient, ObjectMapper objectMapper, LlmProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.model = properties.model();
    }

    @Override
    public ClassifyResult classify(String description) {
        try {
            String content = complete(ClassifyPrompt.SYSTEM, ClassifyPrompt.userInput(description));
            JsonNode json = objectMapper.readTree(content);
            return new ClassifyResult(
                    RequestCategory.valueOf(json.get("category").asText()),
                    json.get("confidence").asDouble(),
                    json.get("reason").asText());
        } catch (Exception exception) {
            log.warn("LLM classify failed, falling back to rule-based result", exception);
            return ruleBasedClassifyFallback(description);
        }
    }

    @Override
    public PriorityResult suggestPriority(String description) {
        try {
            String content = complete(PriorityPrompt.SYSTEM, PriorityPrompt.userInput(description));
            JsonNode json = objectMapper.readTree(content);
            return new PriorityResult(
                    RequestPriority.valueOf(json.get("priority").asText()),
                    json.get("confidence").asDouble(),
                    json.get("reason").asText());
        } catch (Exception exception) {
            log.warn("LLM suggest-priority failed, falling back to rule-based result", exception);
            return ruleBasedPriorityFallback(description);
        }
    }

    @Override
    public String summarize(RequestSummaryInput request) {
        if (!request.hasDescription()) {
            return RequestSummaryInput.EMPTY_SUMMARY;
        }
        try {
            return complete(SummaryPrompt.SYSTEM, request.description()).trim();
        } catch (Exception exception) {
            log.warn("LLM summary failed, falling back to truncated description", exception);
            return truncateSummaryFallback(request.description());
        }
    }

    private String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model",
                model,
                "temperature",
                0.2,
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        JsonNode response =
                restClient.post().uri("/chat/completions").body(body).retrieve().body(JsonNode.class);
        return stripMarkdownCodeFence(response.at("/choices/0/message/content").asText());
    }

    /**
     * Gemini (unlike OpenAI) often wraps its JSON answer in a ```json ... ``` markdown fence
     * even when the prompt asks for raw JSON only; strip it so parsing doesn't fail and fall
     * back to the rule-based result on every call.
     */
    private static String stripMarkdownCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int fenceEnd = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && fenceEnd > firstNewline) {
                return trimmed.substring(firstNewline + 1, fenceEnd).trim();
            }
        }
        return trimmed;
    }

    private ClassifyResult ruleBasedClassifyFallback(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("lỗi")
                || lower.contains("error")
                || lower.contains("crash")
                || lower.contains("500")
                || lower.contains("không hoạt động")
                || lower.contains("fail")) {
            return new ClassifyResult(RequestCategory.BUG, 0.5, "keyword rule: error terms");
        }
        if (lower.contains("thêm")
                || lower.contains("mong")
                || lower.contains("đề nghị")
                || lower.contains("feature")) {
            return new ClassifyResult(RequestCategory.FEATURE, 0.5, "keyword rule: request terms");
        }
        return new ClassifyResult(RequestCategory.INQUIRY, 0.4, "fallback default");
    }

    private PriorityResult ruleBasedPriorityFallback(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("bảo mật")
                || lower.contains("security")
                || lower.contains("thanh toán")
                || lower.contains("payment")
                || lower.contains("down")
                || lower.contains("crash")) {
            return new PriorityResult(RequestPriority.HIGH, 0.5, "keyword rule: security/payment/outage terms");
        }
        if (lower.contains("hỏi")
                || lower.contains("question")
                || lower.contains("mỹ phẩm")
                || lower.contains("cosmetic")) {
            return new PriorityResult(RequestPriority.LOW, 0.4, "keyword rule: cosmetic/question terms");
        }
        return new PriorityResult(RequestPriority.MEDIUM, 0.4, "fallback default");
    }

    private static String truncateSummaryFallback(String description) {
        return description.length() > 150 ? description.substring(0, 150) + "..." : description;
    }
}
