package com.bzcom.crm.llm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import org.junit.jupiter.api.Test;

class MockLlmServiceTest {

    private final MockLlmService service = new MockLlmService();

    @Test
    void classifiesErrorKeywordsAsBug() {
        assertThat(service.classify("Page shows 500 error").category()).isEqualTo(RequestCategory.BUG);
        assertThat(service.classify("Có lỗi khi đăng nhập").category()).isEqualTo(RequestCategory.BUG);
    }

    @Test
    void classifiesFeatureKeywordsAsFeature() {
        assertThat(service.classify("Mong thêm đăng nhập bằng Google").category())
                .isEqualTo(RequestCategory.FEATURE);
    }

    @Test
    void classifiesEverythingElseAsInquiry() {
        assertThat(service.classify("Cho tôi hỏi cách đổi mật khẩu").category()).isEqualTo(RequestCategory.INQUIRY);
    }

    @Test
    void suggestsHighPriorityForSecurityAndPaymentKeywords() {
        assertThat(service.suggestPriority("Lỗi thanh toán không chạy").priority())
                .isEqualTo(RequestPriority.HIGH);
    }

    @Test
    void suggestsLowPriorityForCosmeticQuestions() {
        assertThat(service.suggestPriority("Cho tôi hỏi về giao diện").priority())
                .isEqualTo(RequestPriority.LOW);
    }

    @Test
    void suggestsMediumPriorityByDefault() {
        assertThat(service.suggestPriority("Trang danh sách tải chậm").priority())
                .isEqualTo(RequestPriority.MEDIUM);
    }

    @Test
    void summarizeTruncatesLongDescription() {
        String longDescription = "x".repeat(200);
        String summary = service.summarize(new RequestSummaryInput(longDescription));
        assertThat(summary).startsWith("[mock] ").hasSize(7 + 150 + 3);
    }

    @Test
    void summarizeHandlesMissingDescription() {
        assertThat(service.summarize(new RequestSummaryInput(null))).isEqualTo(RequestSummaryInput.EMPTY_SUMMARY);
        assertThat(service.summarize(new RequestSummaryInput("  "))).isEqualTo(RequestSummaryInput.EMPTY_SUMMARY);
    }
}
