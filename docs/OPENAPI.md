# OPENAPI — Đặc tả & quy ước REST API

> Hợp đồng API (API contract) của **Bzcom CRM**. Tài liệu này giải thích **quy ước**, **danh mục endpoint**, và **ví dụ request/response**. Đặc tả máy-đọc-được đầy đủ nằm ở [`openapi.yaml`](./openapi.yaml).
>
> Phục vụ **Slide 4 — API Design Overview** & **Slide 9 — Swagger Demo**. Rubric: *"đúng RESTful, đúng HTTP status"* ↔ *"tất cả dùng POST, verb trong URL"*.

---

## Mục lục
1. [Cách xem & dùng đặc tả](#1-cách-xem--dùng-đặc-tả)
2. [Nguyên tắc RESTful áp dụng](#2-nguyên-tắc-restful-áp-dụng)
3. [Quy ước chung](#3-quy-ước-chung)
4. [Response envelope & phân trang](#4-response-envelope--phân-trang)
5. [Bảng mã HTTP status](#5-bảng-mã-http-status)
6. [Danh mục endpoint](#6-danh-mục-endpoint)
7. [Ví dụ request/response từng endpoint](#7-ví-dụ-requestresponse-từng-endpoint)
8. [Ma trận phân quyền](#8-ma-trận-phân-quyền)

---

## 1. Cách xem & dùng đặc tả

- **Swagger UI (khi app chạy):** http://localhost:8080/swagger-ui.html — springdoc tự sinh từ annotation trong code. Đây là phần demo API ở slide 9.
- **File tĩnh [`openapi.yaml`](./openapi.yaml):** có thể:
  - Dán vào https://editor.swagger.io để xem/validate.
  - Import vào **Postman** (`File → Import`) → có ngay collection để test.
  - Dùng làm **contract** ở Phase 0 để cả nhóm code khớp nhau (frontend/tester không phải chờ backend).

> **Nguồn chuẩn:** `openapi.yaml` là API contract canonical. Controller/DTO/test phải bám file này; springdoc `/v3/api-docs` là runtime snapshot để audit chứ không phải contract thứ hai được tự ý sửa. CI có một contract test tối thiểu: parse YAML, so danh sách `path + method + response status` với runtime snapshot và fail khi lệch. Mọi breaking change phải sửa YAML, docs, FE types và test trong cùng PR.

## 2. Nguyên tắc RESTful áp dụng

| Nguyên tắc | Áp dụng trong dự án | Phản ví dụ (bị trừ điểm) |
|---|---|---|
| **Danh từ cho resource, không dùng verb trong URL** | `/api/requests`, `/api/alerts` | ❌ `/api/createRequest` |
| **HTTP method thể hiện hành động** | `POST` tạo, `GET` đọc, `PATCH` sửa một phần | ❌ mọi thứ dùng `POST` |
| **PATCH cho cập nhật một phần** | `/requests/{id}/status`, `/{id}/assign` | ❌ `PUT` full object khi chỉ đổi 1 field |
| **Sub-resource cho quan hệ** | `/requests/{id}/history`, `/{id}/sources` | ❌ `/getHistoryByRequestId?id=` |
| **HTTP status đúng ngữ nghĩa** | 201 khi tạo, 409 khi transition sai | ❌ luôn trả 200 kèm `success:false` |
| **Query param cho filter/sort/paging** | `?page&size&sort&status&keyword` | ❌ nhét filter vào path |
| **Stateless** | JWT trong header mỗi request, không session | ❌ lưu login state ở server |

**Về hành động không "CRUD thuần" (assign, status, read):** dùng `PATCH /resource/{id}/{action}` — đây là pattern REST được chấp nhận rộng rãi cho *state transition* trên một resource (còn gọi là "controller sub-resource"). Nói được lý do này khi bị hỏi = ghi điểm.

## 3. Quy ước chung

- **Base URL:** `/api`
- **Content-Type:** `application/json`
- **Auth header:** `Authorization: Bearer <accessToken>` (mọi endpoint trừ `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `POST /members`). `refresh`/`logout` xác thực bằng refresh token trong body và **không gửi Authorization header**; chúng không phải anonymous business API.
- **Naming:** path dạng `kebab`/`snake` số ít-số nhiều nhất quán (dùng số nhiều cho collection: `requests`, `members`, `alerts`); field JSON dạng `camelCase`.
- **Thời gian:** ISO-8601 (`2026-07-02T10:15:30`).
- **Enum:** viết HOA (`BUG`, `HIGH`, `PENDING`).

## 4. Response envelope & phân trang

**Mọi response** (thành công lẫn lỗi) đều bọc trong envelope thống nhất (NFR-01):
```json
{ "status": 200, "message": "success", "data": { } }
```

**Thành công tạo mới:**
```json
{ "status": 201, "message": "created", "data": { "id": 10, "status": "PENDING" } }
```

**Lỗi:** `data = null`, `message` mô tả lỗi:
```json
{ "status": 409, "message": "Invalid status transition: DONE -> IN_PROGRESS", "data": null }
```

**Phân trang** (cho `GET /requests`): `data` chứa cả nội dung lẫn metadata trang:
```json
{
  "status": 200, "message": "success",
  "data": {
    "content": [ { "id": 1, "title": "..." } ],
    "page": 0, "size": 10, "totalElements": 42, "totalPages": 5
  }
}
```

## 5. Bảng mã HTTP status

> Đây là **checklist** khi implement từng endpoint — bám đúng để không rơi vào "Hire: RECONSIDER".

| Tình huống | Status | Ví dụ |
|---|---|---|
| Lấy dữ liệu thành công | **200** | GET /requests |
| Tạo mới thành công | **201** | POST /requests, POST /members |
| Validation sai (thiếu field, sai enum) | **400** | title rỗng |
| Chưa đăng nhập / token sai/hết hạn | **401** | không có header Authorization |
| Đã đăng nhập nhưng sai quyền (role) | **403** | CLIENT gọi /requests/stats |
| Không tìm thấy resource | **404** | GET /requests/999 |
| Xung đột: email trùng / request chưa gán / transition sai / optimistic lock | **409** | DONE→IN_PROGRESS |
| Không xử lý được về mặt nghiệp vụ (vd auto-assign khi không có dev) | **422** | assign auto khi 0 developer |
| Lỗi server không lường trước | **500** | (global handler bọc, không lộ stacktrace) |

> Phân biệt **401 vs 403**: 401 = "bạn là ai?" (chưa xác thực); 403 = "biết bạn là ai rồi, nhưng không được phép".

## 6. Danh mục endpoint

### Auth
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/auth/login` | public | Đăng nhập, cấp JWT |
| POST | `/api/auth/refresh` | refresh token | Cấp access token mới và refresh-token rotation |
| POST | `/api/auth/logout` | refresh token | Thu hồi refresh token của phiên hiện tại |

### Member
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/members` | public | Đăng ký **CLIENT** → 201; server không nhận `role`, field lạ bị 400 |
| GET | `/api/members` | ADMIN | Danh sách member |
| GET | `/api/members/{id}` | ADMIN hoặc chính member đó | Chi tiết member |

### Request
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/requests` | CLIENT | Tạo yêu cầu → 201 |
| GET | `/api/requests` | all (lọc theo role) | Danh sách: paging/sort/filter |
| GET | `/api/requests/{id}` | all (kiểm quyền) | Chi tiết |
| PATCH | `/api/requests/{id}/assign` | ADMIN | Gán developer (auto/thủ công) |
| PATCH | `/api/requests/{id}/status` | ADMIN/DEVELOPER | Đổi trạng thái |
| GET | `/api/requests/{id}/history` | all (kiểm quyền như request detail) | Lịch sử |
| GET | `/api/requests/stats` | ADMIN | Thống kê |

### Alert
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/alerts` | authenticated | Alert của bản thân |
| PATCH | `/api/alerts/{id}/read` | owner | Đánh dấu đã đọc |

### LLM (đều nằm trong contract — xem [LLM.md](./LLM.md))
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/requests/classify` | Phân loại category từ description |
| POST | `/api/requests/suggest-priority` | Gợi ý priority từ description |
| GET | `/api/requests/{id}/summary` | Tóm tắt 1-2 dòng |

## 7. Ví dụ request/response từng endpoint

### POST /api/auth/login
```jsonc
// Request
{ "email": "admin@bzcom.com", "password": "1234" }
// Response 200
{ "status": 200, "message": "success",
  "data": { "accessToken": "eyJ...", "refreshToken": "eyJ...",
            "tokenType": "Bearer", "role": "ADMIN" } }
```

### POST /api/auth/refresh và POST /api/auth/logout
```jsonc
// Request body dùng cho cả hai endpoint (refresh token là opaque credential)
{ "refreshToken": "<refresh-token>" }

// Validation: 32–512 ký tự; field lạ bị reject với 400.

// POST /api/auth/refresh → 200: trả accessToken và refreshToken MỚI.
// Token refresh cũ bị revoke nguyên tử; cùng một token chỉ có một refresh thành công.
// POST /api/auth/logout → 200: refresh token bị revoke; access token còn tối đa 15 phút rồi tự hết hạn.
```

### POST /api/members
```jsonc
// Request — endpoint công khai chỉ tạo CLIENT; không có field role.
// Gửi role hoặc field lạ → 400 (fail-closed).
{ "email": "client2@bzcom.com", "password": "1234", "name": "Client Two" }
// Response 201  (KHÔNG bao giờ trả password)
{ "status": 201, "message": "created",
  "data": { "id": 5, "email": "dev1@bzcom.com", "name": "Dev One", "role": "DEVELOPER" } }
```

### POST /api/requests  (HIGH → tự sinh alert cho ADMIN)
```jsonc
// Request (role CLIENT)
{ "title": "Login fails", "description": "Click login shows 500 error",
  "category": "BUG", "priority": "HIGH" }
// Response 201
{ "status": 201, "message": "created",
  "data": { "id": 10, "status": "PENDING", "category": "BUG", "priority": "HIGH",
            "clientId": 4, "assignedDeveloperId": null } }
```

### GET /api/requests?page=0&size=10&sort=createdAt,desc&status=PENDING&category=BUG&keyword=login
```jsonc
// Response 200
{ "status": 200, "message": "success",
  "data": { "content": [ { "id": 10, "title": "Login fails", "status": "PENDING" } ],
            "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 } }
```

### PATCH /api/requests/{id}/assign
```jsonc
// Auto-assign
{ "auto": true, "expectedVersion": 0 }
// Thủ công
{ "auto": false, "developerId": 5, "expectedVersion": 0 }
// Response 200
{ "status": 200, "message": "assigned",
  "data": { "id": 10, "assignedDeveloperId": 5, "status": "PENDING" } }
```

### PATCH /api/requests/{id}/status
```jsonc
// Request hợp lệ
{ "status": "IN_PROGRESS", "memo": "start working", "expectedVersion": 1 }
// Request phải đã được gán developer; chưa gán → 409.
// Response 200 ... 
// Request sai luật (DONE → IN_PROGRESS)
// Response 409
{ "status": 409, "message": "Invalid status transition: DONE -> IN_PROGRESS", "data": null }
```

### GET /api/requests/{id}/history
```jsonc
{ "status": 200, "message": "success",
  "data": [
    { "id": 1, "fromStatus": null, "toStatus": null, "memo": "auto-assigned to Dev One", "changedBy": 1, "changedAt": "..." },
    { "id": 2, "fromStatus": "PENDING", "toStatus": "IN_PROGRESS", "memo": "start working", "changedBy": 5, "changedAt": "..." }
  ] }
```

### GET /api/requests/stats
```jsonc
{ "status": 200, "message": "success",
  "data": {
    "total": 42, "completed": 18, "completionRate": 0.43,
    "byCategory": { "BUG": 20, "FEATURE": 15, "INQUIRY": 7 },
    "byDeveloper": [ { "developerId": 5, "developerName": "Dev One", "assignedCount": 8, "doneCount": 3 } ]
  } }
```

### GET /api/alerts
```jsonc
{ "status": 200, "message": "success",
  "data": [ { "id": 3, "requestId": 10, "alertType": "HIGH_PRIORITY_REGISTERED",
              "message": "New HIGH request: Login fails", "isRead": false, "createdAt": "..." } ] }
```

### POST /api/requests/classify  (LLM)
```jsonc
// Request
{ "description": "When I click login, page shows 500 error" }
// Response 200
{ "status": 200, "message": "success",
  "data": { "category": "BUG", "confidence": 0.94, "reason": "mentions error on action" } }
```

## 8. Ma trận phân quyền

> Chi tiết logic kiểm quyền (ownership check trong service) ở [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md#1-authentication--authorization).

| Endpoint | ADMIN | DEVELOPER | CLIENT | public |
|---|:---:|:---:|:---:|:---:|
| POST /auth/login | | | | ✅ |
| POST /members | | | | ✅ |
| GET /members | ✅ | | | |
| GET /members/{id} | ✅ | chỉ chính mình | chỉ chính mình | |
| POST /requests | | | ✅ | |
| GET /requests | ✅ (all) | ✅ (được gán) | ✅ (của mình) | |
| GET /requests/{id} | ✅ | ✅ (nếu được gán) | ✅ (nếu là chủ) | |
| PATCH /requests/{id}/assign | ✅ | | | |
| PATCH /requests/{id}/status | ✅ (request phải đã gán) | ✅ (được gán) | | |
| GET /requests/{id}/history | ✅ | ✅ (nếu được gán) | ✅ (nếu là chủ) | |
| GET /requests/stats | ✅ | | | |
| GET /alerts | ✅ | ✅ | ✅ (của mình) | |
| PATCH /alerts/{id}/read | owner | owner | owner | |
| POST /requests/classify, /suggest-priority | ✅ | ✅ | ✅ | |
| GET /requests/{id}/summary | ✅ | ✅ (nếu được gán) | ✅ (nếu là chủ) | |

---

*Đặc tả máy đọc: [`openapi.yaml`](./openapi.yaml). Logic đằng sau các endpoint: [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md).*
