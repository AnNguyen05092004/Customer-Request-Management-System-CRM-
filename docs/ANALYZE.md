# ANALYZE — Phân tích yêu cầu

> Tài liệu phân tích nghiệp vụ & yêu cầu cho **Bzcom CRM**. Mục tiêu: biến đề bài thành các yêu cầu **kiểm chứng được** (verifiable), map trực tiếp vào rubric chấm điểm.
>
> Phục vụ **Slide 2 — Problem Definition** và là cơ sở cho toàn bộ thiết kế.

---

## Mục lục
1. [Bối cảnh & phát biểu vấn đề](#1-bối-cảnh--phát-biểu-vấn-đề)
2. [Mục tiêu & phạm vi](#2-mục-tiêu--phạm-vi)
3. [Actor & phân quyền](#3-actor--phân-quyền)
4. [Từ điển miền (Domain glossary)](#4-từ-điển-miền-domain-glossary)
5. [Yêu cầu chức năng (FR)](#5-yêu-cầu-chức-năng-fr)
6. [Yêu cầu phi chức năng (NFR)](#6-yêu-cầu-phi-chức-năng-nfr)
7. [Business Rules (BR)](#7-business-rules-br)
8. [Use case chi tiết](#8-use-case-chi-tiết)
9. [Acceptance Criteria & truy vết rubric](#9-acceptance-criteria--truy-vết-rubric)
10. [Giả định, ràng buộc, out-of-scope](#10-giả-định-ràng-buộc-out-of-scope)

---

## 1. Bối cảnh & phát biểu vấn đề

**Bối cảnh.** Bzcom vận hành và bảo trì web service cho 3 khách hàng Hàn Quốc. Yêu cầu (báo lỗi, đề nghị tính năng, thắc mắc) đến qua **email + KakaoTalk + điện thoại** — phân tán, không có nơi lưu tập trung.

**Điểm đau (pain points):**

| # | Vấn đề thực tế | Hệ quả | Hệ thống giải quyết bằng |
|---|---|---|---|
| P1 | Yêu cầu rải rác nhiều kênh | Khó tổng hợp, dễ trùng | Một nơi tập trung: bảng `requests` |
| P2 | Đổi nhân sự → mất lịch sử | Không biết ai đã làm gì | `request_histories` tự ghi mọi thay đổi |
| P3 | Yêu cầu bị bỏ sót | Khách phàn nàn, mất uy tín | Auto-assign + alert tự động |
| P4 | Không rõ ai chịu trách nhiệm | Đùn đẩy, chậm trễ | `assigned_developer_id` + phân quyền role |
| P5 | Không đo được hiệu suất | Không cải tiến được | API thống kê (tổng, completion rate, theo dev) |

**Phát biểu vấn đề (1 câu để đưa lên slide):**
> *"Bzcom cần một hệ thống backend tập trung hóa yêu cầu khách hàng, ràng buộc quy trình xử lý bằng luật, và tự động hóa việc gán — ghi vết — thông báo, để không còn yêu cầu nào bị thất lạc khi nhân sự thay đổi."*

## 2. Mục tiêu & phạm vi

**Mục tiêu sản phẩm (product goals):**
- G1 — Mọi yêu cầu được lưu vết đầy đủ vòng đời (ai tạo, ai xử lý, trạng thái đi qua những đâu).
- G2 — Đúng người thấy đúng dữ liệu (ADMIN/DEVELOPER/CLIENT).
- G3 — Giảm thao tác thủ công: gán việc, ghi lịch sử, gửi thông báo đều tự động.
- G4 — API chuẩn REST, tài liệu Swagger đầy đủ, chạy được bằng một lệnh.

**Trong phạm vi (in-scope):** Auth (JWT), quản lý member, CRUD + filter request, auto-assign, state machine trạng thái, history, alert, statistics, 1 tính năng LLM, Swagger, Docker, CI.

**Ngoài phạm vi (out-of-scope):** xem [§10](#10-giả-định-ràng-buộc-out-of-scope).

## 3. Actor & phân quyền

| Actor | Là ai | Quyền cốt lõi |
|---|---|---|
| **ADMIN** | Quản trị/PM của Bzcom | Toàn quyền: xem mọi request, gán developer, xem thống kê, quản lý member |
| **DEVELOPER** | Lập trình viên xử lý yêu cầu | Chỉ thấy & xử lý request **được gán cho mình**; đổi trạng thái |
| **CLIENT** | Khách hàng Hàn Quốc | Tạo request; chỉ xem request **mình tạo**; nhận thông báo đổi trạng thái |
| **System** (actor ẩn) | Chính hệ thống | Tự gán, tự ghi history, tự sinh alert (kích hoạt bởi hành động của 3 actor trên) |

**Ma trận quyền (tóm tắt — chi tiết ở [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md#1-authentication--authorization)):**

| Hành động | ADMIN | DEVELOPER | CLIENT |
|---|:---:|:---:|:---:|
| Tạo request | — | — | ✅ |
| Xem tất cả request | ✅ | chỉ của mình | chỉ của mình |
| Gán developer | ✅ | — | — |
| Đổi trạng thái | ✅ | request được gán | — |
| Xem thống kê | ✅ | — | — |
| Xem danh sách member | ✅ | — | — |

## 4. Từ điển miền (Domain glossary)

| Thuật ngữ | Định nghĩa |
|---|---|
| **Request** | Một yêu cầu của khách: BUG / FEATURE / INQUIRY |
| **Category** | Loại yêu cầu: `BUG`, `FEATURE`, `INQUIRY` |
| **Priority** | Mức ưu tiên: `HIGH`, `MEDIUM`, `LOW` |
| **Status** | Trạng thái vòng đời: `PENDING → IN_PROGRESS → DONE` |
| **Assignment** | Việc gán một request cho một DEVELOPER |
| **currentTaskCount** | Số request đang gán cho 1 dev **và chưa DONE** — dùng cho auto-assign; **tính động, không lưu cột** |
| **History** | Bản ghi bất biến mỗi lần đổi status/gán dev |
| **Alert** | Thông báo tự sinh gửi tới 1 member |
| **State machine** | Tập luật quy định trạng thái nào được chuyển sang trạng thái nào |

## 5. Yêu cầu chức năng (FR)

> Mỗi FR có **tiêu chí chấp nhận** ở [§9](#9-acceptance-criteria--truy-vết-rubric).

| ID | Nhóm | Mô tả | Ưu tiên |
|---|---|---|---|
| FR-01 | Auth | Đăng nhập cấp JWT access token + opaque refresh token | Must |
| FR-02 | Auth | Refresh token rotation và logout (thu hồi refresh token) | Must |
| FR-03 | Auth | Xác thực token trên mọi request được bảo vệ | Must |
| FR-04 | Auth | Phân quyền theo role (ADMIN/DEVELOPER/CLIENT) | Must |
| FR-05 | Member | CLIENT tự đăng ký (BCrypt; server ép role `CLIENT`) | Must |
| FR-06 | Member | ADMIN lấy danh sách member; lấy chi tiết 1 member | Must |
| FR-07 | Request | CLIENT tạo yêu cầu mới (mặc định status = PENDING) | Must |
| FR-08 | Request | Lấy danh sách: **phân trang + sắp xếp + filter tổ hợp**, giới hạn theo role | Must |
| FR-09 | Request | Lấy chi tiết 1 yêu cầu (kiểm quyền xem) | Must |
| FR-10 | Request | ADMIN gán developer (thủ công **hoặc** auto-assign) | Must |
| FR-11 | Request | Cập nhật trạng thái, **chặn transition không hợp lệ** | Must |
| FR-12 | Request | Lấy lịch sử thay đổi trạng thái của 1 request | Must |
| FR-13 | Request | Thống kê: tổng, completion rate, theo category, theo developer | Must |
| FR-14 | Alert | Lấy danh sách alert của bản thân; đánh dấu đã đọc | Must |
| FR-15 | Alert | **Tự sinh alert**: HIGH→all ADMIN; assigned→developer; status changed→client | Must |
| FR-16 | History | **Tự ghi history** khi đổi status hoặc gán developer | Must |
| FR-17 | LLM | ≥1 tính năng: auto-classify / suggest-priority / auto-summary | Must |

## 6. Yêu cầu phi chức năng (NFR)

| ID | Loại | Mô tả | Đo lường/kiểm chứng |
|---|---|---|---|
| NFR-01 | API | Response format thống nhất `{status,message,data}` | Mọi endpoint trả đúng envelope |
| NFR-02 | Docs | Swagger/OpenAPI cho **mọi** endpoint | Swagger UI liệt kê đủ endpoint |
| NFR-03 | API | HTTP status đúng chuẩn (200/201/400/401/403/404/409/500) | Bảng test status ở OPENAPI.md |
| NFR-04 | Security | Password lưu BCrypt, không bao giờ trả về client | DB không có plaintext; response không có field password |
| NFR-05 | Reliability | Exception xử lý tập trung (global handler) | Không lộ stacktrace, luôn đúng envelope |
| NFR-06 | Data | Validate input trước khi lưu (`@Valid`) | Input sai → 400 với message rõ |
| NFR-07 | Concurrency | Chống race condition khi cập nhật đồng thời (optimistic lock) | 2 update song song → 1 nhận 409 |
| NFR-08 | Deploy | Chạy bằng một lệnh (`docker compose up`) | Máy trắng chạy được |
| NFR-09 | Demo | Seed data cố định + reset được | Demo lặp lại cho kết quả nhất quán |
| NFR-10 | Quality | Có unit/integration test cho luồng cốt lõi | CI xanh trên mỗi PR |
| NFR-11 | Database fidelity | Integration test chạy PostgreSQL thật qua Testcontainers | Flyway và truy vấn chạy cùng dialect production; không dùng H2 |

## 7. Business Rules (BR)

> Đây là các **luật nghiệp vụ bất biến** — vi phạm phải bị chặn. Implement & giải thích chi tiết ở [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md).

| ID | Luật | Vi phạm → |
|---|---|---|
| BR-01 | Chỉ CLIENT được tạo request | 403 |
| BR-02 | DEVELOPER chỉ truy cập request được gán cho mình | 403 |
| BR-03 | CLIENT chỉ truy cập request mình tạo | 403 |
| BR-04 | Chỉ ADMIN được gán developer | 403 |
| BR-05 | Trạng thái chỉ đi theo `PENDING → IN_PROGRESS → DONE`; request phải được gán DEVELOPER trước khi vào `IN_PROGRESS` | 409 |
| BR-06 | Cấm lùi trạng thái (`DONE → *`, `IN_PROGRESS → PENDING`) | 409 |
| BR-07 | Cấm nhảy cóc (`PENDING → DONE`) | 409 |
| BR-08 | Auto-assign chọn DEVELOPER có `currentTaskCount` nhỏ nhất; hoà → người **hoàn thành gần nhất** | — |
| BR-09 | Mọi lần đổi status/gán dev **phải** tạo 1 bản ghi history (cùng transaction) | rollback nếu lỗi |
| BR-10 | Tạo request HIGH → alert **tất cả** ADMIN | — |
| BR-11 | Gán dev → alert đúng developer đó | — |
| BR-12 | Đổi status → alert CLIENT tạo request | — |
| BR-13 | LLM chỉ **gợi ý**; quyết định cuối do người/logic xác nhận | — |
| BR-14 | Mọi PATCH request phải gửi `expectedVersion`; version lệch hoặc JPA optimistic lock → 409 | 409 |
| BR-15 | Refresh token là opaque credential, DB chỉ lưu SHA-256 hash; refresh luôn rotation bằng revoke nguyên tử (một token chỉ refresh thành công một lần), logout revoke token | 401 / 200 idempotent |
| BR-17 | Public registration luôn tạo CLIENT; `role` không thuộc DTO công khai và field lạ bị reject (fail-closed) | 400 |
| BR-16 | History và summary kế thừa quyền đọc của request cha | 403 |

## 8. Use case chi tiết

### UC-01 — CLIENT tạo yêu cầu ưu tiên cao
- **Actor chính:** CLIENT
- **Tiền điều kiện:** đã login, có JWT hợp lệ, role = CLIENT
- **Luồng chính:**
1. CLIENT gửi `POST /api/requests` với title, description, category, priority=HIGH.
  2. Hệ thống validate (BR: title/category/priority bắt buộc) → lưu request, status = PENDING.
  3. Vì priority = HIGH → **tự sinh alert** `HIGH_PRIORITY_REGISTERED` cho **mọi ADMIN** (BR-10).
  4. Trả `201 Created` + request vừa tạo.
- **Luồng phụ / lỗi:** thiếu field → `400`; role ≠ CLIENT → `403` (BR-01).
- **Hậu điều kiện:** request tồn tại (PENDING), các ADMIN có alert mới.

### UC-02 — ADMIN auto-assign developer
- **Actor chính:** ADMIN · **Kích hoạt:** `PATCH /api/requests/{id}/assign` body `{"auto":true}`
- **Luồng chính:**
1. Hệ thống khóa tập developer theo thứ tự cố định trong transaction, rồi tính `currentTaskCount` động cho từng người (BR-08). Việc này tránh hai auto-assign đồng thời chọn cùng một dev vì cùng snapshot tải.
  2. Chọn người count nhỏ nhất; nếu hoà → người `last_completed_at` mới nhất.
  3. Gán `assigned_developer_id`; **ghi history** (BR-09); **sinh alert** ASSIGNED cho dev đó (BR-11).
  4. Trả `200` + request đã gán.
- **Lỗi:** không có DEVELOPER nào → `409`/`422` với message rõ; role ≠ ADMIN → `403` (BR-04).

### UC-03 — DEVELOPER chuyển trạng thái công việc
- **Actor chính:** DEVELOPER · **Kích hoạt:** `PATCH /api/requests/{id}/status`
- **Luồng chính:**
  1. Kiểm quyền: request này có `assigned_developer_id == me`? Không → `403` (BR-02).
2. So `expectedVersion` với version hiện tại; request phải đã được gán developer; rồi kiểm transition hợp lệ qua state machine (BR-05..07). Bất kỳ lệch version/chưa gán/transition sai → `409`.
3. Cập nhật status (double guard: `expectedVersion` + JPA `@Version`, NFR-07); **ghi history** (BR-09); **alert** CLIENT (BR-12).
  4. Trả `200`.
- **Lỗi đồng thời:** 2 người đổi cùng lúc → người thứ 2 nhận `409` (OptimisticLock).

### UC-04 — Xem danh sách request theo vai trò
- **Actor:** ADMIN / DEVELOPER / CLIENT · `GET /api/requests?page&size&sort&status&category&priority&keyword`
- **Luồng:** hệ thống áp filter phạm vi theo role (BR-02/03) rồi mới áp filter tổ hợp + phân trang + sort. Trả `200` + `Page`.

### UC-05 — LLM auto-classify
- **Actor:** ADMIN/CLIENT · `POST /api/requests/classify` body `{description}`
- **Luồng:** gọi `LlmService.classify(description)` → JSON `{category, confidence, reason}`. Nếu LLM lỗi/không parse được → fallback rule keyword (BR-13, xem [LLM.md](./LLM.md)). Trả `200`.

## 9. Acceptance Criteria & truy vết rubric

> Bảng này là **hợp đồng "định nghĩa hoàn thành" (Definition of Done)** — mỗi tiêu chí rubric map tới FR/BR/NFR cụ thể và cách chứng minh khi demo.

| Tiêu chí rubric | "Hire: YES" đòi hỏi | Map tới | Cách chứng minh khi demo |
|---|---|---|---|
| ERD Design | Quan hệ chuẩn hoá, giải thích được | [ERD.md](./ERD.md) | Slide 3: chỉ 2 FK tới members, taskCount động, version |
| REST API | Đúng RESTful, đúng HTTP status | FR-07..14, NFR-03 | Swagger UI: method đúng, status đúng bảng OPENAPI |
| JWT Auth | Token flow + phân quyền role | FR-01..04, BR-01..04 | Login 3 role, thử truy cập chéo → 403 |
| Business Logic | Auto-assign + transition + history đúng | FR-10,11,16 + BR-05..09 | Demo transition sai → 409; history tự ghi |
| LLM Prompt | Có purpose/context/output format | FR-17, [LLM.md](./LLM.md) | Slide 8: show prompt có cấu trúc + output JSON |
| Git Flow | Branch/PR/commit thực thi thật | [GIT_WORKFLOW.md](./GIT_WORKFLOW.md) | Slide 10: lịch sử PR + CI xanh |
| PPT | Giải thích intent & trade-off | toàn bộ docs | Mỗi slide 1 câu "chọn X vì Y, đánh đổi Z" |

**Definition of Done cho một FR bất kỳ:** (1) code chạy đúng luồng chính + luồng lỗi; (2) có Swagger; (3) trả đúng envelope + status; (4) có ≥1 test cho luồng chính; (5) merge qua PR có review.

## 10. Giả định, ràng buộc, out-of-scope

**Giả định (assumptions):**
- A1 — Số lượng dữ liệu nhỏ (đây là demo) → không cần tối ưu hiệu năng quy mô lớn.
- A2 — Không cần multi-tenant thật sự cho 3 khách hàng; phân quyền theo member là đủ cho demo.
- A3 — PostgreSQL 16 là nguồn dữ liệu chuẩn ở dev/demo/integration test; Testcontainers khởi tạo DB cô lập cho integration test.

**Ràng buộc (constraints):**
- C1 — Ngôn ngữ Java hoặc Python (chọn Java + Spring Boot).
- C2 — Bắt buộc Swagger cho mọi endpoint; response envelope cố định.
- C3 — Tổng thời lượng nhóm ~8h → ưu tiên "làm đúng luồng cốt lõi + giải thích được" hơn là nhồi tính năng.

**Out-of-scope (nói rõ trong slide để tránh bị hỏi lạc đề):**
- Frontend React là **lớp trình diễn bổ sung** (không nằm trong rubric), làm sau khi backend ổn — xem [FRONTEND.md](./FRONTEND.md). Swagger UI vẫn là phần demo API chính thức.
- Không microservices thật (dùng modular monolith — xem [ARCHITECTURE.md](./ARCHITECTURE.md#2-phong-c%C3%A1ch-ki%E1%BA%BFn-tr%C3%BAc-modular-monolith)).
- Không có UI quản trị để tạo ADMIN/DEVELOPER trong MVP: các tài khoản nội bộ được provision qua seed/migration; endpoint public chỉ tạo CLIENT để tránh privilege escalation.
- Không tích hợp email/Kakao thật (alert lưu trong DB, đọc qua API).
- Không có payment, SLA, audit nâng cao.

---

*Tài liệu phân tích này là nguồn sự thật cho "hệ thống cần làm gì". "Làm như thế nào" xem ARCHITECTURE.md & BUSINESS_LOGIC.md.*
