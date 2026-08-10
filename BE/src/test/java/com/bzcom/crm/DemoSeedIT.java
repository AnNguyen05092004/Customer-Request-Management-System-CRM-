package com.bzcom.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DemoSeedIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void demoProfileSeedsLoginReadyMembersAndRequests() throws Exception {
        Map<String, String> expectedRoles = Map.of(
                "admin@bzcom.com", "ADMIN",
                "dev1@bzcom.com", "DEVELOPER",
                "dev2@bzcom.com", "DEVELOPER",
                "client1@bzcom.com", "CLIENT");

        String adminAccessToken = null;
        for (Map.Entry<String, String> account : expectedRoles.entrySet()) {
            MvcResult login = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginBody(account.getKey(), "1234"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.role").value(account.getValue()))
                    .andReturn();
            if ("ADMIN".equals(account.getValue())) {
                JsonNode data = objectMapper
                        .readTree(login.getResponse().getContentAsString())
                        .path("data");
                adminAccessToken = data.path("accessToken").asText();
            }
        }

        assertThat(adminAccessToken).isNotBlank();
        mockMvc.perform(get("/api/requests").param("size", "10").header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    private record LoginBody(String email, String password) {}
}
