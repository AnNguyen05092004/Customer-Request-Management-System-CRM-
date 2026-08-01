# Bzcom CRM — Customer Request Management System
### Tài liệu Phân tích · Thiết kế · Kế hoạch triển khai (OJT 2026 KITS Hanoi)

> **Mục tiêu:** Xây dựng backend REST API quản lý yêu cầu khách hàng (bug / feature / inquiry) cho Bzcom, đạt mức **"Hire: YES"** ở mọi tiêu chí chấm điểm, sản phẩm hoàn chỉnh & ấn tượng để demo tại lễ tốt nghiệp.
>
> **Nhóm:** 4 thành viên · **Track:** SW Developer · **Công ty:** Bzcom (Korea–Vietnam Web/App Dev & Operations)

---

## Mục lục

1. [Tư duy chiến lược & bản chất bài toán](#1-tư-duy-chiến-lược--bản-chất-bài-toán)
2. [Phân tích yêu cầu](#2-phân-tích-yêu-cầu)
3. [Tech Stack](#3-tech-stack)
4. [Kiến trúc hệ thống](#4-kiến-trúc-hệ-thống)
5. [Thiết kế Database (ERD)](#5-thiết-kế-database-erd)
6. [API Design chi tiết](#6-api-design-chi-tiết)
7. [Business Logic cốt lõi](#7-business-logic-cốt-lõi)
8. [Tính năng LLM](#8-tính-năng-llm)
9. [Tính năng bổ sung (giá trị gia tăng)](#9-tính-năng-bổ-sung-giá-trị-gia-tăng)
10. [Phân công nhóm 4 người](#10-phân-công-nhóm-4-người)
11. [Git Flow & CI/CD](#11-git-flow--cicd)
12. [Kế hoạch triển khai theo phase](#12-kế-hoạch-triển-khai-theo-phase)
13. [Quản lý rủi ro demo](#13-quản-lý-rủi-ro-demo)
14. [Cấu trúc PPT thuyết trình](#14-cấu-trúc-ppt-thuyết-trình)
15. [Checklist đối chiếu rubric](#15-checklist-đối-chiếu-rubric)
16. [Phụ lục: mẫu code nền](#16-phụ-lục-mẫu-code-nền)

---

## 1. Tư duy chiến lược & bản chất bài toán

### 1.1 Bảng rubric = kim chỉ nam
Bảng **"Hiring Evaluation Criteria"** ở cuối đề chính là thước đo tuyển dụng. Giám khảo **không** chấm số lượng feature — họ chấm việc nhóm **hiểu và giải thích được quyết định kỹ thuật**. Nguyên tắc xuyên suốt: *mỗi tính năng phải map được vào một tiêu chí, hoặc phục vụ việc "explain design intent".*

| Tiêu chí | "Hire: YES" | "Hire: RECONSIDER" |
|---|---|---|
| ERD Design | Quan hệ chuẩn hóa, giải thích rõ | Chỉ liệt kê field, không nêu quan hệ |
| REST API | Đúng nguyên tắc RESTful, đúng HTTP status | Mọi thứ dùng POST, verb nằm trong URL |
| JWT Auth | Hiểu token flow, có phân quyền theo role | API mở toang, không auth |
| Business Logic | Auto-assign, transition, history chạy đúng | Lỗi logic hoặc không xử lý exception |
| LLM Prompt | Có purpose, context, output format rõ ràng | Prompt chung chung kiểu "analyze this" |
| Git Flow | Branch, PR, commit convention thực thi thật | Push thẳng main, commit vô nghĩa |
| PPT | Giải thích được intent & quyết định kỹ thuật | Chỉ đọc code, không giải thích |

### 1.2 Bản chất bài toán
Đây là hệ thống **ticketing / CRM nội bộ**. Ba trục nghiệp vụ:
- **Phân quyền theo vai trò**: ADMIN / DEVELOPER / CLIENT nhìn thấy dữ liệu khác nhau.
- **Vòng đời trạng thái có luật** (state machine): `PENDING → IN_PROGRESS → DONE`, cấm nhảy cóc & cấm lùi.
- **Tự động hóa**: auto-assign developer, auto-record history, auto-generate alert.

### 1.3 Điều chỉnh so với đề
Đề viết cho **5 người (A–E)**, nhóm ta có **4** → gộp lại vai trò (xem [mục 10](#10-phân-công-nhóm-4-người)).

---

## 2. Phân tích yêu cầu

### 2.1 Yêu cầu chức năng (Functional Requirements)

| ID | Nhóm | Mô tả |
|---|---|---|
| FR-01 | Auth | Đăng nhập cấp JWT access token (+ refresh token); đăng xuất |
| FR-02 | Auth | Xác thực token trên mọi request được bảo vệ |
| FR-03 | Auth | Phân quyền theo role (ADMIN / DEVELOPER / CLIENT) |
| FR-04 | Member | Đăng ký member; lấy danh sách (ADMIN); lấy chi tiết |
| FR-05 | Request | CLIENT tạo yêu cầu mới |
| FR-06 | Request | Lấy danh sách yêu cầu: phân trang + sắp xếp + filter tổ hợp, giới hạn theo role |
| FR-07 | Request | Lấy chi tiết một yêu cầu |
| FR-08 | Request | ADMIN gán developer (thủ công hoặc auto-assign) |
| FR-09 | Request | Cập nhật trạng thái, chặn transition không hợp lệ |
| FR-10 | Request | Lấy lịch sử thay đổi trạng thái |
| FR-11 | Request | Thống kê (tổng, tỉ lệ hoàn thành, theo category, theo developer) |
| FR-12 | Alert | Lấy danh sách alert của bản thân; đánh dấu đã đọc |
| FR-13 | Alert | Tự sinh alert: HIGH priority → tất cả ADMIN; assigned → developer; status changed → client |
| FR-14 | History | Tự ghi lịch sử khi đổi status hoặc gán developer |
| FR-15 | LLM | ≥1 tính năng: auto-classify / suggest-priority / auto-summary |

### 2.2 Yêu cầu phi chức năng (Non-Functional Requirements)

| ID | Mô tả |
|---|---|
| NFR-01 | Response format thống nhất: `{ "status", "message", "data" }` |
| NFR-02 | Swagger/OpenAPI documentation cho **mọi** endpoint (bắt buộc) |
| NFR-03 | HTTP status code đúng chuẩn (200/201/400/401/403/404/409...) |
| NFR-04 | Password lưu dạng BCrypt hash, không plaintext |
| NFR-05 | Xử lý exception tập trung (global handler) |
| NFR-06 | Validate input trước khi lưu |
| NFR-07 | Chống race condition khi cập nhật đồng thời (optimistic lock) |
| NFR-08 | Chạy được bằng một lệnh (Docker Compose) |
| NFR-09 | Có seed data để demo & test |

---

## 3. Tech Stack

| Lớp | Công nghệ | Phiên bản gợi ý | Lý do |
|---|---|---|---|
| Ngôn ngữ | Java | 21 (LTS) | Bản LTS mới nhất, hỗ trợ virtual threads / pattern matching |
| Framework | Spring Boot | 3.5.5 | Phiên bản khóa trong Maven Wrapper/pom; hệ sinh thái mạnh, đội đã quen |
| Web | Spring Web (REST) | — | REST controller |
| Bảo mật | Spring Security 6 + JWT (`io.jsonwebtoken:jjwt` 0.12.x) | — | Bắt buộc theo đề |
| ORM | Spring Data JPA + Hibernate | — | Map ERD → Entity |
| Database | PostgreSQL | 16 | Chạy qua Docker |
| DB (integration test) | Testcontainers PostgreSQL 16 | — | Chạy Flyway/truy vấn trên đúng dialect production |
| Migration | Flyway | — | Version hoá schema |
| API Docs | springdoc-openapi | 2.x | Swagger UI tự sinh |
| Mapping | MapStruct | 1.5.x | DTO ↔ Entity |
| Boilerplate | Lombok | — | Giảm getter/setter |
| Validation | Jakarta Bean Validation | — | `@Valid` |
| Test | JUnit 5 + Mockito (+ Testcontainers) | — | Unit + integration |
| LLM | OpenAI API **hoặc** Anthropic Claude API | — | Qua `LlmService` trừu tượng |
| Đóng gói | Docker + Docker Compose | — | Demo "1 lệnh chạy" |
| CI | GitHub Actions | — | Build + test trên mỗi PR |
| Build tool | Maven (hoặc Gradle) | — | Quản lý dependency |

### 3.1 Ghi chú về Frontend
Đề là **backend-oriented**; **Swagger UI chính là phần demo API**. **Không** làm full frontend (rủi ro tốn giờ). Nếu dư thời gian ở cuối → một trang dashboard tĩnh hiển thị thống kê là "wow" vừa đủ (xếp vào *stretch goal*).

### 3.2 Dependencies chính (Maven)
```
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
postgresql (runtime)
org.testcontainers:postgresql (test)
flyway-core
io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson
org.springdoc:springdoc-openapi-starter-webmvc-ui
org.mapstruct:mapstruct + mapstruct-processor
org.projectlombok:lombok
```

---

## 4. Kiến trúc hệ thống

### 4.1 Xử lý yêu cầu "MSA Concepts"
Đề ghi *"MVC Pattern / MSA Concepts"*. Với 4 người trong khung giờ này, **microservices thật là quá sức và rủi ro**. Giải pháp thông minh (và nói được trong PPT):

> Triển khai **Modular Monolith** — áp dụng *tư duy* MSA (bounded context rõ ràng, mỗi domain là một module độc lập, giao tiếp qua interface, dễ tách service về sau) nhưng deploy như một khối duy nhất. **Giải thích được trade-off này = ghi điểm "technical decision".**

### 4.2 Kiến trúc phân lớp
```
Client (Swagger / Postman)
        │  HTTP + JWT
        ▼
┌─────────────────────────┐
│  Controller  (REST API) │  ← nhận request, validate, trả ApiResponse
├─────────────────────────┤
│  Service    (Business)  │  ← logic nghiệp vụ, transaction
├─────────────────────────┤
│  Repository (Data)      │  ← Spring Data JPA
├─────────────────────────┤
│  Entity / Database      │  ← PostgreSQL
└─────────────────────────┘
Cross-cutting: Security(JWT) · ExceptionHandler · ApiResponse · Mapper · Config
```

### 4.3 Cấu trúc package (package-by-feature)
Chia theo **tính năng** (không theo layer) để **mỗi người sở hữu 1 package → giảm xung đột merge** — hỗ trợ trực tiếp tiêu chí Git Flow.

```
com.bzcom.crm
├── CrmApplication.java
├── common/                         (A - dùng chung)
│   ├── response/ApiResponse.java   # wrapper {status,message,data}
│   ├── exception/                  # BusinessException + subclasses
│   ├── exception/ErrorCode.java     # mã lỗi ổn định ↔ HTTP status
│   ├── exception/GlobalExceptionHandler.java
│   └── entity/BaseTimeEntity.java  # createdAt/updatedAt (JPA Auditing)
├── config/                         (A)
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── JpaAuditingConfig.java
├── auth/                           (A)
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── security/CurrentUser.java    # memberId + role từ JWT
│   ├── jwt/JwtProvider.java, JwtAuthenticationFilter.java
│   └── dto/request/ + dto/response/
├── member/                         (A)
│   ├── controller/ service/ repository/ entity/ dto/{request,response}/ mapper/
├── request/                        (B)
│   ├── controller/RequestController.java
│   ├── service/RequestService.java, RequestStatsService.java
│   ├── repository/RequestRepository.java (+ Specification)
│   ├── entity/Request.java
│   └── dto/{request,response}/ mapper/
├── workflow/                       (C - logic nặng nhất)
│   ├── service/AssignService.java, StatusService.java, HistoryService.java
│   ├── entity/RequestHistory.java
│   ├── domain/RequestStatus.java (enum + state machine)
│   └── dto/{request,response}/ mapper/
├── alert/                          (D)
│   ├── controller/ service/AlertService.java
│   ├── entity/Alert.java
│   └── dto/
└── llm/                            (D)
    ├── service/LlmService.java (interface) + impl + MockLlmService
    ├── prompt/ClassifyPrompt.java
    └── dto/
```

---

## 5. Thiết kế Database (ERD)

### 5.1 Mô tả các bảng

**members** — người dùng hệ thống
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | bigint PK | auto increment |
| email | varchar unique | login id |
| password | varchar | **BCrypt hash** |
| name | varchar | |
| role | varchar | ADMIN / DEVELOPER / CLIENT |
| last_completed_at | timestamptz | tie-break cho auto-assign |
| created_at | timestamptz | UTC instant |

**requests** — yêu cầu khách hàng
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | bigint PK | |
| title | varchar | |
| description | text | |
| category | varchar | BUG / FEATURE / INQUIRY |
| priority | varchar | HIGH / MEDIUM / LOW |
| status | varchar | PENDING / IN_PROGRESS / DONE (default PENDING) |
| client_id | bigint FK → members.id | người tạo |
| assigned_developer_id | bigint FK → members.id | nullable |
| version | int | **optimistic lock** (@Version) |
| created_at / updated_at | timestamptz | UTC instant, JPA Auditing |

**request_histories** — lịch sử thay đổi
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | bigint PK | |
| request_id | bigint FK → requests.id | |
| changed_by | bigint FK → members.id | ai thay đổi |
| from_status / to_status | varchar | nullable (assign thì status không đổi) |
| changed_at | timestamptz | UTC instant |
| memo | varchar | mô tả thay đổi |

**alerts** — thông báo
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | bigint PK | |
| request_id | bigint FK → requests.id | |
| target_member_id | bigint FK → members.id | người nhận |
| alert_type | varchar | ASSIGNED / STATUS_CHANGED / HIGH_PRIORITY_REGISTERED |
| message | varchar | |
| is_read | boolean | default false |
| created_at | timestamptz | UTC instant |

**refresh_tokens** — phiên refresh token có thể thu hồi
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | uuid PK | định danh phiên |
| member_id | bigint FK → members.id | chủ sở hữu |
| token_hash | varchar(64) unique | SHA-256 của opaque refresh token, không lưu raw token |
| expires_at / revoked_at / created_at | timestamptz | UTC instant; rotation/logout dùng `revoked_at` |

### 5.2 Quan hệ (giải thích được trong PPT)
- `members (1) ── (N) requests` qua **client_id** (một client tạo nhiều request).
- `members (1) ── (N) requests` qua **assigned_developer_id** (một developer nhận nhiều request). → **hai quan hệ khác nhau tới cùng bảng members**, cần nêu rõ.
- `requests (1) ── (N) request_histories`: mỗi request có nhiều mốc lịch sử.
- `requests (1) ── (N) alerts`, `members (1) ── (N) alerts` (target).
- `members (1) ── (N) refresh_tokens`: một người có thể có nhiều phiên; chỉ hash token được lưu.

### 5.3 Quyết định thiết kế cần nhấn mạnh
- **`currentTaskCount` KHÔNG lưu cột riêng** mà **tính động** (`COUNT` request đang gán & chưa DONE) → tránh dữ liệu lệch (denormalization risk). `last_completed_at` chỉ lưu để tie-break.
- **`version`** cho optimistic locking → chống 2 người đổi status cùng lúc. (Pattern 更新カウンタ / optimistic lock.)
- **Index** trên `requests.status`, `requests.assigned_developer_id`, `alerts.target_member_id`, `alerts.is_read`.
- **Refresh token opaque + rotation**: access JWT 15 phút vẫn stateless; refresh token 7 ngày được hash, rotate/revoke để logout có nghĩa thực tế.

### 5.4 File DBML (paste vào dbdiagram.io → ra ERD ngay)
```dbml
Table members {
  id bigint [pk, increment]
  email varchar [unique, not null]
  password varchar [not null, note: 'BCrypt hash']
  name varchar [not null]
  role varchar [not null, note: 'ADMIN / DEVELOPER / CLIENT']
  last_completed_at timestamptz [note: 'tie-break auto-assign']
  created_at timestamptz [not null]
}

Table requests {
  id bigint [pk, increment]
  title varchar [not null]
  description text
  category varchar [not null, note: 'BUG / FEATURE / INQUIRY']
  priority varchar [not null, note: 'HIGH / MEDIUM / LOW']
  status varchar [not null, default: 'PENDING', note: 'PENDING / IN_PROGRESS / DONE']
  client_id bigint [not null, ref: > members.id]
  assigned_developer_id bigint [ref: > members.id]
  version int [not null, default: 0, note: 'optimistic lock']
  created_at timestamptz [not null]
  updated_at timestamptz [not null]
}

Table request_histories {
  id bigint [pk, increment]
  request_id bigint [not null, ref: > requests.id]
  changed_by bigint [not null, ref: > members.id]
  from_status varchar
  to_status varchar
  changed_at timestamptz [not null]
  memo varchar
}

Table alerts {
  id bigint [pk, increment]
  request_id bigint [not null, ref: > requests.id]
  target_member_id bigint [not null, ref: > members.id]
  alert_type varchar [not null, note: 'ASSIGNED / STATUS_CHANGED / HIGH_PRIORITY_REGISTERED']
  message varchar
  is_read boolean [not null, default: false]
  created_at timestamptz [not null]
}

Table refresh_tokens {
  id uuid [pk]
  member_id bigint [not null, ref: > members.id]
  token_hash varchar [not null, unique]
  expires_at timestamptz [not null]
  revoked_at timestamptz
  created_at timestamptz [not null]
}
```

---

## 6. API Design chi tiết

### 6.1 Quy ước chung
- **Base URL:** `/api`
- **Response format thống nhất:**
```json
{ "status": 200, "message": "success", "data": { } }
```
- **Header auth:** `Authorization: Bearer <accessToken>` (trừ login/refresh/logout/register)
- **HTTP status:** 200 OK · 201 Created · 400 Bad Request (validation) · 401 Unauthorized (chưa/không hợp lệ token) · 403 Forbidden (sai quyền) · 404 Not Found · 409 Conflict (invalid transition / optimistic lock).

### 6.2 Auth API
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/auth/login` | public | Đăng nhập, cấp JWT |
| POST | `/api/auth/refresh` | refresh token body | Rotation, cấp token mới |
| POST | `/api/auth/logout` | refresh token body | Thu hồi refresh token |

**POST /api/auth/login**
```
Request:  { "email": "admin@bzcom.com", "password": "1234" }
Response: { "status":200, "message":"success",
            "data": { "accessToken":"...", "refreshToken":"...",
                      "tokenType":"Bearer", "role":"ADMIN" } }
```

### 6.3 Member API
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/members` | public (đăng ký CLIENT) | Server ép role CLIENT → 201 |
| GET | `/api/members` | ADMIN | Lấy tất cả member |
| GET | `/api/members/{id}` | ADMIN hoặc chính member đó | Lấy chi tiết member |

**POST /api/members**
```
Request:  { "email":"client2@bzcom.com", "password":"1234", "name":"Client Two" }
Response 201: { "status":201, "message":"created", "data": { "id":5, "email":"...", "name":"...", "role":"CLIENT" } }
```
> Không bao giờ trả `password` trong response.

### 6.4 Request API
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/requests` | CLIENT | Tạo yêu cầu → 201 |
| GET | `/api/requests` | all (lọc theo role) | Danh sách: phân trang/sort/filter |
| GET | `/api/requests/{id}` | all (kiểm quyền xem) | Chi tiết |
| PATCH | `/api/requests/{id}/assign` | ADMIN | Gán developer (auto/thủ công) |
| PATCH | `/api/requests/{id}/status` | ADMIN/DEVELOPER | Đổi trạng thái (phải đã gán developer) |
| GET | `/api/requests/{id}/history` | all (cùng ownership với detail) | Lịch sử |
| GET | `/api/requests/stats` | ADMIN | Thống kê |

**Phạm vi dữ liệu GET /api/requests theo role:**
- **ADMIN**: tất cả request.
- **CLIENT**: chỉ request mình tạo (`client_id = me`).
- **DEVELOPER**: chỉ request được gán cho mình (`assigned_developer_id = me`).

**GET /api/requests — query params:**
```
?page=0&size=10&sort=createdAt,desc
&status=PENDING&category=BUG&priority=HIGH
&keyword=login
```

**POST /api/requests**
```
Request:  { "title":"Login fails", "description":"...", "category":"BUG", "priority":"HIGH" }
Response 201: { "status":201, "message":"created", "data": { "id":10, "status":"PENDING", ... } }
```
> Khi tạo request `HIGH` → tự sinh alert cho tất cả ADMIN (xem mục 7.5).

**PATCH /api/requests/{id}/assign**
```
Request (auto):   { "auto": true, "expectedVersion": 0 }
Request (manual): { "auto": false, "developerId": 5, "expectedVersion": 0 }
Response: { "status":200, "message":"assigned", "data": { "id":10, "assignedDeveloperId":5, "status":"PENDING" } }
```

**PATCH /api/requests/{id}/status**
```
Request:  { "status":"IN_PROGRESS", "memo":"start working", "expectedVersion": 1 }
Response 200 (hợp lệ)
Response 409 (invalid transition): { "status":409, "message":"Invalid status transition: DONE -> IN_PROGRESS", "data":null }
```
> Request chưa được gán developer cũng trả `409`; public registration có field `role`/field lạ trả `400`.

### 6.5 Alert API
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/alerts` | authenticated | Alert của bản thân |
| PATCH | `/api/alerts/{id}/read` | owner | Đánh dấu đã đọc |

### 6.6 LLM API (đủ ba endpoint trong contract)
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/requests/classify` | Tự phân loại category từ description |
| POST | `/api/requests/suggest-priority` | Tự gợi ý priority từ description |
| GET | `/api/requests/{id}/summary` | Tóm tắt 1–2 dòng |

**POST /api/requests/classify**
```
Request:  { "description":"When I click login, page shows 500 error" }
Response: { "status":200, "message":"success",
            "data": { "category":"BUG", "confidence":0.94, "reason":"mentions error on action" } }
```

### 6.7 Bảng tổng hợp HTTP status
| Tình huống | Status |
|---|---|
| Lấy dữ liệu thành công | 200 |
| Tạo mới thành công | 201 |
| Validation sai (thiếu field, sai enum) | 400 |
| Chưa đăng nhập / token sai | 401 |
| Sai quyền (role không đủ) | 403 |
| Không tìm thấy resource | 404 |
| Transition không hợp lệ / optimistic lock | 409 |
| Lỗi server không lường trước | 500 |

---

## 7. Business Logic cốt lõi

### 7.1 JWT Authentication & Authorization
**Luồng:**
1. `POST /login` → BCrypt → access JWT 15 phút + opaque refresh token 7 ngày; DB chỉ lưu SHA-256 hash.
2. `POST /refresh` chỉ cấp cặp mới sau khi revoke token cũ bằng update nguyên tử (token còn hạn/chưa revoke); hai refresh cùng token chỉ một request thành công. `POST /logout` revoke token idempotent.
3. Mọi business request qua `JwtAuthenticationFilter`: đọc header `Authorization`, verify chữ ký + hạn, nạp `Authentication` vào `SecurityContext`.
4. Phân quyền: `@PreAuthorize("hasRole('ADMIN')")` ở method, hoặc kiểm tra ownership trong service.

**Phân quyền theo role:**
- ADMIN: toàn quyền quản lý.
- DEVELOPER: chỉ request được gán cho mình.
- CLIENT: chỉ request mình tạo.

### 7.2 Auto-Assignment Logic
```
function autoAssign(request):
    developers = members WHERE role = DEVELOPER ORDER BY id FOR UPDATE
    for each dev: dev.taskCount = COUNT(requests WHERE assigned_developer_id = dev.id AND status != DONE)
    candidates = developers sorted by taskCount ASC
    minCount = candidates[0].taskCount
    tied = candidates WHERE taskCount == minCount
    if tied.size == 1:
        chosen = tied[0]
    else:
        chosen = tied sorted by last_completed_at DESC  # hoàn thành gần nhất
                 (member chưa từng hoàn thành → xếp cuối)
    assert request.version == command.expectedVersion  # stale request → 409
    request.assigned_developer_id = chosen.id
    recordHistory(request, memo="auto-assigned to " + chosen.name)
    createAlert(target=chosen, type=ASSIGNED)
```

### 7.3 Status Transition Rules (State Machine)
| Từ | Được phép sang |
|---|---|
| PENDING | IN_PROGRESS |
| IN_PROGRESS | DONE |
| DONE | (không được sang đâu) |

- **Chặn:** `DONE → *` (lùi về bất kỳ trạng thái nào).
- **Chặn:** `PENDING → DONE` (nhảy cóc, bỏ qua IN_PROGRESS).
- Vi phạm → ném `InvalidStatusTransitionException` → HTTP **409**.
- Cài đặt gợi ý: enum `RequestStatus` chứa `Set<RequestStatus> allowedNext` (xem [phụ lục](#16-phụ-lục-mẫu-code-nền)).

### 7.4 Automatic History Recording
Mỗi khi **đổi status** hoặc **gán developer** → tự tạo bản ghi `request_histories` (from/to status, changedBy, changedAt, memo). Đặt logic này trong service, cùng transaction với thao tác chính (`@Transactional`) để đảm bảo nguyên tử.

### 7.5 Automatic Alert Generation
| Sự kiện | Người nhận | alert_type |
|---|---|---|
| Tạo request priority = HIGH | tất cả ADMIN | HIGH_PRIORITY_REGISTERED |
| Gán developer | developer được gán | ASSIGNED |
| Đổi status | client tạo request | STATUS_CHANGED |

> Đặt việc tạo alert trong cùng transaction với hành động gây ra nó.

---

## 8. Tính năng LLM

### 8.1 Nguyên tắc (để đạt "Hire: YES")
Rubric: **có purpose + context + output format rõ ràng** ↔ prompt chung chung. → Đầu tư vào **prompt có cấu trúc + output JSON + verification**.

### 8.2 Thiết kế prompt (ví dụ: auto-classify)
```
SYSTEM:
Bạn là trợ lý phân loại yêu cầu hỗ trợ của Bzcom. Phân loại mô tả của khách hàng
vào ĐÚNG một trong ba nhãn:
- BUG: lỗi/sự cố khi hệ thống đang chạy (error, crash, không hoạt động đúng).
- FEATURE: đề nghị thêm/cải tiến chức năng mới.
- INQUIRY: câu hỏi/thắc mắc, không phải lỗi cũng không phải yêu cầu tính năng.

Chỉ trả về JSON đúng định dạng, không thêm chữ nào khác:
{"category":"BUG|FEATURE|INQUIRY","confidence":0.0-1.0,"reason":"<ngắn gọn>"}

FEW-SHOT:
Input: "Nút thanh toán bấm không phản hồi" → {"category":"BUG","confidence":0.95,"reason":"chức năng không hoạt động"}
Input: "Cho tôi hỏi cách đổi mật khẩu?" → {"category":"INQUIRY","confidence":0.9,"reason":"là câu hỏi"}
Input: "Mong thêm đăng nhập bằng Google" → {"category":"FEATURE","confidence":0.92,"reason":"đề nghị tính năng mới"}

USER:
Input: "<description của request>"
```

### 8.3 Xử lý & verification
- Parse JSON an toàn (try/catch); nếu lỗi → fallback về rule đơn giản (keyword) hoặc trả `INQUIRY` + confidence thấp.
- **LLM chỉ gợi ý** — quyết định cuối do người/logic xác nhận. Nói được điều này trong PPT = điểm cộng lớn.

### 8.4 Mock mode (chống chết demo)
`LlmService` là interface, có 2 impl: `OpenAiLlmService` (thật) và `MockLlmService` (trả kết quả cố định theo keyword). Chọn qua config `llm.enabled=true/false` → **demo không bao giờ chết vì lỗi mạng/API key**.

---

## 9. Tính năng bổ sung (giá trị gia tăng)

Mỗi mục map vào một tiêu chí — **không thêm bừa**.

| Tính năng | Map vào tiêu chí |
|---|---|
| Global Exception Handler + unified response | NFR-01/05, REST API |
| State machine cho status (enum + allowedNext) | Business Logic |
| Optimistic locking (`@Version`) | Business Logic (race condition) |
| Filter tổ hợp + phân trang (JPA Specification + Pageable) | REST API |
| JPA Auditing (createdAt/updatedAt tự động) | ERD/thiết kế sạch |
| Refresh token | JWT Auth (hiểu sâu flow) |
| Seed / demo data loader (có cờ bật tắt) | Demo mượt |
| Docker Compose (app + PostgreSQL) | "1 lệnh chạy" — ấn tượng |
| GitHub Actions CI (build+test/PR) + branch protection | Git Flow |
| Spring Actuator health check | Tư duy vận hành |
| Unit/Integration test luồng chính | Chất lượng code |
| **Stretch:** SSE/WebSocket alert real-time | wow |
| **Stretch:** dashboard thống kê tĩnh | wow |

---

## 10. Phân công nhóm 4 người

> Nguyên tắc: **A dựng nền trước (front-load)**, xong sớm để hỗ trợ B/C/D. **C giữ phần logic nặng nhất**.

| Thành viên | Phụ trách | Package |
|---|---|---|
| **A — Nền tảng & Auth** | ERD lead, skeleton project, `common` (ApiResponse + GlobalExceptionHandler + BaseTimeEntity), Spring Security + JWT config, **Auth API + Member API** | `common`, `config`, `auth`, `member` |
| **B — Request core** | `POST/GET /requests`, filter tổ hợp + phân trang (Specification), **Statistics API** | `request` |
| **C — Workflow logic** | Auto-assign algorithm, state machine status, ghi history, optimistic lock | `workflow` |
| **D — Cross-cutting & AI** | Alert API + auto-alert, cấu hình Swagger/OpenAPI, **LLM feature**, kiêm **Git/CI lead** (Actions, branch protection, điều phối review PR) | `alert`, `llm` |

**PPT:** mỗi người tự viết slide phần mình; **D làm lead tổng hợp** (D thường rảnh hơn giai đoạn đầu). Slide 11 "Individual Contributions" mỗi người tự trình.

**Cân bằng tải:** A nặng đầu dự án, C nặng giữa dự án → sau khi A xong nền có thể nhảy vào phụ C.

---

## 11. Git Flow & CI/CD

### 11.1 Branch strategy
```
main      ← release cuối (chỉ merge từ develop qua PR)
develop   ← nhánh tích hợp
feature/{tên}   ← nhánh cá nhân (feature/auth-jwt, feature/request-crud, ...)
```

### 11.2 Rules (thực thi thật)
- Mỗi người làm trên feature branch riêng.
- PR vào `develop`, **bắt buộc ≥1 review** mới được merge.
- **Bật branch protection**: cấm push thẳng `main` và `develop`.
- Commit convention:
  - `feat:` tính năng mới
  - `fix:` sửa lỗi
  - `docs:` tài liệu
  - `refactor:` tái cấu trúc
  - `test:` test code

### 11.3 GitHub Actions CI (điểm cộng lớn)
Workflow chạy `cd BE && ./mvnw verify` (build + test) trên **mỗi PR** → PR fail test hiện đỏ ngay. Slide 10 chụp lịch sử PR + CI xanh = gần như ăn trọn tiêu chí Git Flow.

```yaml
# .github/workflows/ci.yml (phác thảo)
name: CI
on: { pull_request: { branches: [develop, main] } }
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: cd BE && ./mvnw -B verify
```

### 11.4 PR template
Tạo `.github/pull_request_template.md`: mô tả thay đổi + checklist (đã test? đã cập nhật Swagger? có breaking change?).

---

## 12. Kế hoạch triển khai theo phase

> Đề yêu cầu "tối thiểu 8 giờ" (ngoài giờ học), không nhất thiết dồn 1 ngày. Nếu trải nhiều buổi → càng nên đầu tư chất lượng. Thứ tự ưu tiên như dưới.

| Phase | Nội dung | Ai | Thời lượng |
|---|---|---|---|
| **0. Thống nhất (làm CHUNG)** | Chốt ERD, **hợp đồng API** (endpoint + request/response mẫu, viết ra để tránh xung đột), quy ước Git, cấu trúc package | Cả nhóm | ~60′ |
| **1. Nền tảng** | Skeleton + Maven, DB + Flyway, security scaffolding, ApiResponse + GlobalExceptionHandler, BaseTimeEntity, seed data khung | A | ~90′ |
| **2. Song song** | Request CRUD/filter (B), workflow logic (C), alert + swagger (D). A phụ C sau khi xong nền | B, C, D (+A) | ~150′ |
| **3. LLM + Stats** | LLM service + prompt + mock (D); Statistics API (B) | D, B | ~40′ |
| **4. Tích hợp** | Merge PR, seed data đầy đủ, Docker Compose, test luồng chính, fix bug | Cả nhóm | ~80′ |
| **5. Demo prep** | PPT, kịch bản demo Swagger, rehearsal | Cả nhóm | ~100′ |

> **Phase 0 không được bỏ qua** — nó quyết định việc merge sau này có xung đột hay không.

### 12.1 Kịch bản demo Swagger (diễn theo thứ tự)
1. Login ADMIN → lấy token.
2. Login CLIENT → tạo request priority **HIGH**.
3. GET `/alerts` bằng ADMIN → thấy alert HIGH_PRIORITY_REGISTERED (tự sinh).
4. ADMIN `PATCH /assign` với `auto:true` → xem thuật toán chọn developer.
5. Login DEVELOPER → thấy chỉ request của mình; `PATCH /status` PENDING→IN_PROGRESS→DONE.
6. Thử `PATCH /status` sai luật (DONE→IN_PROGRESS) → nhận **409**.
7. GET `/history` → thấy lịch sử tự ghi.
8. GET `/requests/stats` → thống kê.
9. LLM `POST /classify` với mô tả mẫu → JSON kết quả.

---

## 13. Quản lý rủi ro demo

| Rủi ro | Phòng ngừa |
|---|---|
| LLM lỗi mạng / hết API key | Mock mode (mục 8.4) |
| Dữ liệu demo lộn xộn | Seed data cố định + endpoint/lệnh reset để chạy lại sạch |
| Merge conflict phút chót | Package-by-feature + chốt API contract ở Phase 0 |
| Quên HTTP status đúng | Bảng status ở mục 6.7 làm checklist |
| Race condition khi 2 người đổi status | Optimistic lock (`@Version`) |
| Máy demo không cài được môi trường | Docker Compose — `docker compose up` là chạy |
| Trình bày "đọc code" | Mỗi slide chuẩn bị 1 câu "chọn X vì Y, đánh đổi Z" |

---

## 14. Cấu trúc PPT thuyết trình

Rubric: YES = *giải thích được design intent & technical decision*; RECONSIDER = *chỉ đọc code*. → Mỗi slide feature nói **"chọn X vì Y, đánh đổi Z"**, không đọc code.

| # | Slide | Nội dung chính |
|---|---|---|
| 1 | Cover | Tên nhóm, thành viên, tên đề, tên công ty |
| 2 | Problem Definition | Vấn đề thật của Bzcom + phạm vi giải pháp |
| 3 | ERD Design | Cấu trúc bảng + **lý do thiết kế** (vì sao 2 FK tới members, vì sao tính taskCount động, vì sao có version) |
| 4 | API Design Overview | Danh sách endpoint + nguyên tắc RESTful áp dụng |
| 5 | Feature 1 — Auth | JWT flow + phân quyền theo role |
| 6 | Feature 2 — Auto-Assign | Thuật toán + walkthrough code chính |
| 7 | Feature 3 — History & Alerts | Luật transition + luồng auto-record |
| 8 | LLM Feature | Prompt design + output thật + cách verify |
| 9 | Swagger Demo | Ảnh chụp các call API thật |
| 10 | Git Flow | Branch + PR history + CI xanh + vấn đề collaboration đã gặp |
| 11 | Individual Contributions | Phần mỗi người + bài học |
| 12 | Q&A | |

**Câu chốt gợi ý cho slide kỹ thuật:**
- Optimistic lock: *"Dùng optimistic lock thay vì pessimistic vì tần suất ghi thấp, tránh khóa DB làm chậm."*
- Auto-assign: *"Tính task count động để dữ liệu không bao giờ lệch, đổi lại chấp nhận thêm 1 query."*
- Modular monolith: *"Áp dụng tư duy MSA nhưng deploy 1 khối vì team nhỏ, thời gian ngắn — dễ tách service về sau."*

---

## 15. Checklist đối chiếu rubric

- [ ] **ERD**: quan hệ PK/FK rõ ràng, chuẩn hóa, giải thích được từng quan hệ.
- [ ] **REST API**: dùng đúng method (không all-POST), verb không nằm trong URL, HTTP status đúng.
- [ ] **Swagger**: mọi endpoint có tài liệu, response format thống nhất.
- [ ] **JWT**: login cấp token, filter verify mọi request, phân quyền 3 role hoạt động thật.
- [ ] **Business Logic**: auto-assign đúng, state machine chặn transition sai, history tự ghi, alert tự sinh — tất cả chạy đúng.
- [ ] **Exception**: có global handler, không để lỗi 500 thô.
- [ ] **LLM**: prompt có purpose/context/output format + verification + mock mode.
- [ ] **Git Flow**: branch main/develop/feature, PR có review, commit đúng convention, CI xanh, không push thẳng main.
- [ ] **Demo**: Docker Compose chạy, seed data sẵn, kịch bản demo mạch lạc.
- [ ] **PPT**: mỗi quyết định kỹ thuật có lý do & trade-off.

---

## 16. Phụ lục: mẫu code nền

### 16.1 ApiResponse (unified response)
```java
public record ApiResponse<T>(int status, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(200, "success", data); }
    public static <T> ApiResponse<T> created(T data) { return new ApiResponse<>(201, "created", data); }
    public static <T> ApiResponse<T> error(int status, String message) { return new ApiResponse<>(status, message, null); }
}
```

### 16.2 GlobalExceptionHandler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransition(InvalidStatusTransitionException e) {
        return ResponseEntity.status(409).body(ApiResponse.error(409, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(ApiResponse.error(400, msg));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, e.getMessage()));
    }
}
```

### 16.3 Enum state machine
```java
public enum RequestStatus {
    PENDING, IN_PROGRESS, DONE;

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
        PENDING,     EnumSet.of(IN_PROGRESS),
        IN_PROGRESS, EnumSet.of(DONE),
        DONE,        EnumSet.noneOf(RequestStatus.class)
    );

    public boolean canTransitionTo(RequestStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
```

### 16.4 BaseTimeEntity (JPA Auditing)
```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate  @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
// Bật bằng @EnableJpaAuditing trên class config.
```

### 16.5 Optimistic lock trên Request entity
```java
@Entity
public class Request extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) private RequestStatus status = RequestStatus.PENDING;
    @Version private int version;   // optimistic lock
    // ... các field khác
}
```

### 16.6 SecurityConfig (phác thảo)
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // cho @PreAuthorize
public class SecurityConfig {
    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

### 16.7 LlmService interface (đổi provider / mock dễ)
```java
public interface LlmService {
    ClassifyResult classify(String description);
    PriorityResult suggestPriority(String description);
    String summarize(RequestSummaryInput request);
}
// OpenAiLlmService implements LlmService  → gọi API thật
// MockLlmService  implements LlmService   → trả kết quả theo keyword (dùng khi llm.enabled=false)
```

---

*Tài liệu này là bản kế hoạch tổng thể để nhóm bám theo. Cập nhật lại khi API contract hoặc phân công thay đổi trong Phase 0.*
