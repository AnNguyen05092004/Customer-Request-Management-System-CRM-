# LLM Classify & Suggest-Priority Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement T-3.1 (`LlmService` interface + `MockLlmService`) and the classify/suggest-priority half of T-3.2 (`OpenAiLlmService` + `LlmController`) from `docs/TASKS.md`, owned by Member E.

**Architecture:** `LlmService` interface with two implementations selected by `llm.enabled` (`MockLlmService` default, `OpenAiLlmService` when true, calling OpenAI Chat Completions via Spring `RestClient` with a rule-based fallback on any failure). `LlmController` exposes `POST /api/requests/classify` and `POST /api/requests/suggest-priority` per `docs/openapi.yaml`. `Category`/`Priority` enums are created under `request/domain/` as a shared prerequisite (mirrors the existing precedent of `RequestStatus` living in `workflow/domain/` even though it's a `Request` field) — Member B will reuse them when building the `Request` entity in T-2.B1.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring `RestClient` (no new Maven dependency — ships in `spring-boot-starter-web`), Jackson, JUnit 5 + AssertJ + Mockito, `MockRestServiceServer`, `@WebMvcTest`.

## Global Constraints

- HTTP contract is `docs/openapi.yaml` — do not deviate from path/method/status/schema (`/api/requests/classify`, `/api/requests/suggest-priority`, both `POST`, body `{description}`, response `ApiResponse<data>`).
- Prompts must match `docs/LLM.md` §3–4 verbatim (purpose + context + JSON output format + few-shot).
- `llm.enabled=false` (default) must never call the network — `MockLlmService` only.
- No secrets in code; `OPENAI_API_KEY` read from env via `LlmProperties`.
- Package-by-feature: controller → service → repository → entity; no business logic in controller.
- Every task ends green on `cd BE && ./mvnw -B verify` before moving to the next.

## Out of scope (blocked, not part of this plan)

- `GET /api/requests/{id}/summary` and `POST /api/requests/stats` — both require the `Request` entity/repository (T-2.B1, owner B), which does not exist in the codebase yet (`request/` package is an empty `package-info.java`). `LlmService.summarize(RequestSummaryInput)` IS implemented here (it only needs a description string, not the entity), so wiring the summary endpoint later is a small follow-up once B merges T-2.B1. Statistics API (T-3.3) is fully blocked until then and is a separate plan.

---

### Task 1: Category & Priority enums

**Files:**
- Create: `BE/src/main/java/com/bzcom/crm/request/domain/Category.java`
- Create: `BE/src/main/java/com/bzcom/crm/request/domain/Priority.java`

**Interfaces:**
- Produces: `com.bzcom.crm.request.domain.Category` enum `{BUG, FEATURE, INQUIRY}`; `com.bzcom.crm.request.domain.Priority` enum `{HIGH, MEDIUM, LOW}` — used by every later task in this plan.

- [ ] **Step 1: Create the enums (no test needed — plain enums, no behavior)**

```java
package com.bzcom.crm.request.domain;

public enum Category {
    BUG,
    FEATURE,
    INQUIRY
}
```

```java
package com.bzcom.crm.request.domain;

public enum Priority {
    HIGH,
    MEDIUM,
    LOW
}
```

- [ ] **Step 2: Compile**

Run: `cd BE && ./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add BE/src/main/java/com/bzcom/crm/request/domain
git commit -m "feat: add Category and Priority enums"
```

---

### Task 2: LlmService interface + DTOs

**Files:**
- Create: `BE/src/main/java/com/bzcom/crm/llm/dto/response/ClassifyResult.java`
- Create: `BE/src/main/java/com/bzcom/crm/llm/dto/response/PriorityResult.java`
- Create: `BE/src/main/java/com/bzcom/crm/llm/dto/request/DescriptionRequest.java`
- Create: `BE/src/main/java/com/bzcom/crm/llm/dto/request/RequestSummaryInput.java`
- Create: `BE/src/main/java/com/bzcom/crm/llm/service/LlmService.java`

**Interfaces:**
- Consumes: `Category`, `Priority` from Task 1.
- Produces: `LlmService.classify(String): ClassifyResult`, `LlmService.suggestPriority(String): PriorityResult`, `LlmService.summarize(RequestSummaryInput): String` — implemented by Task 3 and Task 5.

- [ ] **Step 1: Create the DTOs and interface**

```java
package com.bzcom.crm.llm.dto.response;

import com.bzcom.crm.request.domain.Category;

public record ClassifyResult(Category category, double confidence, String reason) {}
```

```java
package com.bzcom.crm.llm.dto.response;

import com.bzcom.crm.request.domain.Priority;

public record PriorityResult(Priority priority, double confidence, String reason) {}
```

```java
package com.bzcom.crm.llm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DescriptionRequest(@NotBlank String description) {}
```

```java
package com.bzcom.crm.llm.dto.request;

public record RequestSummaryInput(String description) {}
```

```java
package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;

public interface LlmService {

    ClassifyResult classify(String description);

    PriorityResult suggestPriority(String description);

    String summarize(RequestSummaryInput request);
}
```

- [ ] **Step 2: Compile**

Run: `cd BE && ./mvnw -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add BE/src/main/java/com/bzcom/crm/llm/dto BE/src/main/java/com/bzcom/crm/llm/service/LlmService.java
git commit -m "feat: add LlmService interface and LLM DTOs"
```

---

### Task 3: MockLlmService

**Files:**
- Create: `BE/src/main/java/com/bzcom/crm/llm/service/MockLlmService.java`
- Test: `BE/src/test/java/com/bzcom/crm/llm/service/MockLlmServiceTest.java`

**Interfaces:**
- Consumes: `LlmService` (Task 2).
- Produces: `MockLlmService`, a `@Service` active when `llm.enabled=false` (default) — this becomes the default autowired `LlmService` bean in every test/dev profile.

- [ ] **Step 1: Write the failing test**

```java
package com.bzcom.crm.llm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
import org.junit.jupiter.api.Test;

class MockLlmServiceTest {

    private final MockLlmService service = new MockLlmService();

    @Test
    void classifiesErrorKeywordsAsBug() {
        assertThat(service.classify("Page shows 500 error").category()).isEqualTo(Category.BUG);
        assertThat(service.classify("Có lỗi khi đăng nhập").category()).isEqualTo(Category.BUG);
    }

    @Test
    void classifiesFeatureKeywordsAsFeature() {
        assertThat(service.classify("Mong thêm đăng nhập bằng Google").category())
                .isEqualTo(Category.FEATURE);
    }

    @Test
    void classifiesEverythingElseAsInquiry() {
        assertThat(service.classify("Cho tôi hỏi cách đổi mật khẩu").category())
                .isEqualTo(Category.INQUIRY);
    }

    @Test
    void suggestsHighPriorityForSecurityAndPaymentKeywords() {
        assertThat(service.suggestPriority("Lỗi thanh toán không chạy").priority())
                .isEqualTo(Priority.HIGH);
    }

    @Test
    void suggestsLowPriorityForCosmeticQuestions() {
        assertThat(service.suggestPriority("Cho tôi hỏi về giao diện").priority())
                .isEqualTo(Priority.LOW);
    }

    @Test
    void suggestsMediumPriorityByDefault() {
        assertThat(service.suggestPriority("Trang danh sách tải chậm").priority())
                .isEqualTo(Priority.MEDIUM);
    }

    @Test
    void summarizeTruncatesLongDescription() {
        String longDescription = "x".repeat(200);
        String summary = service.summarize(new RequestSummaryInput(longDescription));
        assertThat(summary).startsWith("[mock] ").hasSize(7 + 150 + 3);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd BE && ./mvnw -q test -Dtest=MockLlmServiceTest`
Expected: FAIL — compile error, `MockLlmService` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
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
        if (d.contains("hỏi") || d.contains("question") || d.contains("mỹ phẩm") || d.contains("cosmetic")
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd BE && ./mvnw -q test -Dtest=MockLlmServiceTest`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add BE/src/main/java/com/bzcom/crm/llm/service/MockLlmService.java BE/src/test/java/com/bzcom/crm/llm/service/MockLlmServiceTest.java
git commit -m "feat: add MockLlmService keyword-based classify/priority/summary"
```

---

### Task 4: LlmProperties + config

**Files:**
- Create: `BE/src/main/java/com/bzcom/crm/llm/config/LlmProperties.java`
- Create: `BE/src/main/java/com/bzcom/crm/llm/config/LlmConfig.java`
- Test: `BE/src/test/java/com/bzcom/crm/llm/config/LlmPropertiesTest.java`
- Modify: `BE/src/main/resources/application.yml` (extend existing `llm:` block)
- Modify: `BE/.env.example` (add `OPENAI_API_KEY`)

**Interfaces:**
- Produces: `LlmProperties(boolean enabled, String provider, String model, long timeoutMs, String apiKey)`, bound to `llm.*` — consumed by Task 5 (`OpenAiLlmService`).

- [ ] **Step 1: Write the failing test**

```java
package com.bzcom.crm.llm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmPropertiesTest {

    @Test
    void defaultsNonPositiveTimeoutToFiveSeconds() {
        LlmProperties properties = new LlmProperties(true, "openai", "gpt-4o-mini", 0, "key");
        assertThat(properties.timeoutMs()).isEqualTo(5000);
    }

    @Test
    void keepsPositiveTimeoutAsGiven() {
        LlmProperties properties = new LlmProperties(true, "openai", "gpt-4o-mini", 8000, "key");
        assertThat(properties.timeoutMs()).isEqualTo(8000);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd BE && ./mvnw -q test -Dtest=LlmPropertiesTest`
Expected: FAIL — `LlmProperties` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
package com.bzcom.crm.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(boolean enabled, String provider, String model, long timeoutMs, String apiKey) {

    public LlmProperties {
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
    }
}
```

```java
package com.bzcom.crm.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {}
```

Extend `BE/src/main/resources/application.yml` (existing `llm:` block only has `enabled`):

```yaml
llm:
  enabled: ${LLM_ENABLED:false}
  provider: openai
  model: gpt-4o-mini
  timeout-ms: 5000
  api-key: ${OPENAI_API_KEY:}
```

Add to `BE/.env.example`:

```
OPENAI_API_KEY=
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd BE && ./mvnw -q test -Dtest=LlmPropertiesTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add BE/src/main/java/com/bzcom/crm/llm/config BE/src/test/java/com/bzcom/crm/llm/config BE/src/main/resources/application.yml BE/.env.example
git commit -m "feat: add LlmProperties config and OPENAI_API_KEY wiring"
```

---

### Task 5: OpenAiLlmService

**Files:**
- Create: `BE/src/main/java/com/bzcom/crm/llm/service/OpenAiLlmService.java`
- Test: `BE/src/test/java/com/bzcom/crm/llm/service/OpenAiLlmServiceTest.java`

**Interfaces:**
- Consumes: `LlmProperties` (Task 4), `LlmService`/DTOs (Task 2), injects `RestClient.Builder` and `ObjectMapper` (both auto-configured by Spring Boot — no new beans to declare).
- Produces: `OpenAiLlmService`, a `@Service` active when `llm.enabled=true`; constructor `OpenAiLlmService(RestClient.Builder, ObjectMapper, LlmProperties)` so tests can bind a `MockRestServiceServer` to the builder.

- [ ] **Step 1: Write the failing test**

```java
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
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
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
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":"
                                + "\"{\\\"category\\\":\\\"BUG\\\",\\\"confidence\\\":0.95,"
                                + "\\\"reason\\\":\\\"error mentioned\\\"}\"}}]}",
                        MediaType.APPLICATION_JSON));
        OpenAiLlmService service = new OpenAiLlmService(builder, objectMapper, properties);

        ClassifyResult result = service.classify("500 error on login");

        assertThat(result.category()).isEqualTo(Category.BUG);
        assertThat(result.confidence()).isEqualTo(0.95);
        server.verify();
    }

    @Test
    void classifyFallsBackToRuleBasedResultWhenLlmCallFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        OpenAiLlmService service = new OpenAiLlmService(builder, objectMapper, properties);

        ClassifyResult result = service.classify("Page shows 500 error");

        assertThat(result.category()).isEqualTo(Category.BUG);
        assertThat(result.reason()).isEqualTo("keyword rule: error terms");
    }

    @Test
    void suggestPriorityFallsBackWhenLlmCallFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        OpenAiLlmService service = new OpenAiLlmService(builder, objectMapper, properties);

        PriorityResult result = service.suggestPriority("Lỗi thanh toán");

        assertThat(result.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void summarizeFallsBackToTruncatedDescriptionWhenLlmCallFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        OpenAiLlmService service = new OpenAiLlmService(builder, objectMapper, properties);

        String summary = service.summarize(new RequestSummaryInput("short description"));

        assertThat(summary).isEqualTo("short description");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd BE && ./mvnw -q test -Dtest=OpenAiLlmServiceTest`
Expected: FAIL — `OpenAiLlmService` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.config.LlmProperties;
import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "true")
public class OpenAiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmService.class);

    private static final String CLASSIFY_SYSTEM_PROMPT =
            """
            Ban la tro ly phan loai yeu cau ho tro cua Bzcom - cong ty van hanh web service.
            Nhiem vu: phan loai mo ta cua khach hang vao DUNG MOT trong ba nhan sau:
            - BUG:     loi/su co khi he thong dang chay (error, crash, khong hoat dong dung).
            - FEATURE: de nghi them hoac cai tien chuc nang moi.
            - INQUIRY: cau hoi/thac mac, khong phai loi cung khong phai yeu cau tinh nang.

            Chi tra ve JSON dung dinh dang sau, KHONG them bat ky chu nao khac:
            {"category":"BUG|FEATURE|INQUIRY","confidence":<0.0-1.0>,"reason":"<ly do ngan gon>"}

            Neu khong chac chan, chon nhan kha di nhat va ha confidence xuong duoi 0.6.

            FEW-SHOT (vi du mau):
            Input: "Nut thanh toan bam khong phan hoi"
            Output: {"category":"BUG","confidence":0.95,"reason":"chuc nang khong hoat dong"}
            Input: "Cho toi hoi cach doi mat khau?"
            Output: {"category":"INQUIRY","confidence":0.90,"reason":"la cau hoi huong dan"}
            Input: "Mong them dang nhap bang Google"
            Output: {"category":"FEATURE","confidence":0.92,"reason":"de nghi tinh nang moi"}
            """;

    private static final String PRIORITY_SYSTEM_PROMPT =
            """
            Ban la tro ly goi y muc do uu tien cho yeu cau ho tro cua Bzcom.
            HIGH = anh huong nhieu nguoi dung / chan nghiep vu / lien quan bao mat-thanh toan.
            MEDIUM = anh huong mot phan.
            LOW = my pham/hoi dap.

            Chi tra ve JSON dung dinh dang sau, KHONG them bat ky chu nao khac:
            {"priority":"HIGH|MEDIUM|LOW","confidence":<0.0-1.0>,"reason":"<ly do ngan gon>"}
            """;

    private static final String SUMMARY_SYSTEM_PROMPT =
            "Tom tat yeu cau trong toi da 2 cau, neu van de chinh va muc khan cap, "
                    + "khong them thong tin khong co trong mo ta.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiLlmService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, LlmProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.timeoutMs());
        requestFactory.setReadTimeout((int) properties.timeoutMs());
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
        this.objectMapper = objectMapper;
        this.model = properties.model();
    }

    @Override
    public ClassifyResult classify(String description) {
        try {
            String content = complete(CLASSIFY_SYSTEM_PROMPT, "Input: \"" + description + "\"\nOutput:");
            JsonNode json = objectMapper.readTree(content);
            return new ClassifyResult(
                    Category.valueOf(json.get("category").asText()),
                    json.get("confidence").asDouble(),
                    json.get("reason").asText());
        } catch (Exception exception) {
            log.warn("LLM classify failed, falling back to rule-based result", exception);
            return ruleBasedClassifyFallback(description);
        }
    }

    @Override
    public PriorityResult suggestPriority(String description) {
        try {
            String content = complete(PRIORITY_SYSTEM_PROMPT, "Input: \"" + description + "\"\nOutput:");
            JsonNode json = objectMapper.readTree(content);
            return new PriorityResult(
                    Priority.valueOf(json.get("priority").asText()),
                    json.get("confidence").asDouble(),
                    json.get("reason").asText());
        } catch (Exception exception) {
            log.warn("LLM suggest-priority failed, falling back to rule-based result", exception);
            return ruleBasedPriorityFallback(description);
        }
    }

    @Override
    public String summarize(RequestSummaryInput request) {
        try {
            return complete(SUMMARY_SYSTEM_PROMPT, request.description()).trim();
        } catch (Exception exception) {
            log.warn("LLM summary failed, falling back to truncated description", exception);
            return truncateSummaryFallback(request.description());
        }
    }

    private String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages",
                        List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt)));
        JsonNode response = restClient
                .post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return response.at("/choices/0/message/content").asText();
    }

    private ClassifyResult ruleBasedClassifyFallback(String description) {
        String lower = description.toLowerCase();
        if (lower.matches(".*(loi|error|crash|500|khong hoat dong|fail).*")
                || description.toLowerCase().contains("lỗi")
                || description.toLowerCase().contains("không hoạt động")) {
            return new ClassifyResult(Category.BUG, 0.5, "keyword rule: error terms");
        }
        if (lower.matches(".*(them|mong|de nghi|feature|ho tro.*moi).*")
                || description.toLowerCase().contains("đề nghị")
                || description.toLowerCase().contains("thêm")) {
            return new ClassifyResult(Category.FEATURE, 0.5, "keyword rule: request terms");
        }
        return new ClassifyResult(Category.INQUIRY, 0.4, "fallback default");
    }

    private PriorityResult ruleBasedPriorityFallback(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("bảo mật")
                || lower.contains("security")
                || lower.contains("thanh toán")
                || lower.contains("payment")
                || lower.contains("down")
                || lower.contains("crash")) {
            return new PriorityResult(Priority.HIGH, 0.5, "keyword rule: security/payment/outage terms");
        }
        if (lower.contains("hỏi") || lower.contains("question") || lower.contains("mỹ phẩm") || lower.contains("cosmetic")) {
            return new PriorityResult(Priority.LOW, 0.4, "keyword rule: cosmetic/question terms");
        }
        return new PriorityResult(Priority.MEDIUM, 0.4, "fallback default");
    }

    private String truncateSummaryFallback(String description) {
        return description.length() > 150 ? description.substring(0, 150) + "..." : description;
    }
}
```

> Note for implementer: the ASCII-only system prompt constants above are a workaround if the editing tool mangles Vietnamese diacritics in a Java text block — if diacritics survive intact, keep the exact wording from `docs/LLM.md` §3–4 (with proper Vietnamese) instead, since that's what the DoD/PPT slide 8 references. Verify by opening the file after writing it.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd BE && ./mvnw -q test -Dtest=OpenAiLlmServiceTest`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add BE/src/main/java/com/bzcom/crm/llm/service/OpenAiLlmService.java BE/src/test/java/com/bzcom/crm/llm/service/OpenAiLlmServiceTest.java
git commit -m "feat: add OpenAiLlmService with rule-based fallback"
```

---

### Task 6: LlmController

**Files:**
- Create: `BE/src/main/java/com/bzcom/crm/llm/controller/LlmController.java`
- Test: `BE/src/test/java/com/bzcom/crm/llm/controller/LlmControllerTest.java`

**Interfaces:**
- Consumes: `LlmService` (Task 2/3/5), `DescriptionRequest` (Task 2).
- Produces: `POST /api/requests/classify`, `POST /api/requests/suggest-priority` matching `docs/openapi.yaml`.

- [ ] **Step 1: Write the failing test**

```java
package com.bzcom.crm.llm.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.service.LlmService;
import com.bzcom.crm.request.domain.Category;
import com.bzcom.crm.request.domain.Priority;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LlmController.class)
class LlmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd BE && ./mvnw -q test -Dtest=LlmControllerTest`
Expected: FAIL — `LlmController` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
package com.bzcom.crm.llm.controller;

import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.llm.dto.request.DescriptionRequest;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "LLM", description = "Phan loai va goi y bang AI")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping(value = "/classify", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tu phan loai category tu description")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", description = "Ket qua phan loai", useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Du lieu khong hop le",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chua dang nhap",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<ClassifyResult> classify(@Valid @RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.classify(request.description()));
    }

    @PostMapping(value = "/suggest-priority", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Goi y priority tu description")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", description = "Priority goi y", useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Du lieu khong hop le",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chua dang nhap",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PriorityResult> suggestPriority(@Valid @RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.suggestPriority(request.description()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd BE && ./mvnw -q test -Dtest=LlmControllerTest`
Expected: PASS (3 tests). If the unauthenticated/security behavior of `@WebMvcTest` differs from expectations, adjust the test to match actual `SecurityConfig` slice behavior rather than assuming — verify, don't guess.

- [ ] **Step 5: Commit**

```bash
git add BE/src/main/java/com/bzcom/crm/llm/controller BE/src/test/java/com/bzcom/crm/llm/controller
git commit -m "feat: add LlmController classify and suggest-priority endpoints"
```

---

### Task 7: Full verify + TASKS.md status

**Files:**
- Modify: `docs/TASKS.md` (mark T-3.1 progress note; T-3.2 stays open — summary endpoint still pending B's Request entity)

- [ ] **Step 1: Run full quality gate**

Run: `cd BE && ./mvnw -B verify`
Expected: BUILD SUCCESS — compile, all unit/slice tests, Spotless check, JaCoCo report.

- [ ] **Step 2: Update task status**

In `docs/TASKS.md`, change T-3.1 to `[x]` (fully done: interface + Mock, all 3 methods). Leave T-3.2 as `[ ]` and add a line: `- Trạng thái: classify/suggest-priority + OpenAiLlmService xong; summary endpoint chờ Request entity (T-2.B1).`

- [ ] **Step 3: Commit**

```bash
git add docs/TASKS.md
git commit -m "docs: mark T-3.1 done, note T-3.2 partial status"
```

- [ ] **Step 4: Push and open PR**

```bash
git push -u origin feature/llm-stats
```

Open PR into `develop` using the template in `docs/GIT_WORKFLOW.md` §6; reference T-3.1/T-3.2 in the description.
