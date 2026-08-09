package com.bzcom.crm.llm;

import com.bzcom.crm.llm.dto.ClassifyResult;
import com.bzcom.crm.llm.dto.PriorityResult;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "false", matchIfMissing = true)
public class MockLlmService implements LlmService {

    @Override
    public ClassifyResult classify(String description) {
        String normalized = normalize(description);
        if (containsAny(normalized, "error", "bug", "lỗi", "500")) {
            return new ClassifyResult("BUG", 0.9, "[mock] error keyword");
        }
        if (containsAny(normalized, "thêm", "feature", "google")) {
            return new ClassifyResult("FEATURE", 0.9, "[mock] feature keyword");
        }
        return new ClassifyResult("INQUIRY", 0.8, "[mock] default");
    }

    @Override
    public PriorityResult suggestPriority(String description) {
        String normalized = normalize(description);
        if (containsAny(normalized, "urgent", "khẩn", "production", "500")) {
            return new PriorityResult("HIGH", 0.9, "[mock] urgent keyword");
        }
        return new PriorityResult("MEDIUM", 0.7, "[mock] default");
    }

    @Override
    public String summarize(String description) {
        String normalized = description == null ? "" : description.trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 157) + "...";
    }

    private String normalize(String description) {
        return description == null ? "" : description.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String description, String... keywords) {
        for (String keyword : keywords) {
            if (description.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
