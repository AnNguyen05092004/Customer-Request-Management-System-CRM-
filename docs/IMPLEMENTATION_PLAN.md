# Backend Foundation & Implementation Plan

> Kế hoạch điều phối dành cho leader khi bắt đầu code. Task chi tiết theo endpoint vẫn nằm
> trong [TASKS.md](./TASKS.md); file này mô tả thứ tự merge, điểm bàn giao và quality gate.

## 1. Kết quả của foundation

Trước khi các feature branch bắt đầu, nhánh nền phải bảo đảm:

- backend nằm độc lập trong `BE/`, build bằng Java 21 + Spring Boot 3.5.5;
- package-by-feature đã có boundary `auth`, `member`, `request`, `workflow`, `alert`, `llm`;
- PostgreSQL 16 + Flyway là nguồn schema duy nhất, JPA chỉ `validate`;
- `ApiResponse`, `PageResponse`, error mapping, auditing, JWT filter, BCrypt, CORS và
  Swagger config dùng chung đã ổn định;
- migration production tách khỏi seed demo;
- profile `prod` bắt buộc nhận DB/JWT secret qua environment và không nạp seed demo;
- Maven Wrapper, Docker Compose, Spotless, JaCoCo, unit/integration test và CI chạy từ
  ngày đầu.

Foundation không tạo controller/service giả và không triển khai thay business logic của
từng owner. Mỗi feature chỉ merge khi tuân thủ `docs/openapi.yaml`.

### Trạng thái foundation hiện tại

Phần kỹ thuật của gate F0 đã được dựng và kiểm chứng ngày **01/08/2026**:

- `cd BE && ./mvnw -B verify`: **12/12 test pass** (8 unit + 4 integration), Spotless và
  JaCoCo chạy thành công;
- integration test dùng PostgreSQL 16 thật qua Testcontainers, Flyway tạo đúng 5 bảng;
- `docker compose up -d`: PostgreSQL healthy, app health `UP`, schema + seed demo chạy đủ
  2 migration, có 4 member và 3 request mẫu;
- image backend chạy bằng user non-root `spring`; Swagger/OpenAPI runtime truy cập được;
- business endpoint chưa được dựng có chủ ý, vì vậy F1–F7 vẫn là phần việc của các owner.

F0 chỉ được coi là **merge hoàn tất** sau khi leader tạo branch/PR, CI GitHub xanh và team
review theo DoD. Không tick các task feature chỉ vì foundation đã tạo sẵn package boundary.

## 2. Thứ tự triển khai và merge

| Gate | Nội dung | Owner chính | Điều kiện mở khóa |
|---|---|---|---|
| F0 | Foundation trong `BE/` + CI | Leader/A | `./mvnw -B verify` xanh |
| F1 | Member entity/repository/API | A | migration và common contract ổn định |
| F2 | Login/refresh/logout | A | F1; test rotation/replay/revoke |
| F3 | Request entity/repository | B | F1; không thay migration V1 |
| F4 | Alert service contract | D | F1; B/C có interface để gọi |
| F5 | Request CRUD/filter/page | B | F3 + F4 |
| F6 | State/history/assign/status | C | F3 + F4 |
| F7 | Statistics + LLM mock/API | B/D | F5/F6 ổn định |
| F8 | Contract/E2E/Docker audit | ALL | mọi feature merge vào `develop` |

Luồng phụ thuộc quan trọng: `Member → Auth`, `Request entity → Request + Workflow`,
`AlertService → create HIGH + assign + status`. Vì vậy AlertService phải được merge sớm,
trước ba luồng gọi nó.

## 3. Kế hoạch thực tế trong một tuần

| Ngày | Mục tiêu | Bằng chứng cuối ngày |
|---|---|---|
| 1 | Merge foundation; A bắt đầu Member, D bắt đầu Alert contract | CI xanh, app/Flyway start được |
| 2 | Member + Auth; Request entity/repo; Alert service/API | login và protected API chạy |
| 3 | Request create/list/detail; state machine + history | unit test policy/state xanh |
| 4 | Assign/status/concurrency; statistics | luồng login→create→assign→done chạy |
| 5 | LLM mock + 3 API; rà Swagger/OpenAPI | runtime API không lệch contract |
| 6 | Integration/E2E, lỗi quyền, race refresh/optimistic lock | `verify` xanh trên `develop` |
| 7 | Docker clean-room, bugfix, demo/PPT | máy sạch chạy một lệnh |

## 4. Quy tắc làm song song

- Mỗi task dùng branch riêng từ `develop` mới nhất; PR nhỏ và tham chiếu Task ID.
- Owner không sửa file của feature khác. Thay đổi `common`, `config`, migration hoặc OpenAPI
  cần leader review vì có thể chặn toàn team.
- Không sửa `V1__init.sql` sau khi foundation merge. Nếu schema cần đổi, thêm `V3__...sql`
  (V2 được dành cho seed demo ở profile demo).
- Không merge controller trước service contract giả chỉ để compile. Nếu dependency chưa có,
  chốt interface nhỏ qua PR riêng.
- Sau mỗi merge vào `develop`, các branch đang mở rebase/merge `develop` sớm và chạy lại
  test liên quan.

## 5. Definition of Ready cho một feature

- Path/method/DTO/status/quyền đã có trong `docs/openapi.yaml`.
- Business rule và ownership đã có trong `BUSINESS_LOGIC.md`.
- Entity/migration và dependency service đã merge.
- Owner liệt kê ít nhất một happy path và một failure path cần test.

## 6. Definition of Done trước merge

- Controller mỏng; service giữ transaction và business rule; repository chỉ truy cập dữ liệu.
- Không trả entity, password, token hash hoặc exception nội bộ.
- Unit test cho logic nhánh; integration test PostgreSQL cho repository/transaction/security.
- Swagger annotation và runtime response đúng OpenAPI.
- `cd BE && ./mvnw -B verify` xanh; PR có review của ít nhất một người không phải author.

## 7. Checklist của leader

- Bảo vệ `main`/`develop`, bắt buộc Backend CI và một approval.
- Chỉ leader hoặc người được chỉ định duyệt thay đổi contract/migration/common.
- Mỗi sáng kiểm dependency blocker, không đo tiến độ bằng số file/code line.
- Mỗi cuối ngày chạy một smoke flow trên `develop`, không chờ đến ngày cuối mới tích hợp.
- Giữ LLM mock là mặc định; API thật không được làm hỏng demo khi thiếu key/mạng.
