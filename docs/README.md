# Bzcom CRM — Customer Request Management System

> Backend REST API quản lý yêu cầu khách hàng (bug / feature / inquiry) cho **Bzcom** — công ty vận hành & bảo trì web service cho các khách hàng Hàn Quốc.
>
> **Bài tập OJT 2026 KITS Hanoi — SW Developer Track.** Nhóm 4 người.

---

## 1. Bài toán trong một câu

Hiện tại yêu cầu của khách hàng đến qua email / KakaoTalk / điện thoại → lịch sử bị thất lạc khi đổi nhân sự, yêu cầu bị bỏ sót. Hệ thống này tập trung mọi yêu cầu vào một nơi, **phân quyền theo vai trò**, **tự động gán việc**, **ràng buộc vòng đời trạng thái**, **tự ghi lịch sử & tự sinh thông báo**, và có **1 tính năng LLM** hỗ trợ phân loại.

## 2. Chỉ mục tài liệu (đọc theo thứ tự này)

| # | Tài liệu | Nội dung | Dùng cho slide PPT |
|---|---|---|---|
| 1 | [ANALYZE.md](./ANALYZE.md) | Phân tích yêu cầu, actor, use case, business rules, acceptance criteria | Slide 2 (Problem) |
| 2 | [ARCHITECTURE.md](./ARCHITECTURE.md) | Kiến trúc phân lớp, modular monolith, sequence diagram, quyết định kỹ thuật (ADR) | Slide 4 |
| 3 | [ERD.md](./ERD.md) | Thiết kế database, quan hệ, DBML, Flyway migration | Slide 3 (ERD) |
| 4 | [OPENAPI.md](./OPENAPI.md) + [openapi.yaml](./openapi.yaml) | Đặc tả API đầy đủ, quy ước REST, ví dụ request/response | Slide 4, 9 (Swagger) |
| 5 | [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md) | State machine, auto-assign, history, alert, statistics, đồng thời | Slide 5, 6, 7 |
| 6 | [LLM.md](./LLM.md) | Thiết kế prompt, verification, mock mode | Slide 8 |
| 7 | [GIT_WORKFLOW.md](./GIT_WORKFLOW.md) | Branch strategy, commit convention, PR, CI | Slide 10 |
| 8 | [FRONTEND.md](./FRONTEND.md) | React SPA (Vite + TS + Ant Design) phủ toàn bộ API — lớp demo trực quan | Slide 9 |
| 9 | [TASKS.md](./TASKS.md) | **Task-list triển khai đầy đủ** — chia phase, có owner/phụ thuộc/DoD, giao cho thành viên hoặc AI agent | — |
| 10 | [BACKEND_CODING_RULES.md](./BACKEND_CODING_RULES.md) | Quy tắc code Java/Spring Boot, layer, security, transaction, test, PR checklist | — |
| 11 | [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | Kế hoạch foundation, thứ tự merge, lịch một tuần và checklist điều phối cho leader | — |
| 12 | [TEAM_DEVELOPMENT_GUIDE.md](./TEAM_DEVELOPMENT_GUIDE.md) | Cài đặt môi trường, bắt đầu feature branch, phối hợp code và checklist PR cho team | — |

> Bản kế hoạch tổng thể (tư duy chiến lược + phân công + kế hoạch phase) nằm ở [`../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md`](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md). Bộ `docs/` này là bản **chuyên sâu từng mảng** để implement và bảo vệ trong Q&A.

> Khi code, đọc [BACKEND_CODING_RULES.md](./BACKEND_CODING_RULES.md) cùng với contract; file này quy định layer, transaction, security, testing và review checklist.

## 3. Tech stack (tóm tắt)

**Backend:** Java 21 (LTS) · Spring Boot 3.5.5 · Spring Security 6 + JWT · Spring Data JPA · PostgreSQL 16 · Flyway · springdoc-openapi (Swagger UI) · MapStruct · Lombok · JUnit 5 · Testcontainers · Spotless · JaCoCo · Docker Compose · GitHub Actions.

**Frontend (lớp demo):** Vite · React 18 · TypeScript · Ant Design · TanStack Query · axios · React Router — chi tiết [FRONTEND.md](./FRONTEND.md).

Chi tiết & lý do chọn: xem [ARCHITECTURE.md §Tech decisions](./ARCHITECTURE.md#8-nh%E1%BA%ADt-k%C3%BD-quy%E1%BA%BFt-%C4%91%E1%BB%8Bnh-k%E1%BB%B9-thu%E1%BA%ADt-adr).

## 4. Quickstart — chạy bằng một lệnh

```bash
# Yêu cầu: Docker + Docker Compose
git clone <repo-url> bzcom-crm && cd bzcom-crm/BE
cp .env.example .env
docker compose up --build     # app + postgres khởi động, Flyway migrate, seed demo nạp
```

Sau khi lên:

| Thành phần | URL |
|---|---|
| Swagger UI (demo API) | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |

### Chạy dev với PostgreSQL local

```bash
# Chạy trong thư mục BE/. Khởi động riêng PostgreSQL trước, sau đó:
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Profile `dev` mặc định kết nối PostgreSQL container qua `localhost:5433`; có thể đổi
`POSTGRES_HOST_PORT` và `DB_URL` trong `BE/.env` khi cần.

## 5. Tài khoản seed sẵn để demo

> Mật khẩu mặc định cho mọi tài khoản seed: `1234` (chỉ dùng cho demo).

| Email | Role | Ghi chú |
|---|---|---|
| `admin@bzcom.com` | ADMIN | Toàn quyền |
| `dev1@bzcom.com` | DEVELOPER | taskCount thấp — auto-assign hay chọn |
| `dev2@bzcom.com` | DEVELOPER | |
| `client1@bzcom.com` | CLIENT | Đã có sẵn vài request để demo filter/stats |

## 6. Kịch bản demo Swagger (thứ tự trình diễn)

1. `POST /api/auth/login` bằng **ADMIN** → lấy `accessToken` và refresh token; thử `POST /api/auth/refresh` để thấy rotation.
2. Login **CLIENT** → `POST /api/requests` priority **HIGH**.
3. Login lại **ADMIN** → `GET /api/alerts` → thấy alert `HIGH_PRIORITY_REGISTERED` (tự sinh).
4. `PATCH /api/requests/{id}/assign` với `{"auto":true}` → xem thuật toán chọn developer.
5. Login **DEVELOPER** → `GET /api/requests` chỉ thấy việc của mình → `PATCH /status`: `PENDING → IN_PROGRESS → DONE`.
6. Thử `PATCH /status` sai luật (`DONE → IN_PROGRESS`) → nhận **409**.
7. `GET /api/requests/{id}/history` → lịch sử tự ghi.
8. `GET /api/requests/stats` → thống kê.
9. `POST /api/requests/classify` (LLM) → JSON kết quả phân loại.

Chi tiết mỗi bước: xem [OPENAPI.md](./OPENAPI.md).

## 7. Cấu trúc thư mục repo (dự kiến)

```
bzcom-crm/
├── docs/                     # ← bộ tài liệu này
│   ├── README.md
│   ├── ANALYZE.md
│   ├── ARCHITECTURE.md
│   ├── ERD.md
│   ├── OPENAPI.md
│   ├── openapi.yaml
│   ├── BUSINESS_LOGIC.md
│   ├── LLM.md
│   ├── GIT_WORKFLOW.md
│   ├── FRONTEND.md
│   └── BACKEND_CODING_RULES.md
├── FE/                       # frontend và UI specification
├── BE/                       # Spring Boot backend độc lập
│   ├── src/main/java/com/bzcom/crm/  # package-by-feature
│   ├── src/main/resources/
│   │   ├── db/migration/     # schema dùng ở mọi môi trường
│   │   └── db/demo/          # seed chỉ cho profile dev/docker
│   ├── src/test/java/        # unit + Testcontainers PostgreSQL
│   ├── compose.yaml
│   ├── Dockerfile
│   ├── mvnw
│   └── pom.xml
└── .github/workflows/backend-ci.yml
```

---

*Cập nhật tài liệu này khi API contract hoặc phân công thay đổi ở Phase 0.*
