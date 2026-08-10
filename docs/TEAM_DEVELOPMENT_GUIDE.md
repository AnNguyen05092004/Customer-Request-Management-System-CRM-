# Team Development Guide — Bzcom CRM Backend

> Tài liệu bàn giao để thành viên A/B/C/D cài môi trường, bắt đầu feature và phối hợp
> nhất quán trên nhánh `develop`. Đọc một lượt trước khi code và dùng checklist cuối file
> trước mỗi Pull Request.

## 1. Môi trường cần cài

### Bắt buộc

| Công cụ | Phiên bản dùng cho dự án | Ghi chú |
|---|---|---|
| Git | 2.40 trở lên | Cần quyền truy cập repository GitHub và đã cấu hình tên/email |
| JDK | Java 21 LTS, khuyến nghị Eclipse Temurin 21 | Kiểm tra `java` và `javac` cùng là 21 |
| Docker | Docker Desktop bản stable có Docker Compose v2 | Bắt buộc cho PostgreSQL local và Testcontainers |
| IDE | IntelliJ IDEA bản hỗ trợ Java 21, hoặc IDE tương đương | Import project dưới dạng Maven từ `BE/pom.xml` |

Dự án đã có Maven Wrapper (`BE/mvnw`, `BE/mvnw.cmd`), vì vậy **không cần cài Maven**.
Không cần cài PostgreSQL trực tiếp vì PostgreSQL 16 chạy bằng Docker.

Khuyến nghị thêm:

- GitHub CLI hoặc giao diện GitHub để tạo/review PR;
- Postman để import `docs/openapi.yaml`, hoặc dùng Swagger UI có sẵn;
- IntelliJ plugins hỗ trợ Lombok. Maven vẫn compile annotation processor độc lập với IDE.

Trước khi clone, kiểm tra:

```bash
git --version
java -version
javac -version
docker --version
docker compose version
docker info
```

Kết quả cần đạt: Java/Javac 21 và `docker info` chạy thành công. Nếu `docker info` lỗi,
hãy mở Docker Desktop trước khi chạy app hoặc test.

Cấu hình danh tính Git một lần trên máy (dùng đúng email tài khoản GitHub):

```bash
git config --global user.name "Tên của bạn"
git config --global user.email "email-cua-ban@example.com"
```

### Lưu ý theo hệ điều hành

- macOS/Linux: dùng `./mvnw` như các lệnh trong tài liệu.
- Windows PowerShell: có thể dùng `mvnw.cmd`; Git Bash/WSL có thể dùng `./mvnw`.
- Không commit cấu hình IDE (`.idea`, `.vscode`, `*.iml`) hay file `.env`.
- Không tự đổi line ending hoặc format toàn bộ repository trong một feature PR.

## 2. Nguồn chuẩn phải đọc trước khi code

Đọc [`../CLAUDE.md`](../CLAUDE.md) trước: file này là working agreement chung, quy định
traceability `task → docs/contract → code → test → PR`, ma trận đồng bộ tài liệu và cách xử
lý khi implementation hợp lý cần thay đổi thiết kế đã ghi.

Không áp dụng một thứ tự ưu tiên chung cho mọi mâu thuẫn. Dùng đúng nguồn canonical theo
phạm vi: `openapi.yaml` cho HTTP contract, `BUSINESS_LOGIC.md` cho nghiệp vụ, `ERD.md` +
Flyway cho dữ liệu, `ARCHITECTURE.md`/`BACKEND_CODING_RULES.md` cho cấu trúc code và
`TASKS.md` cho owner/dependency/DoD. Bảng đầy đủ và quy trình xử lý mâu thuẫn nằm trong
[`CLAUDE.md P2–P3`](../CLAUDE.md#p2-nguồn-chuẩn-theo-từng-loại-quyết-định).

Không tự đổi contract để code thuận tiện hơn. Nếu thực sự cần đổi, trao đổi cả team và tạo
PR cập nhật OpenAPI/docs/test trước hoặc cùng thay đổi code.

## 3. Clone và chạy project lần đầu

### 3.1 Clone mới

```bash
git clone <repository-url> Bzcom
cd Bzcom/BE
cp .env.example .env
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Kiểm tra sau khi app start:

- Health: <http://localhost:8080/actuator/health> phải trả trạng thái `UP`;
- Swagger UI: <http://localhost:8080/swagger-ui.html>;
- OpenAPI runtime: <http://localhost:8080/v3/api-docs>.

`BE/.env` được Docker Compose đọc. Khi chạy Spring bằng Maven, profile `dev` dùng các
default trong `application-dev.yml`; mẫu `.env.example` đã được giữ trùng với các default
này. Nếu đổi port/user/password DB, phải đổi đồng bộ `BE/.env` **và** truyền `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD` cho process Spring.

Để chạy cả app và DB hoàn toàn bằng Docker:

```bash
docker compose up --build
```

Dừng container mà vẫn giữ dữ liệu local:

```bash
docker compose down
```

Không chạy `docker compose down -v` trừ khi chủ động muốn xóa database local.

### Chạy full-stack bằng một lệnh

Từ thư mục gốc repository (không phải `BE/`):

```bash
cp .env.example .env       # tùy chọn
docker compose up --build
docker compose ps          # db, backend, frontend phải healthy
```

- Frontend: <http://localhost:5173>
- Backend/Swagger: <http://localhost:8080/swagger-ui.html>
- Frontend Docker dùng Nginx proxy `/api`; khi chạy `npm run dev` thì vẫn dùng CORS tới
  `localhost:8080`.

### 3.2 Xác nhận quality gate

Tại thư mục `BE/`:

```bash
./mvnw -B verify
```

Lệnh này compile, chạy unit test, integration test với Testcontainers PostgreSQL, kiểm tra
Spotless và tạo JaCoCo report. Đây cũng là lệnh CI chạy trên GitHub.

Nếu chỉ lỗi format:

```bash
./mvnw spotless:apply
./mvnw -B verify
```

Chỉ format các file thuộc thay đổi của mình; kiểm tra diff trước khi commit.

## 4. Tạo feature branch từ `develop`

### 4.1 Thiết lập `develop` lần đầu

```bash
git fetch origin
git switch --track origin/develop
```

Nếu local đã có `develop`:

```bash
git switch develop
git pull --ff-only origin develop
```

Luôn kiểm tra đang đứng đúng nhánh bằng `git status` trước khi bắt đầu code.

### 4.2 Branch và phạm vi của từng thành viên

#### Member A — Auth & Member

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/auth-member
git push -u origin feature/auth-member
```

Phạm vi sở hữu: `auth`, `member` và phần `security` liên quan. `common`, `config` là file
dùng chung; chỉ sửa khi task cần và phải báo trong PR để leader/reviewer chú ý.

> Nếu `feature/auth-member` đã được merge, không tiếp tục commit trên nhánh cũ. Mỗi thay
> đổi tiếp theo dùng branch mới từ `develop`, ví dụ `fix/auth-member-validation`.

#### Member B — Request core

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/request-core
git push -u origin feature/request-core
```

Phạm vi sở hữu: `request` entity/repository, create/list/detail, filter, pagination và
statistics. B nên làm và merge `T-2.B1` sớm để C có entity/repository thật để tích hợp.

#### Member C — Workflow logic

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/workflow-logic
git push -u origin feature/workflow-logic
```

Phạm vi sở hữu: assign, status state machine, history và optimistic concurrency. C có thể
làm state machine/history model độc lập, nhưng phải chờ `Request` entity của B được merge
trước phần persistence/transaction. Không tạo một `Request` entity thứ hai trong
`workflow` và không tự sửa entity của B mà chưa thống nhất.

#### Member D — Alert, LLM và contract audit

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/alert-llm
git push -u origin feature/alert-llm
```

Phạm vi sở hữu: `alert`, `llm`, Swagger/OpenAPI audit và hỗ trợ CI. D ưu tiên merge
`AlertService`/`T-2.D1` sớm vì create request của B và workflow của C đều phụ thuộc nó.

Nếu tên branch đã tồn tại trên remote nhưng chưa có local, dùng:

```bash
git fetch origin
git switch --track origin/<branch-name>
```

Không dùng `git switch -c` để tạo lại một branch đã tồn tại.

## 5. Cách phối hợp để giảm conflict

### 5.1 Quyền sở hữu module

| Khu vực | Owner chính | Người khác muốn sửa phải làm gì |
|---|---|---|
| `auth`, `member`, security liên quan | A | Trao đổi với A và ghi rõ trong PR |
| `request` | B | Chốt public method/entity change với B trước |
| `workflow` | C | Chốt use case contract với C trước |
| `alert`, `llm`, Swagger audit, CI | D | Trao đổi với D trước |
| `common`, `config`, `pom.xml` | Dùng chung, A/leader review | Báo team trước khi sửa |
| `openapi.yaml`, ERD, business docs | Contract chung | Cả team đồng ý; cập nhật code/test cùng PR |
| Flyway migration | Schema chung | Leader cấp version và review |

Owner không có nghĩa là người khác không được hỗ trợ; owner chịu trách nhiệm giữ boundary
và review thay đổi trong module đó.

### 5.2 Dependency giữa B, C và D

Thứ tự tích hợp thực tế:

1. B merge Request entity/repository (`T-2.B1`) bằng một PR nhỏ.
2. D merge Alert entity và public `AlertService` (`T-2.D1`) bằng một PR nhỏ.
3. Các branch đang mở cập nhật `develop`.
4. B triển khai create/list/detail; C triển khai history/assign/status.
5. D hoàn thiện Alert API/LLM và audit Swagger.

Trước khi dependency được merge, chỉ thống nhất chữ ký tối thiểu qua trao đổi/issue. Không
copy entity, repository hay tạo implementation giả ở hai branch vì sẽ gây duplicate class
và conflict khó xử lý.

Giao tiếp giữa module đi qua public service nhỏ. Ví dụ B/C gọi `AlertService`, C gọi
`MemberService.recordDeveloperCompletion(...)`; module khác không dùng repository của
owner để thay đổi business state.

### 5.3 File dùng chung và migration

- Không sửa migration đã merge, đặc biệt `V1__init.sql`.
- `V2` đang dành cho seed demo; migration schema tiếp theo bắt đầu từ `V3__...sql`.
- Trước khi tạo migration mới, báo nhóm để giữ version duy nhất, tránh hai file cùng version.
- Không tự thêm dependency vào `pom.xml`; nêu lý do và ảnh hưởng trong PR.
- Không sửa `SecurityConfig`, `ErrorCode`, response envelope hoặc OpenAPI chỉ để endpoint
  của mình compile. Nếu thiếu shared capability, tách một PR nhỏ hoặc phối hợp owner.
- Không đưa `.env`, API key, JWT/refresh token, password thật hay log chứa secret lên Git.

### 5.4 Cập nhật `develop` trong khi đang làm

Trước mỗi ngày làm việc và ngay sau khi dependency vừa merge:

```bash
git status
git switch develop
git pull --ff-only origin develop
git switch <feature-branch>
git merge develop
```

Với team nhỏ, ưu tiên `merge develop` để lịch sử dễ hiểu và không phải force-push. Nếu có
conflict:

1. Dừng lại và đọc cả hai phía; không chọn “accept all” máy móc.
2. Trao đổi owner của file dùng chung nếu ý nghĩa nghiệp vụ chưa rõ.
3. Sửa conflict, chạy test liên quan và `./mvnw -B verify`.
4. Commit merge và push branch của mình.

Không force-push nhánh người khác. Không merge trực tiếp vào `develop` hoặc `main`.

## 6. Quy tắc code thống nhất

- Package-by-feature; bên trong feature giữ chiều `Controller → Service → Repository → Entity`.
- Controller chỉ nhận/validate DTO, lấy current user, gọi service và trả response.
- Business rule và `@Transactional` đặt ở service; repository chỉ truy cập dữ liệu.
- Không trả JPA entity ra API; DTO request/response tách riêng, ưu tiên Java `record`.
- Dùng constructor injection; không field injection; entity không dùng Lombok `@Data`.
- Password dùng BCrypt; enum persist bằng string; thời gian dùng `Instant`/UTC.
- Mọi response và HTTP status phải đúng `docs/openapi.yaml`.
- Mọi lỗi dùng `BusinessException`/`ErrorCode` và `GlobalExceptionHandler`; không trả stack
  trace, SQL hay thông tin nội bộ cho client.
- Mỗi use case mới có ít nhất happy path và failure/permission test phù hợp.
- Logic thuần dùng unit test; repository/Flyway/security/transaction/concurrency dùng
  integration test với PostgreSQL Testcontainers.
- Không thêm abstraction, generic framework hoặc refactor ngoài phạm vi feature PR.

Chi tiết bắt buộc xem [`BACKEND_CODING_RULES.md`](./BACKEND_CODING_RULES.md).

## 7. Commit, push và Pull Request

Commit theo format `type: mô tả ngắn`, ví dụ:

```text
feat: add request filtering and pagination
test: cover invalid status transitions
fix: reject stale assignment version
docs: update alert API contract
```

Một commit nên chứa một thay đổi logic; không dùng message như `update`, `fix code`,
`final`. Trước khi push:

```bash
git status
git diff --check
cd BE
./mvnw -B verify
git push
```

Tạo PR từ `feature/*` vào `develop`, không vào `main`. PR phải:

- tham chiếu Task ID trong `TASKS.md`;
- mô tả thay đổi và cách test;
- nêu rõ migration/contract/shared file đã đổi;
- có ít nhất một reviewer không phải author;
- chờ **`Backend verify`**, **`Frontend verify`** và **`Full-stack Docker smoke`** xanh;
- xử lý hết review comment trước khi merge.

Sau khi merge, có thể xóa feature branch. Bản cuối chỉ được đưa từ `develop` vào `main`
bằng release PR sau khi toàn bộ flow tích hợp và CI xanh.

## 8. Checklist ngắn trước mỗi PR

- [ ] Branch được tạo từ `develop` mới nhất và chỉ chứa phạm vi task của mình.
- [ ] Không lệch `openapi.yaml`, business rules hoặc ERD.
- [ ] Không sửa nhầm module/file dùng chung của owner khác.
- [ ] Controller mỏng; transaction/business rule ở service; không trả entity.
- [ ] Có validation, phân quyền và mapping lỗi/status đúng contract.
- [ ] Có test main flow và failure/permission flow quan trọng.
- [ ] Migration append-only, version không trùng và đã test trên PostgreSQL.
- [ ] Không có secret, `.env`, token/password raw hoặc file IDE trong diff.
- [ ] `git diff --check` sạch và `cd BE && ./mvnw -B verify` xanh.
- [ ] PR vào `develop`, có reviewer và chờ cả ba CI check xanh.

## 9. Khi bị vướng

Không giữ blocker im lặng. Gửi vào group theo mẫu ngắn:

```text
Task: T-...
Đang cần: class/interface/contract nào
Blocker: lỗi hoặc quyết định chưa rõ
Đã thử: ...
Đề xuất: ...
Ảnh hưởng tới: A/B/C/D
```

Ưu tiên giải quyết dependency và contract trong ngày; không tạo workaround riêng khiến
hai module hiểu cùng một nghiệp vụ theo hai cách khác nhau.

---

Tài liệu liên quan: [`GIT_WORKFLOW.md`](./GIT_WORKFLOW.md) ·
[`../CLAUDE.md`](../CLAUDE.md) ·
[`BACKEND_CODING_RULES.md`](./BACKEND_CODING_RULES.md) · [`TASKS.md`](./TASKS.md).
