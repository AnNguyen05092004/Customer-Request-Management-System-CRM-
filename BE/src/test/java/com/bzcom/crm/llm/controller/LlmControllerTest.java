package com.bzcom.crm.llm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bzcom.crm.auth.jwt.JwtAuthenticationFilter;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.service.LlmService;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.service.RequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LlmController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class LlmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LlmService llmService;

    @MockitoBean
    private RequestService requestService;

    @Test
    @WithMockUser
    void classifyReturnsLlmResult() throws Exception {
        given(llmService.classify("500 error"))
                .willReturn(new ClassifyResult(RequestCategory.BUG, 0.9, "error keyword"));

        mockMvc.perform(post("/api/requests/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "500 error"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("BUG"))
                .andExpect(jsonPath("$.data.confidence").value(0.9));
    }

    @Test
    @WithMockUser
    void classifyRejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/api/requests/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void suggestPriorityReturnsLlmResult() throws Exception {
        given(llmService.suggestPriority("Lỗi thanh toán"))
                .willReturn(new PriorityResult(RequestPriority.HIGH, 0.9, "payment keyword"));

        mockMvc.perform(post("/api/requests/suggest-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "Lỗi thanh toán"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    @WithMockUser
    void summaryReturnsLlmSummaryOfAnAccessibleRequest() throws Exception {
        RequestResponse request = new RequestResponse(
                1L,
                "Login fails",
                "500 error when clicking login",
                RequestCategory.BUG,
                RequestPriority.HIGH,
                RequestStatus.PENDING,
                2L,
                null,
                0,
                Instant.now(),
                Instant.now());
        given(requestService.getRequestDetail(eq(1L), any())).willReturn(request);
        given(llmService.summarize(any())).willReturn("Người dùng gặp lỗi 500 khi đăng nhập.");

        mockMvc.perform(get("/api/requests/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("Người dùng gặp lỗi 500 khi đăng nhập."));
    }
}
