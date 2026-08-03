package com.bzcom.crm.llm.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bzcom.crm.auth.jwt.JwtAuthenticationFilter;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.service.LlmService;
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    @WithMockUser
    void classifyReturnsLlmResult() throws Exception {
        given(llmService.classify("500 error")).willReturn(new ClassifyResult(Category.BUG, 0.9, "error keyword"));

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
                .willReturn(new PriorityResult(Priority.HIGH, 0.9, "payment keyword"));

        mockMvc.perform(post("/api/requests/suggest-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "Lỗi thanh toán"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }
}
