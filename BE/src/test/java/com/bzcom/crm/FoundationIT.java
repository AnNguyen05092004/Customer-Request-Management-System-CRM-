package com.bzcom.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoundationIT {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flywayCreatesTheFiveContractTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('members', 'requests', 'request_histories', 'alerts', 'refresh_tokens')
                ORDER BY table_name
                """,
                String.class);

        assertThat(tables).containsExactly("alerts", "members", "refresh_tokens", "request_histories", "requests");
    }

    @Test
    void flywayEnforcesRequiredNonBlankAlertMessages() {
        String nullable = jdbcTemplate.queryForObject(
                """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'alerts'
                  AND column_name = 'message'
                """,
                String.class);
        Integer constraintCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'alerts'
                  AND constraint_name = 'chk_alerts_message_not_blank'
                  AND constraint_type = 'CHECK'
                """,
                Integer.class);

        assertThat(nullable).isEqualTo("NO");
        assertThat(constraintCount).isEqualTo(1);
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void openApiEndpointIsPublicAndContainsProjectMetadata() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bzcom CRM — Customer Request Management System API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    @Test
    void runtimeOpenApiOperationsAndStatusesMatchCanonicalContract() throws Exception {
        String runtimeJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode runtime = objectMapper.readTree(runtimeJson);
        Path canonicalPath = resolveCanonicalOpenApiPath();
        JsonNode canonical = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(canonicalPath));

        assertThat(extractOperationsAndStatuses(runtime))
                .as("runtime /v3/api-docs must match %s", canonicalPath)
                .isEqualTo(extractOperationsAndStatuses(canonical));
    }

    @Test
    void businessEndpointsDenyAnonymousRequestsWithCanonicalEnvelope() throws Exception {
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private static Path resolveCanonicalOpenApiPath() {
        List<Path> candidates = List.of(Path.of("..", "docs", "openapi.yaml"), Path.of("docs", "openapi.yaml"));
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate canonical docs/openapi.yaml"));
    }

    private static Map<String, Set<String>> extractOperationsAndStatuses(JsonNode openApi) {
        Map<String, Set<String>> operations = new TreeMap<>();
        openApi.path("paths")
                .properties()
                .forEach(pathEntry -> pathEntry.getValue().properties().forEach(methodEntry -> {
                    String method = methodEntry.getKey().toLowerCase();
                    if (!HTTP_METHODS.contains(method)) {
                        return;
                    }
                    Set<String> statuses = new TreeSet<>();
                    methodEntry
                            .getValue()
                            .path("responses")
                            .propertyStream()
                            .forEach(response -> statuses.add(response.getKey()));
                    operations.put(method.toUpperCase() + " " + pathEntry.getKey(), statuses);
                }));
        return operations;
    }
}
