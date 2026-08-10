# TASKS — Task-list triển khai toàn dự án Bzcom CRM

> Danh sách công việc **đầy đủ, có thứ tự, kiểm chứng được** để hoàn thiện dự án. Mỗi task tự chứa đủ thông tin để một **thành viên** hoặc **AI agent** nhận và làm độc lập.
>
> **Cách dùng:** làm theo thứ tự Phase. Trong một phase, các task cùng owner làm tuần tự; khác owner có thể làm song song **nếu đã thoả `Depends on`**. Tick `[x]` khi **đạt toàn bộ Definition of Done (DoD)**, không phải khi "viết xong code".
>
> **Snapshot `develop` 2026-08-10:** backend feature đã merge; 44 unit test + 19 integration
> test và full-stack Docker smoke đang xanh. Task còn `[ ]` có thể đã có implementation nhưng
> vẫn thiếu một phần DoD được ghi rõ (test nghiệm thu, bằng chứng thủ công hoặc FE action),
> không đồng nghĩa phải viết lại từ đầu.

---

## 0. Quy ước đọc task

**Mỗi task có dạng:**
```
### [ ] T-<phase>.<số> — <Tên task>
- Owner: <A/B/C/D/FE/ALL>   Depends on: <task ID hoặc —>   Ước lượng: <phút>
- Refs: <tài liệu liên quan>
- Việc: các bước cụ thể.
- DoD: điều kiện nghiệm thu (kiểm chứng được).
```

**Mã Owner** (theo phân công 4 người — [kế hoạch §10](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#10-phân-công-nhóm-4-người)):
| Mã | Người | Phạm vi backend |
|---|---|---|
| **A** | Nền tảng & Auth | `common`, `config`, `auth`, `member` |
| **B** | Request core | `request` (CRUD, filter, stats) |
| **C** | Workflow logic | `workflow` (assign, status, history) |
| **D** | Cross-cutting & AI | `alert`, `llm`, Swagger config, Git/CI lead |
| **FE** | Frontend | `FE/` (do A hoặc D kiêm khi rảnh, hoặc chia theo feature) |
| **ALL** | Cả nhóm | việc chung |

**Definition of Done — tiêu chuẩn chung** (áp cho MỌI task code backend, trừ khi task ghi khác):
1. Code chạy đúng luồng chính **và** luồng lỗi.
2. Trả đúng **response envelope** `{status,message,data}` + đúng **HTTP status** ([bảng](./OPENAPI.md#5-bảng-mã-http-status)).
3. Có **Swagger annotation** (endpoint hiện đúng trên `/swagger-ui.html`).
4. Có **≥1 test** cho luồng chính (unit hoặc integration).
5. Merge vào `develop` qua **PR có ≥1 review + CI xanh** (không push thẳng).

**Nguyên tắc code** (bắt buộc — [CLAUDE.md](../CLAUDE.md)): làm tối giản, chỉ code đúng yêu cầu, không thêm abstraction thừa, không "cải thiện" code ngoài phạm vi task.

---

## PHASE 0 — Thống nhất nền tảng (CẢ NHÓM, ~60′) 🔴 KHÔNG ĐƯỢC BỎ QUA

> Phase này quyết định việc merge sau có xung đột không. Mọi người phải đồng ý trước khi ai code.

### [ ] T-0.1 — Chốt ERD
- Owner: ALL (A dẫn)   Depends on: —   Ước lượng: 15′
- Refs: [ERD.md](./ERD.md)
- Việc: cả nhóm review [ERD.md](./ERD.md); dán DBML vào dbdiagram.io, xuất ảnh ERD để dùng slide 3. Xác nhận: 5 bảng (4 nghiệp vụ + `refresh_tokens`), 2 FK từ requests→members, `version`, `last_completed_at`.
- DoD: có file ảnh ERD trong `docs/assets/erd.png`; cả nhóm xác nhận không đổi schema nữa.

### [ ] T-0.2 — Chốt API contract
- Owner: ALL (D dẫn)   Depends on: —   Ước lượng: 15′
- Refs: [openapi.yaml](./openapi.yaml), [OPENAPI.md](./OPENAPI.md)
- Việc: review toàn bộ endpoint + request/response mẫu + bảng HTTP status. Import `openapi.yaml` vào Postman tạo collection dùng chung.
- DoD: Postman collection được share; mọi người đồng ý contract (path, method, body, status). Đây là "hợp đồng" — sau này lệch phải sửa qua PR.

### [ ] T-0.3 — Tạo GitHub repo + branch + protection
- Owner: D   Depends on: —   Ước lượng: 15′
- Refs: [GIT_WORKFLOW.md](./GIT_WORKFLOW.md)
- Việc: tạo repo; tạo `main` + `develop`; bật branch protection cho cả hai (require PR + 1 approval + status check); thêm `.gitignore` (Java, Node, `.env`), `pull_request_template.md`.
- DoD: không ai push thẳng được vào `main`/`develop` (thử push trực tiếp bị chặn); template PR hiện khi mở PR.

### [ ] T-0.4 — Chốt cấu trúc package & quy ước
- Owner: ALL (A dẫn)   Depends on: —   Ước lượng: 15′
- Refs: [ARCHITECTURE.md §5](./ARCHITECTURE.md#5-cấu-trúc-package-package-by-feature)
- Việc: thống nhất package-by-feature, quy ước đặt tên (Controller/Service/Repository/Dto), commit convention, ai review ai.
- DoD: cả nhóm nắm rõ package mình sở hữu; ghi lại trong PR/issue "team conventions".

---

## PHASE 1 — Nền tảng backend (Owner A, ~90′) 🔴 CHẶN CÁC PHASE SAU

> A làm trước và merge sớm. B/C/D dựa trên nền này. Xong sớm → A nhảy vào phụ C.

### [x] T-1.1 — Khởi tạo project Spring Boot
- Owner: A   Depends on: T-0.4   Ước lượng: 15′
- Refs: [ARCHITECTURE.md](./ARCHITECTURE.md), [kế hoạch §3.2](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md)
- Việc: tạo project Maven trong `BE/`: Java 21 (LTS), Spring Boot 3.5.5 (khóa bằng Maven Wrapper và `pom.xml`). Thêm dependency: web, data-jpa, security, `spring-security-test`, validation, actuator, postgresql, flyway-core + PostgreSQL module, jjwt (api/impl/jackson), springdoc-openapi-starter-webmvc-ui, mapstruct + processor, lombok, `spring-boot-testcontainers`, Testcontainers 2.0.5 `testcontainers-junit-jupiter` + `testcontainers-postgresql` (test; tương thích Docker Engine 29+). Tạo `CentralMapperConfig` (`componentModel=spring`, constructor injection, unmapped target = error), compiler annotation processors (Lombok + MapStruct), Spotless (format/import order), Failsafe cho `*IT` và JaCoCo report. Tạo `CrmApplication.java`, package gốc `com.bzcom.crm`.
- DoD: `cd BE && ./mvnw compile` pass; app start được (dù chưa có business endpoint); push branch `feature/foundation`.

### [x] T-1.2 — Cấu hình DB + Flyway + profiles
- Owner: A   Depends on: T-1.1   Ước lượng: 15′
- Refs: [ERD.md §7](./ERD.md#7-flyway-migration-v1__initsql), [ARCHITECTURE.md §9](./ARCHITECTURE.md#9-cấu-hình-profile--môi-trường)
- Việc: tạo `application.yml` với profile `dev`/`docker` đều trỏ PostgreSQL qua env; test integration dùng `@ServiceConnection` từ Testcontainers PostgreSQL. Tạo `db/migration/V1__init.sql` (5 bảng) dùng mọi môi trường; seed demo đặt riêng trong `db/demo` và chỉ bật ở `dev`/`docker`. Bật Flyway, đặt `ddl-auto=validate`.
- DoD: chạy profile `dev` → Flyway tạo 5 bảng + index; test integration khởi PostgreSQL container và log Flyway "Successfully applied 1 migration".

### [x] T-1.3 — Common: ApiResponse + PageResponse
- Owner: A   Depends on: T-1.1   Ước lượng: 10′
- Refs: [kế hoạch §16.1](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md), [OPENAPI.md §4](./OPENAPI.md#4-response-envelope--phân-trang)
- Việc: `common/response/ApiResponse.java` (record, `ok`/`created`/`error`), `PageResponse.java` (content/page/size/totalElements/totalPages).
- DoD: unit test `ApiResponse.ok(x)` → status=200, message="success", data=x.

### [ ] T-1.4 — Common: Exception + GlobalExceptionHandler
- Owner: A   Depends on: T-1.3   Ước lượng: 20′
- Refs: [kế hoạch §16.2](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md), [BUSINESS_LOGIC.md §9](./BUSINESS_LOGIC.md#9-bảng-edge-case-tổng-hợp)
- Việc: `BusinessException` immutable (chứa `ErrorCode`) + `ErrorCode` enum (mã ổn định, message an toàn, HTTP status). `GlobalExceptionHandler` (`@RestControllerAdvice`) xử lý: validation→400, JSON field lạ/malformed→400, AccessDenied→403, EntityNotFound→404, InvalidStatusTransition/RequestNotAssigned/OptimisticLock→409, fallback→500. Tất cả trả `ApiResponse.error`; fallback chỉ log stacktrace ở server, không trả chi tiết nội bộ.
- DoD: test: ném mỗi exception → nhận đúng status + envelope; không lộ stacktrace.
- Trạng thái: implementation và các lỗi chính đã được E2E cover; còn thiếu test ma trận đầy
  đủ 404/422/500 trước khi tick theo đúng DoD "mỗi exception".

### [x] T-1.5 — Common: BaseTimeEntity + JPA Auditing
- Owner: A   Depends on: T-1.1   Ước lượng: 10′
- Refs: [kế hoạch §16.4](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md)
- Việc: `BaseTimeEntity` (`@MappedSuperclass`, `@CreatedDate`/`@LastModifiedDate`). `JpaAuditingConfig` với `@EnableJpaAuditing`.
- DoD: một entity kế thừa → createdAt/updatedAt tự set khi save (test).

### [x] T-1.6 — Security + JWT
- Owner: A   Depends on: T-1.4   Ước lượng: 30′
- Refs: [BUSINESS_LOGIC.md §1](./BUSINESS_LOGIC.md#1-authentication--authorization), [kế hoạch §16.6](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md)
- Việc: `SecurityConfig` stateless, CSRF off, chỉ permitAll **theo method** cho `POST /api/auth/login|refresh|logout`, `POST /api/members`, Swagger; mọi route khác authenticated + `@EnableMethodSecurity`. `JwtProvider` chỉ generate/validate access JWT (memberId+role, 15 phút); refresh token opaque sinh từ `SecureRandom`, DB chỉ lưu SHA-256 hash. `JwtAuthenticationFilter` đọc Bearer. `PasswordEncoder` = BCrypt. `CorsConfig` cho `http://localhost:5173`.
- DoD: endpoint bảo vệ không token → 401; token hợp lệ → qua; Swagger UI truy cập được không cần token.

### [x] T-1.7 — OpenApiConfig (Swagger)
- Owner: A (khởi tạo) / D (hoàn thiện)   Depends on: T-1.1   Ước lượng: 10′
- Refs: [OPENAPI.md](./OPENAPI.md)
- Việc: `OpenApiConfig` (metadata: title, version) + security scheme `bearerAuth` (nút Authorize trên Swagger UI để dán JWT).
- DoD: `/swagger-ui.html` mở được, có nút "Authorize".

### [x] T-1.8 — Member domain (entity/repo/service/controller/dto)
- Owner: A   Depends on: T-1.5, T-1.6   Ước lượng: 25′
- Refs: [openapi.yaml](./openapi.yaml)(Member), [ERD.md](./ERD.md)(members)
- Việc: `Member` entity (role enum, password BCrypt), `MemberRepository` (findByEmail), `MemberService` (public register luôn gán role `CLIENT`, hash password, chặn email trùng→409; getAll ADMIN; getById chỉ ADMIN hoặc chính member), `MemberController` (`POST /api/members` public 201, `GET /api/members` ADMIN, `GET /api/members/{id}`). Public DTO **không có role**; cấu hình `spring.jackson.deserialization.fail-on-unknown-properties=true` và DTO phải reject field lạ; response không chứa password. MapStruct mapper.
- DoD: đăng ký member → CLIENT/201; body có `role: ADMIN` → **400** (không âm thầm bỏ qua/không leo quyền); DB lưu hash (không plaintext), response **không** có password; email trùng → 409; GET /members không phải ADMIN → 403; GET member khác mình → 403. + DoD chung.

### [x] T-1.9 — Auth API (login/refresh/logout)
- Owner: A   Depends on: T-1.6, T-1.8   Ước lượng: 20′
- Refs: [openapi.yaml](./openapi.yaml)(Auth), [BUSINESS_LOGIC.md §1.1](./BUSINESS_LOGIC.md#11-luồng-jwt)
- Việc: thêm `RefreshToken` entity/repository (opaque token hash, expiresAt, revokedAt). `POST /login`: BCrypt → access JWT 15 phút + refresh 7 ngày, lưu hash. `POST /refresh`: chỉ phát cặp mới khi revoke token cũ thành công qua `UPDATE ... WHERE revoked_at IS NULL AND expires_at > now()` (hoặc khóa row token) trong cùng transaction. `POST /logout`: nhận refresh token, revoke idempotent. Login/refresh/logout không phụ thuộc Bearer access token. DTO: LoginRequest, RefreshTokenRequest (32–512 ký tự), TokenResponse.
- DoD: login đúng → 200 + tokens + role; refresh hợp lệ trả cặp mới và token cũ không refresh được; **hai refresh song song trên cùng token chỉ đúng một 200, một 401**; logout → refresh token không dùng lại được; sai password/token hết hạn → 401; access token dùng được cho endpoint bảo vệ. + DoD chung.

### [ ] T-1.10 — Seed data
- Owner: A   Depends on: T-1.8   Ước lượng: 10′
- Refs: [ERD.md §8](./ERD.md#8-demo-seed-v2__seed_demosql), [README §5](./README.md)
- Việc: `db/demo/V2__seed_demo.sql` chỉ được thêm vào Flyway locations của profile `dev`/`docker`: admin/dev1/dev2/client1 (password BCrypt của `1234`) + vài request mẫu. Dùng hash BCrypt thật; production profile chỉ chạy `db/migration`.
- DoD: sau khi start (profile demo), login được cả 4 tài khoản; có sẵn request để test filter/stats.
- Trạng thái: migration seed và BCrypt hash thật đã chạy trong Docker; còn thiếu biên bản
  smoke login đủ cả 4 tài khoản trước khi tick.

> **🚩 Milestone M1:** merge Phase 1 vào `develop`. B/C/D bắt đầu Phase 2. A thông báo "nền tảng sẵn sàng".

---

## PHASE 2 — Tính năng backend song song (B, C, D + A phụ, ~150′)

> B/C/D `git pull develop` (có nền của A) rồi làm trên feature branch riêng. Package tách biệt → ít đụng độ.
>
> Implementation Phase 2 đã merge. Các task còn mở dưới đây đang thiếu test DoD cụ thể;
> chúng là phạm vi hardening, không phải blocker biên dịch/runtime.

### Nhánh B — Request core

### [x] T-2.B1 — Request entity + repository
- Owner: B   Depends on: M1   Ước lượng: 15′
- Refs: [ERD.md](./ERD.md)(requests), [kế hoạch §16.5](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md)
- Việc: `Request` entity kế thừa BaseTimeEntity (enum category/priority/status, `@Version int version`, clientId, assignedDeveloperId nullable). `RequestRepository extends JpaRepository, JpaSpecificationExecutor`.
- DoD: entity map đúng bảng; save/find hoạt động trong Testcontainers PostgreSQL.

### [ ] T-2.B2 — Tạo request (CLIENT) + trigger alert HIGH
- Owner: B   Depends on: T-2.B1, T-2.D1   Ước lượng: 25′
- Refs: [openapi.yaml](./openapi.yaml), [BUSINESS_LOGIC.md §5.3](./BUSINESS_LOGIC.md#53-pseudocode-tạo-request-high)
- Việc: `POST /api/requests` (CLIENT only, `@PreAuthorize`), status mặc định PENDING, clientId = current user. `@Transactional`: nếu priority=HIGH → gọi `AlertService.create(...)` cho mọi ADMIN.
- DoD: CLIENT tạo → 201; role khác → 403; tạo HIGH → mọi ADMIN có alert (cùng transaction). + DoD chung.
- Trạng thái: create 201 và HIGH alert đã có E2E; còn thiếu IT role khác → 403.

### [ ] T-2.B3 — Danh sách request: filter + sort + paging + phạm vi role
- Owner: B   Depends on: T-2.B1   Ước lượng: 35′
- Refs: [BUSINESS_LOGIC.md §1.3](./BUSINESS_LOGIC.md#13-ma-trận-truy-cập-dữ-liệu-request-áp-trước-mọi-filter-khác), [OPENAPI.md §7](./OPENAPI.md)
- Việc: `GET /api/requests` với `Pageable` + JPA `Specification` (status/category/priority/keyword). **Áp phạm vi role trước:** ADMIN=all, CLIENT=client_id, DEVELOPER=assigned_developer_id. Trả `PageResponse`.
- DoD: 3 role gọi → thấy đúng phạm vi; filter tổ hợp + `?page&size&sort` hoạt động; keyword tìm trong title/description. + DoD chung.
- Trạng thái: code/filter/paging đã merge; còn thiếu integration test dữ liệu cho đủ 3 role
  và filter tổ hợp trước khi tick hoàn tất.

### [ ] T-2.B4 — Chi tiết request (kiểm quyền xem)
- Owner: B   Depends on: T-2.B1   Ước lượng: 15′
- Refs: [BUSINESS_LOGIC.md §1.3](./BUSINESS_LOGIC.md#13-ma-trận-truy-cập-dữ-liệu-request-áp-trước-mọi-filter-khác)
- Việc: `GET /api/requests/{id}`. Kiểm ownership: ngoài phạm vi role → 403; không tồn tại → 404.
- DoD: chủ/ADMIN xem được; người khác → 403; id sai → 404. + DoD chung.
- Trạng thái: endpoint và ownership 403 đã có IT; còn thiếu case 404 riêng trước khi tick.

### Nhánh C — Workflow logic (nặng nhất — A phụ khi rảnh)

### [x] T-2.C1 — State machine RequestStatus
- Owner: C   Depends on: M1   Ước lượng: 15′
- Refs: [BUSINESS_LOGIC.md §3](./BUSINESS_LOGIC.md#3-status-state-machine), [kế hoạch §16.3](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md)
- Việc: enum `RequestStatus` với `ALLOWED` map + `canTransitionTo()`. `InvalidStatusTransitionException`.
- DoD: **unit test đầy đủ**: PENDING→IN_PROGRESS ok; IN_PROGRESS→DONE ok; DONE→* false; PENDING→DONE false; IN_PROGRESS→PENDING false.

### [ ] T-2.C2 — RequestHistory entity + HistoryService
- Owner: C   Depends on: M1   Ước lượng: 20′
- Refs: [BUSINESS_LOGIC.md §4](./BUSINESS_LOGIC.md#4-automatic-history-recording), [ERD.md](./ERD.md)(request_histories)
- Việc: `RequestHistory` entity + repo. `HistoryService.record(request, changedBy, from, to, memo)`. `GET /api/requests/{id}/history` sắp xếp `changedAt ASC`, bắt buộc gọi chung `RequestAccessPolicy.assertCanRead` như detail.
- DoD: gọi record → tạo bản ghi; chủ/ADMIN lấy history đúng thứ tự, DEV/CLIENT không thuộc request → 403. + DoD chung.
- Trạng thái: record/thứ tự history đã chạy trong E2E; còn thiếu IT history ngoài phạm vi → 403.

### [ ] T-2.C3 — Auto-assign + manual assign
- Owner: C   Depends on: T-2.C2, T-2.B1, T-2.D1   Ước lượng: 40′
- Refs: [BUSINESS_LOGIC.md §2](./BUSINESS_LOGIC.md#2-auto-assignment)
- Việc: `AssignService` + `PATCH /api/requests/{id}/assign` (ADMIN only). Command bắt buộc `expectedVersion`; auto=true khóa developer theo `id ASC` (`PESSIMISTIC_WRITE`) rồi tính taskCount động (COUNT chưa DONE), min → tie-break `last_completed_at` desc (null cuối); 0 developer → 422. auto=false → dùng developerId (phải là DEVELOPER). `@Transactional`: version check + gán + flush (`@Version`) + history + alert.
- DoD: **unit test thuật toán** (case count khác nhau + case hoà tie-break như [ví dụ §2.3](./BUSINESS_LOGIC.md#23-ví-dụ-minh-hoạ-để-demoslide)); auto-assign chọn đúng dev; ghi history + alert; 0 dev → 422; role khác → 403. + DoD chung.
- Trạng thái: auto/manual assign, history và alert đã merge; còn thiếu test ma trận thuật toán,
  zero-developer 422 và permission trước khi tick.

### [x] T-2.C4 — Đổi status (state machine + optimistic lock + history + alert)
- Owner: C   Depends on: T-2.C1, T-2.C2, T-2.D1   Ước lượng: 35′
- Refs: [BUSINESS_LOGIC.md §3,§8](./BUSINESS_LOGIC.md#3-status-state-machine)
- Việc: `StatusService` + `PATCH /api/requests/{id}/status` (ADMIN hoặc DEVELOPER được gán — ownership check → 403). Command bắt buộc `expectedVersion`; check version trước; request chưa có assignee → 409; `canTransitionTo` → sai 409. `@Transactional`: update + flush (`@Version`, conflict→409) + history + alert STATUS_CHANGED cho client. Khi →DONE: set `member.last_completed_at = now()`.
- DoD: transition hợp lệ → 200; request chưa gán → 409; sai luật → 409 với message rõ; DEVELOPER không được gán → 403; →DONE cập nhật last_completed_at; history + alert tự sinh. + DoD chung.

### Nhánh D — Alert + Swagger

### [x] T-2.D1 — Alert entity + AlertService (điểm vào tạo alert)
- Owner: D   Depends on: M1   Ước lượng: 25′
- Refs: [BUSINESS_LOGIC.md §5](./BUSINESS_LOGIC.md#5-automatic-alert-generation), [ERD.md](./ERD.md)(alerts)
- Việc: `Alert` entity + repo. `AlertService.create(targetMemberId, requestId, type, message)` — interface để B/C gọi. (Đây là dependency của T-2.B2, T-2.C3, T-2.C4 → **làm sớm**.)
- DoD: `AlertService.create` lưu alert đúng; test tạo alert từng loại type. + DoD chung.

### [x] T-2.D2 — Alert API (list own + mark read)
- Owner: D   Depends on: T-2.D1   Ước lượng: 20′
- Refs: [openapi.yaml](./openapi.yaml)(Alert)
- Việc: `GET /api/alerts` (của current user, filter `?isRead`), `PATCH /api/alerts/{id}/read` (chỉ owner → 403 nếu không). 
- DoD: user chỉ thấy alert của mình; mark read đổi isRead=true; sửa alert người khác → 403. + DoD chung.

### [x] T-2.D3 — Rà soát Swagger endpoint Phase 2
- Owner: D   Depends on: (các endpoint P2)   Ước lượng: 20′
- Refs: [OPENAPI.md](./OPENAPI.md), [openapi.yaml](./openapi.yaml)
- Việc: đảm bảo controller Phase 2 có `@Tag`/`@Operation`/`@ApiResponse`; đối chiếu path/method/status với `openapi.yaml` trước khi merge.
- DoD: Swagger runtime có đủ endpoint Phase 2 với mô tả; không lệch contract ở phạm vi Phase 2.

> **🚩 Milestone M2:** B/C/D merge Phase 2 vào `develop`. Luồng cốt lõi chạy được end-to-end (login→tạo→gán→đổi status→history→alert).

---

## PHASE 3 — LLM + Statistics (E, ~40′)

### [x] T-3.1 — LlmService interface + MockLlmService
- Owner: E   Depends on: M2   Ước lượng: 20′
- Refs: [LLM.md §5,§6,§7](./LLM.md#6-mock-mode-chống-chết-demo)
- Việc: `LlmService` interface (`classify`, `suggestPriority`, `summarize`). `MockLlmService` (`@ConditionalOnProperty llm.enabled=false`, keyword-based) triển khai đủ ba method. DTO: `ClassifyResult`, `PriorityResult`. Cấu hình `llm.*` trong application.yml.
- DoD: `llm.enabled=false` → dùng mock, không gọi mạng; test mock trả đúng theo keyword.

### [x] T-3.2 — LLM APIs (+ fallback) & GeminiLlmService
- Owner: E   Depends on: T-3.1   Ước lượng: 25′
- Refs: [LLM.md §3,§5](./LLM.md#3-thiết-kế-prompt--auto-classify)
- Việc: triển khai `POST /api/requests/classify`, `POST /api/requests/suggest-priority`, `GET /api/requests/{id}/summary`. `GeminiLlmService` (`@ConditionalOnProperty llm.enabled=true`) gọi API thật (Gemini, qua endpoint tương thích OpenAI Chat Completions) với prompt cấu trúc; parse JSON an toàn; timeout/lỗi → fallback. Summary phải kiểm `RequestAccessPolicy` trước khi gửi description cho LLM.
- DoD: classify/priority (mock) trả enum + confidence + reason; summary chỉ 1–2 câu và chặn request không thuộc quyền bằng 403; LLM lỗi → fallback không làm chết API (200); prompt lưu trong `prompt/`. + DoD chung.
- Trạng thái: xong toàn bộ sau khi B merge `Request` entity + `RequestAccessPolicy` (T-2.B1). `GET /{id}/summary` tái sử dụng `RequestService.getRequestDetail` (đã check `RequestAccessPolicy` → 403/404 sẵn). Đã gỡ một bản LLM khác do D merge trùng vào `develop` (thiếu fallback/timeout/test, sai contract endpoint `/summarize`) — xem PR #5. Provider thật đổi từ OpenAI sang Gemini (key OpenAI hết credit, team có key Gemini hoạt động) — đã verify bằng gọi API thật với prompt thật, phát hiện và sửa lỗi Gemini bọc JSON trong markdown code fence (khác OpenAI), có test regression.

### [ ] T-3.3 — Statistics API
- Owner: E   Depends on: M2   Ước lượng: 25′
- Refs: [BUSINESS_LOGIC.md §6](./BUSINESS_LOGIC.md#6-statistics), [openapi.yaml](./openapi.yaml)(StatsResponse)
- Việc: `GET /api/requests/stats` (ADMIN). total, completed, completionRate (total=0→0, tránh chia 0), byCategory (group), byDeveloper (assignedCount + doneCount).
- DoD: số liệu đúng với seed data; total=0 không lỗi; role khác → 403. + DoD chung.
- Trạng thái: `RequestStatsService` + `GET /api/requests/stats` (thêm vào `RequestController`, `@PreAuthorize("hasRole('ADMIN')")`) đã code xong, có unit test cho logic tổng hợp (total=0 → completionRate=0; byCategory/byDeveloper). CI thật (GitHub Actions, PR #5) đã chạy full `mvn -B verify` bao gồm integration test Testcontainers PostgreSQL (`AuthMemberIT`, `FoundationIT`, `RequestWorkflowIT`) — tất cả xanh, nên môi trường/schema không phải vấn đề. Giữ `[ ]` vì còn 1 phần DoD chưa có test riêng: chưa có integration test xác nhận cụ thể role khác ADMIN → 403 cho đúng endpoint `/stats` (đã thử ở mức `@WebMvcTest` nhưng `@PreAuthorize` không enforce trong slice đó — cần IT thật, xem PR #5).

> **🚩 Milestone M3:** backend hoàn chỉnh tính năng. Tag `v0.9-backend`.

---

## PHASE 4 — Tích hợp & chất lượng (ALL, ~80′)

### [x] T-4.1 — Docker Compose (app + postgres)
- Owner: D   Depends on: M3   Ước lượng: 30′
- Refs: [ARCHITECTURE.md §10](./ARCHITECTURE.md#10-kiến-trúc-triển-khai-docker), [README §4](./README.md)
- Việc: `BE/Dockerfile` multi-stage (Maven build → JRE slim). `BE/compose.yaml`: service `db` (postgres:16 + healthcheck + volume) + `app` (depends_on db healthy, env từ `.env`). `BE/.env.example`.
- DoD: máy sạch chạy `docker compose up --build` → app lên, Flyway migrate, Swagger truy cập `:8080`; seed data có sẵn.

### [ ] T-4.2 — Integration test luồng chính (E2E)
- Owner: ALL (mỗi người phần mình)   Depends on: M3   Ước lượng: 40′
- Refs: [README §6](./README.md)(kịch bản demo), [BUSINESS_LOGIC.md §9](./BUSINESS_LOGIC.md#9-bảng-edge-case-tổng-hợp)
- Việc: test tích hợp (`@SpringBootTest` + MockMvc + Testcontainers PostgreSQL) theo kịch bản: login/refresh/logout (kể cả refresh song song), 3 role → tạo HIGH → alert ADMIN → auto-assign → đổi status hợp lệ → request chưa gán/transition sai/version cũ 409 → history → stats. Cover các edge case ở §9.
- DoD: toàn bộ test xanh trong CI; các case 401/403/404/409/422 đều được kiểm.

### [ ] T-4.3 — Hoàn thiện GitHub Actions CI
- Owner: D   Depends on: T-0.3   Ước lượng: 15′
- Refs: [GIT_WORKFLOW.md §5](./GIT_WORKFLOW.md#5-github-actions-ci)
- Việc: duy trì `backend-ci.yml` chạy Maven verify, full-stack Docker smoke và
  `frontend-ci.yml` chạy npm verify trên PR/push vào `develop`/`main`; GitHub-hosted
  runner dùng Docker cho Testcontainers PostgreSQL và Compose smoke test. Gắn cả ba
  required status check vào branch protection.
- DoD: mở PR → `Backend verify`, `Frontend verify` và `Full-stack Docker smoke` đều chạy; PR fail một
  quality gate không merge được; badge CI xanh trên README.
- Trạng thái: cả ba check đã xanh trên PR #13; còn thao tác GitHub thủ công là thêm
  `Full-stack Docker smoke` vào required checks của `main` và `develop` (và xác nhận badge nếu dùng).

### [ ] T-4.4 — Endpoint/lệnh reset demo
- Owner: A   Depends on: T-1.10   Ước lượng: 10′
- Refs: [ERD.md §8](./ERD.md#8-demo-seed-v2__seed_demosql), [kế hoạch §13](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#13-quản-lý-rủi-ro-demo)
- Việc: cách reset dữ liệu về seed sạch (documented: `docker compose down -v && up`, hoặc endpoint `POST /api/admin/reset-demo` chỉ profile demo).
- DoD: chạy reset → dữ liệu về đúng seed ban đầu; demo lặp lại nhất quán.
- Trạng thái: lệnh reset đã document trong ERD/README; chưa ghi nhận hai lần reset/replay
  liên tiếp nên chưa tick.

### [x] T-4.5 — Contract test OpenAPI cuối cùng
- Owner: D   Depends on: M3   Ước lượng: 20′
- Refs: [OPENAPI.md §1](./OPENAPI.md#1-cách-xem--dùng-đặc-tả), [openapi.yaml](./openapi.yaml)
- Việc: coi `openapi.yaml` là canonical; viết test parse YAML và đối chiếu runtime `/v3/api-docs` theo `path + method + response status`. Dùng fixture/login để bảo đảm endpoint thực sự có thể gọi theo contract.
- DoD: CI fail khi runtime contract lệch YAML; Swagger UI đủ endpoint Auth/Member/Request/Alert/LLM; một PR đổi contract buộc cập nhật YAML + DTO/test cùng lúc.
- Trạng thái: `FoundationIT.runtimeOpenApiOperationsAndStatusesMatchCanonicalContract` so
  khớp chính xác path + method + response statuses của 18 operations.

> **🚩 Milestone M4:** backend production-ready, chạy 1 lệnh, CI xanh. Đủ điều kiện demo dù không có FE.

---

## PHASE 5 — Frontend React (FE, làm SAU khi có API chạy)

> ⚠️ Chỉ bắt đầu khi backend endpoint tương ứng đã chạy (M2 trở đi). **Không hi sinh giờ backend cho FE.** Thứ tự cắt được nếu thiếu giờ: giữ tối thiểu T-5.1→T-5.5.

### [x] T-5.1 — Setup FE (Vite+TS+antd+router+RQ+apiClient+Auth)
- Owner: FE   Depends on: T-0.2   Ước lượng: 40′
- Refs: [FRONTEND.md §2,§3,§6,§7](./FRONTEND.md#3-cấu-trúc-thư-mục)
- Việc: dựng Vite + React + TypeScript trực tiếp trong `FE/` (giữ `FE/ui_design_specification/`); cài antd, @tanstack/react-query, axios, react-router-dom. Tạo `apiClient.ts` (interceptor JWT + refresh single-flight + unwrap envelope), `queryClient.ts`, `types/api.ts` (§5), `AuthContext` + route guard, `AppLayout` + providers, `.env.example`, lint/typecheck/test/build scripts.
- DoD: `npm run dev` chạy `:5173`; app render layout; chưa login → redirect `/login`.
  Hoàn tất thêm lint/typecheck/Vitest/build gate, explicit Request demo mode và workflow
  `Frontend verify`.

### [x] T-5.2 — LoginPage
- Owner: FE   Depends on: T-5.1, T-1.9   Ước lượng: 20′
- Refs: [FRONTEND.md §10](./FRONTEND.md#10-đặc-tả-từng-trang)
- Việc: Form login → `login()` → lưu token+role → điều hướng `/requests`. Lỗi 401 → message.error.
- DoD: login 3 role thành công; sai password hiện lỗi; token gắn vào các request sau (kiểm Network).

### [ ] T-5.3 — RequestListPage (Table + filter + paging server-side)
- Owner: FE   Depends on: T-5.2, T-2.B3   Ước lượng: 40′
- Refs: [FRONTEND.md §8,§10](./FRONTEND.md#8-data-fetching-với-tanstack-query)
- Việc: `useRequests` + antd `Table` (StatusTag/Priority tag), filter bar (status/category/priority/keyword), pagination + sort map sang backend. Nút "Tạo request" chỉ CLIENT.
- DoD: 3 role thấy đúng phạm vi; filter/sort/paging gọi backend đúng; loading/empty state ok.
- Trạng thái FE: UI responsive + URL filter/sort/page + OpenAPI-aligned API hook + explicit
  demo adapter đã xong; Request backend đã merge và API mode là mặc định. Giữ task mở đến
  khi kiểm chứng acceptance phạm vi đủ 3 role trên FE/API thật.

### [ ] T-5.4 — RequestDetailPage (history timeline + assign + status + AI summary)
- Owner: FE   Depends on: T-5.3, T-2.C3, T-2.C4, T-2.C2   Ước lượng: 45′
- Refs: [FRONTEND.md §10](./FRONTEND.md#10-đặc-tả-từng-trang)
- Việc: chi tiết + `Timeline` history. ADMIN: Modal gán (auto switch / chọn dev). ADMIN/DEV: nút đổi status **chỉ hiện transition hợp lệ**; 409 → message. Nút "AI tóm tắt".
- DoD: gán & đổi status hoạt động, invalidate query → UI cập nhật + chuông cập nhật; nút status sai luật không hiện; 409 hiển thị message.
- Trạng thái FE: detail và history đã gọi API thật; workflow/assignment/AI backend cũng đã
  merge. Task còn mở vì FE mutations và nút AI summary chưa được nối.

### [ ] T-5.5 — RequestCreatePage + nút AI gợi ý
- Owner: FE   Depends on: T-5.2, T-2.B2, T-3.2   Ước lượng: 30′
- Refs: [FRONTEND.md §10](./FRONTEND.md#10-đặc-tả-từng-trang), [LLM.md](./LLM.md)
- Việc: Form tạo (validation khớp backend). Nút "AI gợi ý" → classify + suggest-priority → điền sẵn + show confidence/reason → **người dùng xác nhận** rồi submit.
- DoD: CLIENT tạo được → về danh sách; AI gợi ý điền category/priority nhưng người dùng vẫn sửa được trước submit.

### [ ] T-5.6 — AlertBell + AlertListPage
- Owner: FE   Depends on: T-5.2, T-2.D2   Ước lượng: 30′
- Refs: [FRONTEND.md §10](./FRONTEND.md#10-đặc-tả-từng-trang)
- Việc: `AlertBell` header (Badge đếm chưa đọc, poll ~15s / refetch sau mutation), popover danh sách, click → mark read + đi tới request.
- DoD: chuông hiển thị số alert chưa đọc; click đánh dấu đọc → badge giảm; điều hướng đúng request.

### [ ] T-5.7 — StatsDashboardPage (charts)
- Owner: FE   Depends on: T-5.2, T-3.3   Ước lượng: 30′
- Refs: [FRONTEND.md §10](./FRONTEND.md#10-đặc-tả-từng-trang)
- Việc: card total/completed/completionRate + pie theo category + bar theo developer. Chỉ ADMIN.
- DoD: số & biểu đồ khớp `/stats`; non-ADMIN không vào được route.

### [x] T-5.8 — Member pages + Register
- Owner: FE   Depends on: T-5.2, T-1.8   Ước lượng: 25′
- Refs: [FRONTEND.md §10](./FRONTEND.md#10-đặc-tả-từng-trang)
- Việc: RegisterPage (public), MemberListPage + MemberDetailPage (ADMIN).
- DoD: đăng ký tạo member; ADMIN xem danh sách/chi tiết; non-ADMIN không vào.

### [x] T-5.9 — FE Docker + thêm vào compose + CORS
- Owner: FE + D   Depends on: T-5.1, T-4.1   Ước lượng: 20′
- Refs: [FRONTEND.md §12](./FRONTEND.md#12-cấu-hình-chạy--docker)
- Việc: `FE/Dockerfile` (build→nginx) + `FE/nginx.conf` (SPA fallback). Thêm service `frontend` vào compose. Xác nhận `CorsConfig` backend cho origin FE.
- DoD: `docker compose up --build` chạy cả frontend+app+db; mở FE gọi được API không lỗi CORS.
  Root compose dùng Nginx reverse proxy `/api` → `backend:8080`, có healthcheck cho cả ba
  service và vẫn giữ `BE/compose.yaml` cho workflow chỉ chạy backend.

> **🚩 Milestone M5:** FE chạy full, demo được qua giao diện.

---

## PHASE 6 — Chuẩn bị demo & thuyết trình (ALL, ~100′)

### [ ] T-6.1 — Chốt kịch bản demo & rehearsal Swagger + FE
- Owner: ALL   Depends on: M4 (M5 nếu có FE)   Ước lượng: 30′
- Refs: [README §6](./README.md), [kế hoạch §12.1](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md)
- Việc: chạy thử kịch bản 9 bước (Swagger) + qua FE; canh thời gian; phân ai bấm gì. Chuẩn bị mock mode LLM (llm.enabled=false) để demo không chết.
- DoD: chạy trọn kịch bản không lỗi ≥2 lần liên tiếp; có phương án dự phòng (mock, seed reset).

### [ ] T-6.2 — Chụp bằng chứng Git Flow
- Owner: D   Depends on: M4   Ước lượng: 15′
- Refs: [GIT_WORKFLOW.md §8](./GIT_WORKFLOW.md#8-chuẩn-bị-bằng-chứng-cho-slide-10)
- Việc: chụp network graph nhánh, danh sách PR merged + review, branch protection rule, CI xanh (và 1 PR từng đỏ→xanh), 1 collaboration issue đã gặp + cách giải quyết.
- DoD: đủ ảnh cho slide 10.

### [ ] T-6.3 — Làm slide PPT (12 slide)
- Owner: ALL (D lead tổng hợp; mỗi người viết phần mình)   Depends on: M4   Ước lượng: 80′
- Refs: [kế hoạch §14](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#14-cấu-trúc-ppt-thuyết-trình) (cấu trúc + câu chốt)
- Việc: 12 slide theo cấu trúc đề. Mỗi slide kỹ thuật có **1 câu "chọn X vì Y, đánh đổi Z"** (dùng ADR ở [ARCHITECTURE.md §8](./ARCHITECTURE.md#8-nhật-ký-quyết-định-kỹ-thuật-adr) làm kho đạn). Slide 3 dùng ảnh ERD (T-0.1); slide 9 ảnh Swagger/FE; slide 10 ảnh Git (T-6.2); slide 8 prompt + output thật.
- DoD: slide đủ 12 mục; mỗi thành viên trình được phần mình; không slide nào "chỉ đọc code".

### [ ] T-6.4 — Đối chiếu rubric lần cuối
- Owner: ALL   Depends on: T-6.3   Ước lượng: 15′
- Refs: [kế hoạch §15](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#15-checklist-đối-chiếu-rubric), [ANALYZE.md §9](./ANALYZE.md#9-acceptance-criteria--truy-vết-rubric)
- Việc: đi qua từng dòng rubric, xác nhận đạt "Hire: YES"; chuẩn bị câu trả lời cho câu hỏi Q&A hay gặp (edge case [BUSINESS_LOGIC.md §9](./BUSINESS_LOGIC.md#9-bảng-edge-case-tổng-hợp)).
- DoD: checklist rubric tick hết; mỗi tiêu chí có bằng chứng demo được.

---

## Bảng tổng quan phụ thuộc & milestone

```
Phase 0 (ALL) ──▶ Phase 1 (A) ──[M1]──▶ Phase 2 (B/C/D) ──[M2]──▶ Phase 3 (D,B) ──[M3]──▶ Phase 4 (ALL) ──[M4]──┐
                                                             │                                                    │
                                                             └──▶ Phase 5 FE (T-5.*) ──[M5]───────────────────────┤
                                                                                                                  ▼
                                                                                                    Phase 6 Demo/PPT (ALL)
```

| Milestone | Điều kiện | Ý nghĩa |
|---|---|---|
| **M1** | Phase 1 merged | Nền tảng sẵn sàng, B/C/D bắt đầu |
| **M2** | Phase 2 merged | Luồng cốt lõi chạy end-to-end |
| **M3** | Phase 3 merged | Backend đủ tính năng |
| **M4** | Phase 4 done | Chạy 1 lệnh, CI xanh — **đủ điều kiện demo** |
| **M5** | Phase 5 done | FE full (bonus) |

## Thứ tự ưu tiên khi thiếu thời gian (cắt từ dưới lên)
1. **Không bao giờ cắt:** Phase 0,1,2 + T-4.1 (docker) + T-4.2 (test luồng chính) + T-6 (demo/PPT). Đây là phần được chấm.
2. Cắt được: Phase 5 (FE) — giữ backend + Swagger là đủ demo.
3. Nếu buộc phải giảm phạm vi, giữ ít nhất classify; nhưng contract hiện tại yêu cầu đủ cả classify, suggest-priority và summary nên chỉ được cắt qua PR thay đổi OpenAPI + FE + test đồng thời.
4. Cắt được: stretch của [kế hoạch §9](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#9-tính-năng-bổ-sung-giá-trị-gia-tăng) (SSE, dashboard tĩnh).

---

*Cập nhật task khi phát sinh. Mỗi PR nên tham chiếu task ID (vd "T-2.C3") để truy vết. Đối chiếu tiến độ theo milestone.*
