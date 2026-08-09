package com.bzcom.crm.request.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bzcom.crm.auth.jwt.JwtAuthenticationFilter;
import com.bzcom.crm.request.dto.response.StatsResponse;
import com.bzcom.crm.request.service.RequestService;
import com.bzcom.crm.request.service.RequestStatsService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Note: {@code @PreAuthorize} role enforcement is NOT active in this {@code @WebMvcTest} slice
 * (SecurityConfig/@EnableMethodSecurity is not loaded here), so ADMIN-only access for
 * {@code GET /api/requests/stats} is not covered by a test in this class — see docs/TASKS.md
 * T-3.3 status note.
 */
@WebMvcTest(
        controllers = RequestController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class RequestStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestService requestService;

    @MockitoBean
    private RequestStatsService requestStatsService;

    @Test
    @WithMockUser
    void statsReturnsAggregatedNumbers() throws Exception {
        given(requestStatsService.getStats()).willReturn(new StatsResponse(10, 4, 0.4, Map.of(), List.of()));

        mockMvc.perform(get("/api/requests/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.completed").value(4))
                .andExpect(jsonPath("$.data.completionRate").value(0.4));
    }
}
