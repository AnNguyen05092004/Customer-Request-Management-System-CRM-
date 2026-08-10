package com.bzcom.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import com.bzcom.crm.member.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMemberIT {

    private static final String TEST_JWT_SECRET = "test-only-secret-with-at-least-thirty-two-characters";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberService memberService;

    private ExecutorService executor;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM alerts");
        jdbcTemplate.update("DELETE FROM request_histories");
        jdbcTemplate.update("DELETE FROM requests");
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    void runtimeOpenApiPublishesAllMemberAndAuthOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.0.1"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/members'].post").exists())
                .andExpect(jsonPath("$.paths['/api/members'].get").exists())
                .andExpect(jsonPath("$.paths['/api/members/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.tags[0]").value("Auth"))
                .andExpect(jsonPath("$.paths['/api/members'].post.tags[0]").value("Member"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/api/auth/refresh'].post.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/api/members'].post.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/api/members'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/members'].post.responses['201']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/members'].post.responses['200']")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/members'].post.responses['400']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/members'].post.responses['409']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/members'].post.requestBody.content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/members'].post.responses['201'].content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.MemberCreateRequest.additionalProperties")
                        .value(false))
                .andExpect(jsonPath("$.components.schemas.RefreshTokenRequest.properties.refreshToken.maxLength")
                        .value(512));
    }

    @AfterEach
    void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void registerCreatesNormalizedClientWithBcryptPasswordAndNoPasswordInResponse() throws Exception {
        mockMvc.perform(
                        post("/api/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email":"  Alice@Example.COM ","password":"secret12","name":" Alice "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.role").value("CLIENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        Member stored = memberRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("secret12");
        assertThat(passwordEncoder.matches("secret12", stored.getPassword())).isTrue();
    }

    @Test
    void registerRejectsPrivilegeEscalationAndDuplicateEmail() throws Exception {
        register("duplicate@example.com", "First User");

        mockMvc.perform(
                        post("/api/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email":"attacker@example.com","password":"secret12","name":"Attacker","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(
                        post("/api/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email":"DUPLICATE@example.com","password":"secret12","name":"Second User"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void loginReturnsUsableAccessTokenAndRejectsWrongPassword() throws Exception {
        register("client@example.com", "Client");

        TokenPair tokenPair = login("client@example.com", "secret12");
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).hasSizeGreaterThanOrEqualTo(32);
        String storedTokenHash = jdbcTemplate.queryForObject("SELECT token_hash FROM refresh_tokens", String.class);
        assertThat(storedTokenHash).hasSize(64).isNotEqualTo(tokenPair.refreshToken());

        Long memberId =
                memberRepository.findByEmail("client@example.com").orElseThrow().getId();
        mockMvc.perform(get("/api/members/{id}", memberId).header("Authorization", "Bearer " + tokenPair.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("client@example.com"));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email":"client@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void expiredAccessTokenIsRejectedByProtectedEndpoint() throws Exception {
        Instant expiredAt = Instant.now().minusSeconds(60);
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("role", "CLIENT")
                .issuedAt(Date.from(expiredAt.minusSeconds(60)))
                .expiration(Date.from(expiredAt))
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/members/{id}", 1).header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void memberReadEnforcesAdminAndSelfPermissions() throws Exception {
        register("first@example.com", "First");
        register("second@example.com", "Second");
        TokenPair firstToken = login("first@example.com", "secret12");
        Long secondId =
                memberRepository.findByEmail("second@example.com").orElseThrow().getId();

        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + firstToken.accessToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/members/{id}", secondId)
                        .header("Authorization", "Bearer " + firstToken.accessToken()))
                .andExpect(status().isForbidden());

        memberRepository.saveAndFlush(
                new Member("admin@example.com", passwordEncoder.encode("secret12"), "Admin", MemberRole.ADMIN));
        TokenPair adminToken = login("admin@example.com", "secret12");
        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
        mockMvc.perform(get("/api/members/{id}", secondId)
                        .header("Authorization", "Bearer " + adminToken.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/members/{id}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + adminToken.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Member not found"));
    }

    @Test
    void workflowContractRecordsDeveloperCompletion() {
        Member developer = memberRepository.saveAndFlush(new Member(
                "developer@example.com", passwordEncoder.encode("secret12"), "Developer", MemberRole.DEVELOPER));
        Instant completedAt = Instant.parse("2026-08-02T02:00:00Z");

        memberService.recordDeveloperCompletion(developer.getId(), completedAt);

        assertThat(memberRepository.findById(developer.getId()).orElseThrow().getLastCompletedAt())
                .isEqualTo(completedAt);
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesIdempotently() throws Exception {
        register("client@example.com", "Client");
        TokenPair original = login("client@example.com", "secret12");

        TokenPair rotated = refresh(original.refreshToken(), 200);
        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());
        refresh(original.refreshToken(), 401);

        logout(rotated.refreshToken());
        logout(rotated.refreshToken());
        refresh(rotated.refreshToken(), 401);
    }

    @Test
    void refreshRejectsUnknownAndExpiredTokens() throws Exception {
        refresh("unknown-token-with-at-least-thirty-two-characters", 401);

        register("client@example.com", "Client");
        String refreshToken = login("client@example.com", "secret12").refreshToken();
        jdbcTemplate.update(
                """
                UPDATE refresh_tokens
                   SET created_at = now() - interval '2 days',
                       expires_at = now() - interval '1 day'
                 WHERE revoked_at IS NULL
                """);
        refresh(refreshToken, 401);
    }

    @Test
    void refreshRejectsOversizedToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody("x".repeat(513)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void concurrentRefreshAllowsExactlyOneRotation() throws Exception {
        register("client@example.com", "Client");
        String refreshToken = login("client@example.com", "secret12").refreshToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        executor = Executors.newFixedThreadPool(2);

        List<Future<Integer>> futures = List.of(
                executor.submit(() -> refreshConcurrently(refreshToken, ready, start)),
                executor.submit(() -> refreshConcurrently(refreshToken, ready, start)));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Integer> statuses =
                List.of(futures.get(0).get(5, TimeUnit.SECONDS), futures.get(1).get(5, TimeUnit.SECONDS));
        assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class))
                .isEqualTo(1);
    }

    private int refreshConcurrently(String refreshToken, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void register(String email, String name) throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterBody(email, "secret12", name))))
                .andExpect(status().isCreated());
    }

    private TokenPair login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").exists())
                .andReturn();
        JsonNode data =
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new TokenPair(
                data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private TokenPair refresh(String refreshToken, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        if (expectedStatus != 200) {
            return null;
        }
        JsonNode data =
                objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new TokenPair(
                data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private void logout(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private record RegisterBody(String email, String password, String name) {}

    private record LoginBody(String email, String password) {}

    private record RefreshBody(String refreshToken) {}

    private record TokenPair(String accessToken, String refreshToken) {}
}
