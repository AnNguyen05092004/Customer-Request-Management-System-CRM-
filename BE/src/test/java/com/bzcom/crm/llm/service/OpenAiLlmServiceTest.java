package com.bzcom.crm.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bzcom.crm.llm.config.LlmProperties;
import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiLlmServiceTest {

    private final LlmProperties properties = new LlmProperties(true, "openai", "gpt-4o-mini", 5000, "test-key");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void classifyParsesLlmJsonResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":"
                                + "\"{\\\"category\\\":\\\"BUG\\\",\\\"confidence\\\":0.95,"
                                + "\\\"reason\\\":\\\"error mentioned\\\"}\"}}]}",
                        MediaType.APPLICATION_JSON));
        OpenAiLlmService service = new OpenAiLlmService(restClient, objectMapper, properties);

        ClassifyResult result = service.classify("500 error on login");

        assertThat(result.category()).isEqualTo(RequestCategory.BUG);
        assertThat(result.confidence()).isEqualTo(0.95);
        server.verify();
    }

    @Test
    void classifyFallsBackToRuleBasedResultWhenLlmCallFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        OpenAiLlmService service = new OpenAiLlmService(restClient, objectMapper, properties);

        ClassifyResult result = service.classify("Page shows 500 error");

        assertThat(result.category()).isEqualTo(RequestCategory.BUG);
        assertThat(result.reason()).isEqualTo("keyword rule: error terms");
    }

    @Test
    void suggestPriorityFallsBackWhenLlmCallFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        OpenAiLlmService service = new OpenAiLlmService(restClient, objectMapper, properties);

        PriorityResult result = service.suggestPriority("Lỗi thanh toán");

        assertThat(result.priority()).isEqualTo(RequestPriority.HIGH);
    }

    @Test
    void summarizeFallsBackToTruncatedDescriptionWhenLlmCallFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        OpenAiLlmService service = new OpenAiLlmService(restClient, objectMapper, properties);

        String summary = service.summarize(new RequestSummaryInput("short description"));

        assertThat(summary).isEqualTo("short description");
    }
}
