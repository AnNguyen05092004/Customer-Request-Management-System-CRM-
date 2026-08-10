package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "false", matchIfMissing = true)
public class MockLlmService implements LlmService {

    @Override
    public ClassifyResult classify(String description) {
        String d = description.toLowerCase();
        if (d.contains("error") || d.contains("lỗi") || d.contains("500")) {
            return new ClassifyResult(RequestCategory.BUG, 0.9, "[mock] error keyword");
        }
        if (d.contains("thêm") || d.contains("feature") || d.contains("google")) {
            return new ClassifyResult(RequestCategory.FEATURE, 0.9, "[mock] feature keyword");
        }
        return new ClassifyResult(RequestCategory.INQUIRY, 0.8, "[mock] default");
    }

    @Override
    public PriorityResult suggestPriority(String description) {
        String d = description.toLowerCase();
        if (d.contains("bảo mật")
                || d.contains("security")
                || d.contains("thanh toán")
                || d.contains("payment")
                || d.contains("down")
                || d.contains("crash")) {
            return new PriorityResult(RequestPriority.HIGH, 0.9, "[mock] security/payment/outage keyword");
        }
        if (d.contains("hỏi")
                || d.contains("question")
                || d.contains("mỹ phẩm")
                || d.contains("cosmetic")
                || d.contains("giao diện")) {
            return new PriorityResult(RequestPriority.LOW, 0.85, "[mock] cosmetic/question keyword");
        }
        return new PriorityResult(RequestPriority.MEDIUM, 0.7, "[mock] default");
    }

    @Override
    public String summarize(RequestSummaryInput request) {
        if (!request.hasDescription()) {
            return RequestSummaryInput.EMPTY_SUMMARY;
        }
        String description = request.description();
        String trimmed = description.length() > 150 ? description.substring(0, 150) + "..." : description;
        return "[mock] " + trimmed;
    }
}
