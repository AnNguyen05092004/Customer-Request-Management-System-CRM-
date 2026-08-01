# ARCHITECTURE — Kiến trúc hệ thống

> Cách hệ thống **Bzcom CRM** được tổ chức và **vì sao** tổ chức như vậy. Trọng tâm của tài liệu là **giải thích quyết định kỹ thuật (design intent + trade-off)** — đúng thứ rubric chấm.
>
> Phục vụ **Slide 4 — API Design Overview** và phần Q&A kỹ thuật.

---

## Mục lục
1. [Yếu tố dẫn dắt kiến trúc](#1-yếu-tố-dẫn-dắt-kiến-trúc)
2. [Phong cách kiến trúc: Modular Monolith](#2-phong-cách-kiến-trúc-modular-monolith)
3. [Sơ đồ C4 rút gọn](#3-sơ-đồ-c4-rút-gọn)
4. [Kiến trúc phân lớp](#4-kiến-trúc-phân-lớp)
5. [Cấu trúc package (package-by-feature)](#5-cấu-trúc-package-package-by-feature)
6. [Cross-cutting concerns](#6-cross-cutting-concerns)
7. [Sequence diagram các luồng chính](#7-sequence-diagram-các-luồng-chính)
8. [Nhật ký quyết định kỹ thuật (ADR)](#8-nhật-ký-quyết-định-kỹ-thuật-adr)
9. [Cấu hình, profile & môi trường](#9-cấu-hình-profile--môi-trường)
10. [Kiến trúc triển khai (Docker)](#10-kiến-trúc-triển-khai-docker)

---

## 1. Yếu tố dẫn dắt kiến trúc

| Driver | Yêu cầu | Ảnh hưởng thiết kế |
|---|---|---|
| Rubric chấm "giải thích được" | Mỗi quyết định phải có lý do | Ưu tiên pattern **kinh điển, dễ giải thích** hơn là "xịn mà khó biện luận" |
| 4 người làm song song | Giảm merge conflict | **Package-by-feature**, mỗi người sở hữu 1 package |
| Phân quyền theo role | Kiểm soát truy cập chặt | Spring Security + JWT filter + `@PreAuthorize` |
| Vòng đời có luật | Chặn transition sai | **State machine** đặt trong domain layer |
| Đồng thời | Chặn stale PATCH, refresh-token replay và cân bằng auto-assign khi đồng thời | `expectedVersion` + **optimistic lock** (`@Version`) + revoke refresh nguyên tử + khóa ngắn khi chọn developer |
| Demo "1 lệnh chạy" | Không phụ thuộc máy | **Docker Compose** (app + PostgreSQL) |
| "MSA Concepts" trong đề | Thể hiện tư duy MSA | Modular monolith với bounded context rõ ràng |

## 2. Phong cách kiến trúc: Modular Monolith

Đề ghi *"MVC Pattern / MSA Concepts"*. Với 4 người trong ~8 giờ, **microservices thật là quá sức & rủi ro** (network, phân tán transaction, DevOps). Quyết định:

> **Modular Monolith** — áp dụng *tư duy* MSA: bounded context rõ ràng, mỗi domain là một module độc lập giao tiếp qua interface, dễ tách thành service riêng về sau — nhưng **deploy như một khối duy nhất**.

**Trade-off (câu để nói trong PPT):**
> *"Chúng em chọn modular monolith thay vì microservices vì team nhỏ và thời gian ngắn: vẫn giữ ranh giới module như MSA (mỗi domain một package, giao tiếp qua interface) nên có thể tách service về sau, nhưng tránh được chi phí vận hành phân tán mà microservices thật đòi hỏi."*

**Bounded context** (mỗi cái ≈ một microservice tiềm năng):
`auth` · `member` · `request` · `workflow` (assign/status/history) · `alert` · `llm`.

## 3. Sơ đồ C4 rút gọn

### Level 1 — System Context
```
┌──────────┐   HTTP/JWT   ┌───────────────────┐   HTTPS   ┌──────────────┐
│  ADMIN   │─────────────▶│                   │──────────▶│ LLM Provider │
│ DEVELOPER│              │   Bzcom CRM API   │           │ (OpenAI/     │
│  CLIENT  │◀─────────────│   (Spring Boot)   │◀──────────│  Claude)     │
└──────────┘   JSON       └─────────┬─────────┘           └──────────────┘
 (qua Swagger/Postman)              │ JDBC
                                    ▼
                            ┌───────────────┐
                            │  PostgreSQL   │
                            └───────────────┘
```

### Level 2 — Container
| Container | Công nghệ | Trách nhiệm |
|---|---|---|
| CRM API | Spring Boot 3.2 (Java 21) | Toàn bộ business logic, REST endpoints, Swagger |
| Database | PostgreSQL 16 | Lưu trữ bền vững; Flyway quản version schema |
| LLM Provider | OpenAI/Claude API (external) | Phân loại/tóm tắt — có **mock** thay thế khi offline |

## 4. Kiến trúc phân lớp

```
        Client (Swagger UI / Postman)
                    │  HTTP + Authorization: Bearer <JWT>
                    ▼
   ┌──────────────────────────────────────────┐
   │  Controller (REST)                        │  nhận request, @Valid, gọi service,
   │                                           │  bọc kết quả trong ApiResponse
   ├──────────────────────────────────────────┤
   │  Service (Business logic + @Transactional)│  luật nghiệp vụ, điều phối, transaction
   ├──────────────────────────────────────────┤
   │  Repository (Spring Data JPA)             │  truy vấn, Specification cho filter
   ├──────────────────────────────────────────┤
   │  Entity / Domain (JPA + state machine)    │  ánh xạ bảng, enum + luật chuyển trạng thái
   └──────────────────────────────────────────┘
                    │ JDBC
                    ▼
              PostgreSQL

  Cross-cutting (xuyên suốt mọi lớp):
  Security(JWT filter) · GlobalExceptionHandler · ApiResponse · Mapper(MapStruct) · Config · Auditing
```

**Luật phụ thuộc:** Controller → Service → Repository → Entity. **Không** cho Controller gọi thẳng Repository (để logic không rò rỉ lên lớp web). DTO tách khỏi Entity qua MapStruct → không lộ entity ra ngoài.

## 5. Cấu trúc package (package-by-feature)

Chia theo **tính năng** (không theo layer) để **mỗi người sở hữu 1 package → giảm xung đột merge** (hỗ trợ trực tiếp tiêu chí Git Flow). Chủ sở hữu ghi trong ngoặc (A/B/C/D — xem phân công ở [kế hoạch tổng thể](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#10-phân-công-nhóm-4-người)).

```
com.bzcom.crm
├── CrmApplication.java
├── common/                         (A - dùng chung)
│   ├── response/ApiResponse.java   # wrapper {status,message,data}
│   ├── response/PageResponse.java  # wrapper phân trang
│   ├── exception/                  # BusinessException + các subclass
│   │   ├── BusinessException.java
│   │   ├── ErrorCode.java          # enum mã lỗi ↔ HTTP status
│   │   └── GlobalExceptionHandler.java
│   └── entity/BaseTimeEntity.java  # createdAt/updatedAt (JPA Auditing)
├── config/                         (A)
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java          # metadata Swagger + bearer scheme
│   └── JpaAuditingConfig.java
├── auth/                           (A)
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── security/CurrentUser.java   # principal tối thiểu: memberId + role
│   ├── jwt/JwtProvider.java, JwtAuthenticationFilter.java
│   ├── entity/RefreshToken.java, repository/RefreshTokenRepository.java
│   └── dto/request/ + dto/response/
├── member/                         (A)
│   └── controller/ service/ repository/ entity/ dto/{request,response}/ mapper/
├── request/                        (B)
│   ├── controller/RequestController.java
│   ├── service/RequestService.java, RequestStatsService.java
│   ├── repository/RequestRepository.java + spec/RequestSpecification.java
│   ├── entity/Request.java
│   └── dto/{request,response}/ mapper/
├── workflow/                       (C - logic nặng nhất)
│   ├── service/AssignService.java, StatusService.java, HistoryService.java
│   ├── entity/RequestHistory.java
│   ├── domain/RequestStatus.java   # enum + state machine
│   └── dto/{request,response}/ mapper/
├── alert/                          (D)
│   ├── controller/AlertController.java
│   ├── service/AlertService.java   # điểm vào tạo alert cho các module khác gọi
│   ├── entity/Alert.java
│   └── dto/response/
└── llm/                            (D)
    ├── service/LlmService.java (interface) + OpenAiLlmService + MockLlmService
    ├── prompt/ClassifyPrompt.java
    └── dto/{request,response}/
```

**Giao tiếp giữa module:** qua **interface của service** (vd `workflow` gọi `AlertService.create(...)`, `llm` chỉ expose `LlmService`). Không module nào chạm entity của module khác trực tiếp → giữ ranh giới bounded context.

## 6. Cross-cutting concerns

| Concern | Cơ chế | Vị trí |
|---|---|---|
| **Response envelope** | `ApiResponse<T>` bọc mọi response `{status,message,data}` | `common/response` |
| **Xử lý lỗi** | `@RestControllerAdvice GlobalExceptionHandler` map exception → HTTP status + envelope | `common/exception` |
| **Bảo mật** | `JwtAuthenticationFilter` verify token mỗi request; `@PreAuthorize` phân quyền method | `auth/jwt`, `config` |
| **Current user** | JWT filter tạo `CurrentUser(memberId, role)`; controller nhận qua `@AuthenticationPrincipal`, ownership kiểm tại service | `auth/security` |
| **Auditing** | `@CreatedDate/@LastModifiedDate` qua `BaseTimeEntity` + `@EnableJpaAuditing` | `common/entity` |
| **Đồng thời** | `expectedVersion` ở API + `@Version` ở DB; refresh revoke nguyên tử; auto-assign khóa danh sách developer trong transaction | `auth`, `request`, `workflow` |
| **Validation** | Jakarta Bean Validation (`@Valid`, `@NotNull`...) ở DTO | các `dto/` |
| **Mapping** | MapStruct sinh mapper DTO↔Entity lúc compile | mỗi feature |
| **Chất lượng code** | Spotless format/import order và JaCoCo report chạy trong Maven/CI | `pom.xml`, CI |
| **API docs** | springdoc-openapi tự sinh từ annotation + `OpenApiConfig` | `config` |

## 7. Sequence diagram các luồng chính

### 7.1 Login (JWT issuance)
```
CLIENT/DEV/ADMIN        AuthController      AuthService       MemberRepository    JwtProvider
   │  POST /auth/login       │                  │                   │                 │
   │───────────────────────▶│  login(req)      │                   │                 │
   │                         │─────────────────▶│  findByEmail      │                 │
   │                         │                  │──────────────────▶│                 │
   │                         │                  │◀── member ────────│                 │
   │                         │   BCrypt.matches(password)           │                 │
   │                         │                  │── generateToken ──────────────────▶│
   │                         │                  │◀────────── accessToken+refresh ─────│
   │◀─── 200 {tokens,role} ──│◀── TokenResponse │                   │                 │
```

### 7.2 CLIENT tạo request HIGH → tự sinh alert cho ADMIN
```
CLIENT      RequestController   RequestService   RequestRepo   AlertService   MemberRepo
  │ POST /requests {HIGH}  │        │                │            │              │
  │──────────────────────▶│ create │                │            │              │
  │                        │───────▶│  @Transactional│            │              │
  │                        │        │── save ───────▶│            │              │
  │                        │        │  if priority==HIGH:         │              │
  │                        │        │───────────────────────────▶│ findAdmins   │
  │                        │        │                             │─────────────▶│
  │                        │        │  createAlert(each admin, HIGH_PRIORITY)    │
  │◀── 201 Created ────────│◀───────│  (commit: request + alerts cùng transaction)
```
> Alert nằm **cùng `@Transactional`** với việc tạo request → hoặc cả hai thành công, hoặc cả hai rollback (nguyên tử — BR-09/10).

### 7.3 Đổi status với optimistic lock + history + alert
```
DEVELOPER   RequestController   StatusService   RequestStatus(enum)   HistoryService   AlertService
  │ PATCH /{id}/status     │        │                 │                   │               │
  │──────────────────────▶│ update │  @Transactional │                   │               │
  │                        │───────▶│ check owner (assigned==me)?         │               │
  │                        │        │ canTransitionTo(next)?──▶│          │               │
  │                        │        │◀── true/false ───────────│          │               │
  │                        │        │ (false → InvalidStatusTransitionException → 409)     │
  │                        │        │ save (version++ ; conflict → 409)   │               │
  │                        │        │─── record ─────────────────────────▶│               │
  │                        │        │─── alert CLIENT ────────────────────────────────────▶│
  │◀── 200 OK ─────────────│◀───────│                                                       │
```

## 8. Nhật ký quyết định kỹ thuật (ADR)

> Định dạng ADR mini: **Quyết định — Lý do — Đánh đổi**. Đây là "kho đạn" cho Q&A.

| # | Quyết định | Lý do | Đánh đổi |
|---|---|---|---|
| ADR-01 | **Java 21 (LTS) + Spring Boot 3.2** | Bản LTS mới nhất; hệ sinh thái mạnh; hưởng tính năng mới (virtual threads, pattern matching, record patterns) | Nặng hơn Python cho demo nhỏ |
| ADR-02 | **Modular Monolith** thay vì microservices | Team nhỏ, thời gian ngắn; vẫn giữ tư duy MSA | Không thật sự scale độc lập từng service |
| ADR-03 | **Package-by-feature** | Mỗi người 1 package → ít merge conflict | Có chút lặp cấu trúc thư mục |
| ADR-04 | **PostgreSQL 16 + Testcontainers PostgreSQL** | Migration, SQL, index và transaction test trên đúng dialect production | Integration test chậm hơn H2 nhưng tránh false confidence |
| ADR-05 | **Double concurrency guard**: `expectedVersion` + `@Version`; khóa ngắn chỉ khi auto-assign | Bắt stale request lẫn race DB; cân tải nhất quán | Client cần gửi version; auto-assign đồng thời bị serialize ngắn |
| ADR-06 | **taskCount tính động** (COUNT) thay vì lưu cột | Không bao giờ lệch dữ liệu (denormalization risk) | Thêm 1 query mỗi lần auto-assign |
| ADR-07 | **State machine trong enum** (`allowedNext`) | Luật tập trung 1 chỗ, dễ test & giải thích | — |
| ADR-08 | **LlmService là interface + Mock impl** | Demo không chết vì mạng/API key; đổi provider dễ | Cần viết thêm mock |
| ADR-09 | **JWT access ngắn hạn + opaque refresh token rotation nguyên tử** | Access request vẫn stateless; logout/revoke có semantics thật, không lưu raw token và chặn refresh replay đồng thời | Access JWT đã phát hành không bị thu hồi tức thời, tối đa 15 phút |
| ADR-10 | **springdoc-openapi** tự sinh Swagger | Không lệch giữa code & docs | Cần annotate controller đầy đủ |
| ADR-11 | **DTO tách Entity qua MapStruct** | Không lộ entity, kiểm soát field trả về (giấu password) | Thêm lớp mapping |

## 9. Cấu hình, profile & môi trường

| Profile | DB | LLM | Dùng khi |
|---|---|---|---|
| `dev` | PostgreSQL Docker local | Mock | Phát triển hằng ngày, cùng dialect production |
| `docker` | PostgreSQL (container) | Mock hoặc thật (theo env) | Demo chính thức |
| `test` | Testcontainers PostgreSQL cô lập | Mock | Integration test/CI; Flyway chạy schema thật |

**Biến môi trường quan trọng** (`.env.example`):
```
POSTGRES_URL=jdbc:postgresql://db:5432/bzcom
POSTGRES_USER=bzcom
POSTGRES_PASSWORD=bzcom
JWT_SECRET=<chuỗi bí mật đủ dài>
JWT_ACCESS_EXP_MIN=15
JWT_REFRESH_EXP_DAYS=7
LLM_ENABLED=false            # false → MockLlmService (an toàn cho demo)
OPENAI_API_KEY=              # chỉ cần khi LLM_ENABLED=true
```
> Secret **không commit** vào repo; `application.yml` đọc qua `${ENV}`.

## 10. Kiến trúc triển khai (Docker)

```
┌─────────────────── docker compose ───────────────────┐
│                                                       │
│   ┌───────────────┐        ┌────────────────────┐    │
│   │   app          │  JDBC  │   db (postgres:16) │    │
│   │ (spring boot)  │───────▶│   volume: pgdata   │    │
│   │  :8080         │        │   :5432            │    │
│   └───────┬────────┘        └────────────────────┘    │
│           │ depends_on: db (healthcheck)              │
└───────────┼───────────────────────────────────────────┘
            ▼
      host :8080 → Swagger UI
```

- `Dockerfile`: multi-stage (build bằng Maven → chạy trên JRE slim) để image gọn.
- `db` có **healthcheck**; `app` `depends_on` `db` khoẻ mới start → tránh lỗi kết nối lúc khởi động.
- Flyway migrate tự động lúc app start; `V2__seed.sql` nạp seed data (bật/tắt bằng profile).
- Lệnh demo duy nhất: `docker compose up --build`.

---

*"Làm gì" xem [ANALYZE.md](./ANALYZE.md); "dữ liệu ra sao" xem [ERD.md](./ERD.md); "luật nghiệp vụ chi tiết" xem [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md).*
