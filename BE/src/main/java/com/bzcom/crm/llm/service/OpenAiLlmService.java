package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.config.LlmProperties;
import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
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
public class OpenAiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmService.class);

    private static final String CLASSIFY_SYSTEM_PROMPT =
            """
            Bạn là trợ lý phân loại yêu cầu hỗ trợ của Bzcom — công ty vận hành web service.
            Nhiệm vụ: phân loại mô tả của khách hàng vào ĐÚNG MỘT trong ba nhãn sau:
            - BUG:     lỗi/sự cố khi hệ thống đang chạy (error, crash, không hoạt động đúng).
            - FEATURE: đề nghị thêm hoặc cải tiến chức năng mới.
            - INQUIRY: câu hỏi/thắc mắc, không phải lỗi cũng không phải yêu cầu tính năng.

            Chỉ trả về JSON đúng định dạng sau, KHÔNG thêm bất kỳ chữ nào khác:
            {"category":"BUG|FEATURE|INQUIRY","confidence":<0.0-1.0>,"reason":"<lý do ngắn gọn>"}

            Nếu không chắc chắn, chọn nhãn khả dĩ nhất và hạ confidence xuống dưới 0.6.

            FEW-SHOT (ví dụ mẫu):
            Input: "Nút thanh toán bấm không phản hồi"
            Output: {"category":"BUG","confidence":0.95,"reason":"chức năng không hoạt động"}
            Input: "Cho tôi hỏi cách đổi mật khẩu?"
            Output: {"category":"INQUIRY","confidence":0.90,"reason":"là câu hỏi hướng dẫn"}
            Input: "Mong thêm đăng nhập bằng Google"
            Output: {"category":"FEATURE","confidence":0.92,"reason":"đề nghị tính năng mới"}
            """;

    private static final String PRIORITY_SYSTEM_PROMPT =
            """
            Bạn là trợ lý gợi ý mức độ ưu tiên cho yêu cầu hỗ trợ của Bzcom.
            HIGH = ảnh hưởng nhiều người dùng / chặn nghiệp vụ / liên quan bảo mật-thanh toán.
            MEDIUM = ảnh hưởng một phần.
            LOW = mỹ phẩm/hỏi đáp.

            Chỉ trả về JSON đúng định dạng sau, KHÔNG thêm bất kỳ chữ nào khác:
            {"priority":"HIGH|MEDIUM|LOW","confidence":<0.0-1.0>,"reason":"<lý do ngắn gọn>"}
            """;

    private static final String SUMMARY_SYSTEM_PROMPT =
            "Tóm tắt yêu cầu trong tối đa 2 câu, nêu vấn đề chính và mức khẩn cấp, "
                    + "không thêm thông tin không có trong mô tả.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiLlmService(RestClient restClient, ObjectMapper objectMapper, LlmProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.model = properties.model();
    }

    @Override
    public ClassifyResult classify(String description) {
        try {
            String content = complete(CLASSIFY_SYSTEM_PROMPT, "Input: \"" + description + "\"\nOutput:");
            JsonNode json = objectMapper.readTree(content);
            return new ClassifyResult(
                    Category.valueOf(json.get("category").asText()),
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
            String content = complete(PRIORITY_SYSTEM_PROMPT, "Input: \"" + description + "\"\nOutput:");
            JsonNode json = objectMapper.readTree(content);
            return new PriorityResult(
                    Priority.valueOf(json.get("priority").asText()),
                    json.get("confidence").asDouble(),
                    json.get("reason").asText());
        } catch (Exception exception) {
            log.warn("LLM suggest-priority failed, falling back to rule-based result", exception);
            return ruleBasedPriorityFallback(description);
        }
    }

    @Override
    public String summarize(RequestSummaryInput request) {
        try {
            return complete(SUMMARY_SYSTEM_PROMPT, request.description()).trim();
        } catch (Exception exception) {
            log.warn("LLM summary failed, falling back to truncated description", exception);
            return truncateSummaryFallback(request.description());
        }
    }

    private String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages",
                        List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt)));
        JsonNode response =
                restClient.post().uri("/chat/completions").body(body).retrieve().body(JsonNode.class);
        return response.at("/choices/0/message/content").asText();
    }

    private ClassifyResult ruleBasedClassifyFallback(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("lỗi")
                || lower.contains("error")
                || lower.contains("crash")
                || lower.contains("500")
                || lower.contains("không hoạt động")
                || lower.contains("fail")) {
            return new ClassifyResult(Category.BUG, 0.5, "keyword rule: error terms");
        }
        if (lower.contains("thêm")
                || lower.contains("mong")
                || lower.contains("đề nghị")
                || lower.contains("feature")) {
            return new ClassifyResult(Category.FEATURE, 0.5, "keyword rule: request terms");
        }
        return new ClassifyResult(Category.INQUIRY, 0.4, "fallback default");
    }

    private PriorityResult ruleBasedPriorityFallback(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("bảo mật")
                || lower.contains("security")
                || lower.contains("thanh toán")
                || lower.contains("payment")
                || lower.contains("down")
                || lower.contains("crash")) {
            return new PriorityResult(Priority.HIGH, 0.5, "keyword rule: security/payment/outage terms");
        }
        if (lower.contains("hỏi") || lower.contains("question") || lower.contains("mỹ phẩm") || lower.contains("cosmetic")) {
            return new PriorityResult(Priority.LOW, 0.4, "keyword rule: cosmetic/question terms");
        }
        return new PriorityResult(Priority.MEDIUM, 0.4, "fallback default");
    }

    private String truncateSummaryFallback(String description) {
        return description.length() > 150 ? description.substring(0, 150) + "..." : description;
    }
}
