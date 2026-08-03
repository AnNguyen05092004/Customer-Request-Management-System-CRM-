package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "false", matchIfMissing = true)
public class MockLlmService implements LlmService {

    @Override
    public ClassifyResult classify(String description) {
        String d = description.toLowerCase();
        if (d.contains("error") || d.contains("lỗi") || d.contains("500")) {
            return new ClassifyResult(Category.BUG, 0.9, "[mock] error keyword");
        }
        if (d.contains("thêm") || d.contains("feature") || d.contains("google")) {
            return new ClassifyResult(Category.FEATURE, 0.9, "[mock] feature keyword");
        }
        return new ClassifyResult(Category.INQUIRY, 0.8, "[mock] default");
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
            return new PriorityResult(Priority.HIGH, 0.9, "[mock] security/payment/outage keyword");
        }
        if (d.contains("hỏi")
                || d.contains("question")
                || d.contains("mỹ phẩm")
                || d.contains("cosmetic")
                || d.contains("giao diện")) {
            return new PriorityResult(Priority.LOW, 0.85, "[mock] cosmetic/question keyword");
        }
        return new PriorityResult(Priority.MEDIUM, 0.7, "[mock] default");
    }

    @Override
    public String summarize(RequestSummaryInput request) {
        String description = request.description();
        String trimmed = description.length() > 150 ? description.substring(0, 150) + "..." : description;
        return "[mock] " + trimmed;
    }
}
