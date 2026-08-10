# Tài liệu Đặc tả Kỹ thuật — Phân hệ Request Core & Workflow API

> **Dự án:** Bzcom CRM — Customer Request Management System  
> **Phiên bản:** 1.0  
> **Ngôn ngữ:** Tiếng Việt  
> **Phạm vi:** Phân hệ Quản lý Yêu cầu Khách hàng (Request Core: Tạo, Tra cứu, Lọc, Phân trang) & Quy trình Xử lý (Request Workflow: Gán Developer, Đổi trạng thái, Lịch sử).

---

## 1. Tổng quan phân hệ

Phân hệ Request là trái tim nghiệp vụ của ứng dụng Bzcom CRM, hỗ trợ nhận yêu cầu từ khách hàng (`CLIENT`), quản lý phân công làm việc cho lập trình viên (`DEVELOPER`) bởi quản trị viên (`ADMIN`), và tự động hóa quy trình theo dõi trạng thái, ghi nhận lịch sử và cảnh báo.

### Ma trận phân quyền theo vai trò (Role Matrix)

| Chức năng | API Endpoint | ADMIN | CLIENT | DEVELOPER |
|---|---|:---:|:---:|:---:|
| Tạo yêu cầu | `POST /api/requests` | ✗ | ✅ (chính mình) | ✗ |
| Xem danh sách | `GET /api/requests` | ✅ (Tất cả) | ✅ (Request sở hữu) | ✅ (Request được gán) |
| Xem chi tiết | `GET /api/requests/{id}` | ✅ | ✅ (Request sở hữu) | ✅ (Request được gán) |
| Gán Developer | `PATCH /api/requests/{id}/assign` | ✅ | ✗ | ✗ |
| Cập nhật trạng thái | `PATCH /api/requests/{id}/status` | ✅ | ✗ | ✅ (Request được gán) |
| Xem lịch sử | `GET /api/requests/{id}/history` | ✅ | ✅ (Request sở hữu) | ✅ (Request được gán) |

---

## 2. Mô hình Dữ liệu (Data Model)

### 2.1 Cấu trúc Enum

1. **`RequestCategory`**: Phân loại yêu cầu
   - `BUG`: Báo lỗi hệ thống
   - `FEATURE`: Yêu cầu tính năng mới
   - `INQUIRY`: Hỏi đáp / Hỗ trợ thông tin

2. **`RequestPriority`**: Mức độ ưu tiên
   - `HIGH`: Cao (kích hoạt cảnh báo tức thì cho ADMIN khi khởi tạo)
   - `MEDIUM`: Trung bình
   - `LOW`: Thấp

3. **`RequestStatus`**: Trạng thái vòng đời
   - `PENDING`: Mới tạo, chờ xử lý / chờ gán
   - `IN_PROGRESS`: Đang xử lý
   - `DONE`: Hoàn tất

### 2.2 Bảng `requests` (Bảng chính)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `BIGINT` | Primary Key, Auto Increment | Định danh request |
| `title` | `VARCHAR(200)` | `NOT NULL` | Tiêu đề yêu cầu (1–200 ký tự) |
| `description` | `TEXT` | `NULLABLE` | Mô tả chi tiết (tối đa 4000 ký tự) |
| `category` | `VARCHAR(20)` | `NOT NULL` | Enum: `BUG`, `FEATURE`, `INQUIRY` |
| `priority` | `VARCHAR(20)` | `NOT NULL` | Enum: `HIGH`, `MEDIUM`, `LOW` |
| `status` | `VARCHAR(20)` | `NOT NULL`, Default `'PENDING'` | Enum: `PENDING`, `IN_PROGRESS`, `DONE` |
| `client_id` | `BIGINT` | FK -> `members.id`, `NOT NULL` | ID người tạo (`CLIENT`) |
| `assigned_developer_id` | `BIGINT` | FK -> `members.id`, `NULLABLE` | ID developer được phân công |
| `version` | `INT` | `NOT NULL`, Default `0` | Khóa lạc quan (Optimistic Locking `@Version`) |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Thời điểm tạo (UTC Instant) |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | Thời điểm cập nhật gần nhất (UTC Instant) |

### 2.3 Bảng `request_histories` (Lịch sử thay đổi)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `BIGINT` | Primary Key, Auto Increment | Định danh bản ghi lịch sử |
| `request_id` | `BIGINT` | FK -> `requests.id`, `NOT NULL` | ID request liên quan |
| `changed_by` | `BIGINT` | FK -> `members.id`, `NOT NULL` | ID người thực hiện thay đổi |
| `from_status` | `VARCHAR(20)` | `NULLABLE` | Trạng thái trước (null khi thao tác là "gán") |
| `to_status` | `VARCHAR(20)` | `NULLABLE` | Trạng thái sau (null khi thao tác là "gán") |
| `memo` | `VARCHAR(255)` | `NULLABLE` | Ghi chú thao tác (VD: "auto-assigned to Dev One") |
| `changed_at` | `TIMESTAMPTZ` | `NOT NULL` | Thời điểm ghi nhận (UTC Instant) |

---

## 3. Quy tắc Nghiệp vụ cốt lõi (Business Rules)

### 3.1 State Machine Trạng thái (`RequestStatus`)

Sơ đồ chuyển trạng thái tuân thủ nghiêm ngặt theo bảng sau:

```
    ┌─────────┐   cho phép    ┌───────────────┐   cho phép    ┌──────┐
    │ PENDING │──────────────▶│  IN_PROGRESS  │──────────────▶│ DONE │
    └─────────┘               └───────────────┘               └──────┘
         │                            │                           │
         │ ✗ Chặn nhảy vọt (409)      │ ✗ Chặn lùi bước (409)     │ ✗ Trạng thái cuối (409)
         ▼                            ▼                           ▼
      BLOCKED                      BLOCKED                     BLOCKED
```

- **Quy tắc chuyển:**
  - `PENDING` ➔ `IN_PROGRESS` (Chỉ cho phép khi request **đã được gán** developer).
  - `IN_PROGRESS` ➔ `DONE` (Khi chuyển sang `DONE`, hệ thống tự động cập nhật mốc hoàn thành `last_completed_at = now()` cho Developer).
  - Các bước chuyển lùi (`IN_PROGRESS -> PENDING`), nhảy cóc (`PENDING -> DONE`), hoặc đổi trạng thái từ `DONE` sẽ bị từ chối với lỗi **`409 Conflict`** ("Invalid status transition").

### 3.2 Thuật toán Auto-Assign Developer (`auto = true`)

1. **Khóa chống tranh chấp:** Thực hiện truy vấn danh sách `DEVELOPER` kèm khóa ghi pessimistic:  
   `SELECT * FROM members WHERE role = 'DEVELOPER' ORDER BY id ASC FOR UPDATE`.
2. **Kiểm tra khả dụng:** Nếu danh sách rỗng, lập tức ném lỗi **`422 Unprocessable Entity`** (`"No developer available for auto-assignment"`).
3. **Tính tải động (Task Count):**  
   Với mỗi Developer, tính `currentTaskCount = COUNT(requests WHERE assigned_developer_id = dev.id AND status != 'DONE')`.
4. **Chọn người ít việc nhất:** Tìm `minCount = min(currentTaskCount)`. Lọc nhóm Developer có `currentTaskCount == minCount`.
5. **Xử lý hòa tải (Tie-breaking):**
   - Nếu nhóm chỉ có 1 người: Chọn người đó.
   - Nếu có từ 2 người trở lên: Sắp xếp theo `last_completed_at` giảm dần (người mới hoàn thành gần đây nhất được ưu tiên trước; người `null` chưa từng xong việc xếp cuối cùng). Nếu tiếp tục hòa, chọn người có `id` nhỏ hơn.

### 3.3 Tự động hóa Lịch sử & Cảnh báo (Atomic History & Alerts)

Mọi thao tác nghiệp vụ thay đổi dữ liệu phải nằm trong cùng **`@Transactional`**:

| Thao tác nghiệp vụ | Ghi lịch sử (`request_histories`) | Tạo Cảnh báo (`alerts`) |
|---|---|---|
| **Tạo Request `HIGH`** | Không | Gửi đến **tất cả ADMIN**: `HIGH_PRIORITY_REGISTERED` |
| **Gán Developer (Auto/Manual)** | `from=null, to=null, memo="assigned..."` | Gửi đến **DEVELOPER được gán**: `ASSIGNED` |
| **Đổi Status** | `from=oldStatus, to=newStatus, memo=inputMemo` | Gửi đến **CLIENT sở hữu**: `STATUS_CHANGED` |

---

## 4. Đặc tả Chi tiết các Endpoint REST API

### 4.1 Request Core APIs

#### `POST /api/requests`
- **Mô tả:** Đăng ký yêu cầu mới (chỉ dành cho `CLIENT`).
- **Authorization:** Bearer JWT (Role: `CLIENT`).
- **Request Body:** `RequestCreateRequest`
  ```json
  {
    "title": "Lỗi đăng nhập hệ thống",
    "description": "Trang đăng nhập báo lỗi 500 khi ấn nút Submit",
    "category": "BUG",
    "priority": "HIGH"
  }
  ```
- **Response `201 Created`:** `ApiResponse<RequestResponse>`

---

#### `GET /api/requests`
- **Mô tả:** Tra cứu danh sách yêu cầu (Phân trang, sắp xếp, lọc tổ hợp & áp phạm vi theo vai trò).
- **Authorization:** Bearer JWT (`ADMIN`, `DEVELOPER`, `CLIENT`).
- **Query Parameters:**
  - `page` (integer, default: `0`)
  - `size` (integer, default: `10`)
  - `sort` (string, example: `createdAt,desc`)
  - `status` (string, enum: `PENDING`, `IN_PROGRESS`, `DONE`)
  - `category` (string, enum: `BUG`, `FEATURE`, `INQUIRY`)
  - `priority` (string, enum: `HIGH`, `MEDIUM`, `LOW`)
  - `keyword` (string, tìm kiếm không phân biệt hoa thường trong `title` hoặc `description`)
- **Validation:** `page >= 0`, `size=1..100`; sort field chỉ gồm `id`, `title`, `category`,
  `priority`, `status`, `clientId`, `assignedDeveloperId`, `createdAt`, `updatedAt`. Giá trị ngoài
  contract trả `400 Bad Request`.
- **Response `200 OK`:** `ApiResponse<PageResponse<RequestResponse>>`

---

#### `GET /api/requests/{id}`
- **Mô tả:** Lấy chi tiết một yêu cầu theo ID.
- **Authorization:** Bearer JWT.
- **Phân quyền sở hữu:**
  - `ADMIN`: Xem được mọi request.
  - `CLIENT`: Chỉ xem request do chính mình tạo.
  - `DEVELOPER`: Chỉ xem request được gán cho chính mình.
  - Vi phạm ➔ **`403 Forbidden`**. Không tìm thấy ➔ **`404 Not Found`**.
- **Response `200 OK`:** `ApiResponse<RequestResponse>`

---

### 4.2 Request Workflow APIs

#### `PATCH /api/requests/{id}/assign`
- **Mô tả:** Gán developer xử lý (chỉ dành cho `ADMIN`).
- **Authorization:** Bearer JWT (Role: `ADMIN`).
- **Request Body:** `AssignRequest`
  - Chế độ tự động: `{"auto": true, "expectedVersion": 0}`
  - Chế độ thủ công: `{"auto": false, "developerId": 2, "expectedVersion": 0}`
- **Response `200 OK`:** `ApiResponse<RequestResponse>`
- **Mã lỗi đặc thù:**
  - `409 Conflict`: Khóa lạc quan thất bại (`expectedVersion` không khớp).
  - `422 Unprocessable Entity`: Không có developer nào trong hệ thống khi gán tự động.

---

#### `PATCH /api/requests/{id}/status`
- **Mô tả:** Cập nhật trạng thái yêu cầu.
- **Authorization:** Bearer JWT (`ADMIN` hoặc `DEVELOPER` được phân công).
- **Request Body:** `StatusUpdateRequest`
  ```json
  {
    "status": "IN_PROGRESS",
    "memo": "Đang kiểm tra log nguyên nhân lỗi",
    "expectedVersion": 1
  }
  ```
- `memo` không bắt buộc, tối đa 255 ký tự.
- **Response `200 OK`:** `ApiResponse<RequestResponse>`
- **Mã lỗi đặc thù:**
  - `403 Forbidden`: Người gọi không phải ADMIN và không phải Developer được gán.
  - `409 Conflict`:
    - Request chưa được gán developer.
    - Chuyển trạng thái sai quy định State Machine.
    - Version khóa lạc quan không khớp.

---

#### `GET /api/requests/{id}/history`
- **Mô tả:** Truy xuất toàn bộ lịch sử biến động trạng thái và gán việc của request.
- **Authorization:** Bearer JWT (Tuân thủ cùng luật `RequestAccessPolicy` như chi tiết request).
- **Response `200 OK`:** `ApiResponse<List<HistoryResponse>>`
  ```json
  {
    "status": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "requestId": 10,
        "changedBy": 1,
        "fromStatus": null,
        "toStatus": null,
        "memo": "auto-assigned to Dev One",
        "changedAt": "2026-08-08T10:00:00Z"
      },
      {
        "id": 2,
        "requestId": 10,
        "changedBy": 2,
        "fromStatus": "PENDING",
        "toStatus": "IN_PROGRESS",
        "memo": "Đang xử lý",
        "changedAt": "2026-08-08T10:15:00Z"
      }
    ]
  }
  ```

---

## 5. Danh mục Mã Lỗi HTTP & Envelope Response

Toàn bộ API trả về cấu trúc Envelope thống nhất:
```json
{
  "status": 200,
  "message": "success",
  "data": { ... }
}
```

| HTTP Status | Trường hợp sử dụng | Ví dụ JSON Message |
|---|---|---|
| `200 OK` | Đọc, cập nhật thành công | `{ "status": 200, "message": "success", "data": ... }` |
| `201 Created` | Tạo request mới thành công | `{ "status": 201, "message": "created", "data": ... }` |
| `400 Bad Request` | Vi phạm validation DTO (thiếu title, title > 200 ký tự...) | `{ "status": 400, "message": "title: must not be blank", "data": null }` |
| `401 Unauthorized` | Không có Bearer token hoặc token không hợp lệ | `{ "status": 401, "message": "Unauthorized", "data": null }` |
| `403 Forbidden` | Đã xác thực nhưng truy cập ngoài phạm vi sở hữu/role | `{ "status": 403, "message": "Forbidden", "data": null }` |
| `404 Not Found` | Không tìm thấy Request ID | `{ "status": 404, "message": "Request not found: 999", "data": null }` |
| `409 Conflict` | Sai transition status, request chưa gán dev, xung đột version | `{ "status": 409, "message": "Invalid status transition: PENDING -> DONE", "data": null }` |
| `422 Unprocessable` | Chạy auto-assign nhưng không có DEVELOPER nào | `{ "status": 422, "message": "No developer available for auto-assignment", "data": null }` |
