# BUSINESS_LOGIC — Logic nghiệp vụ cốt lõi

> Đặc tả chi tiết **5 khối logic** làm nên giá trị hệ thống, kèm pseudocode, edge case và ranh giới transaction. Đây là phần rubric chấm nặng nhất: *"auto-assign, transition, history chạy đúng"* ↔ *"lỗi logic hoặc không xử lý exception"*.
>
> Phục vụ **Slide 5 (Auth) · 6 (Auto-Assign) · 7 (History & Alerts)**.

---

## Mục lục
1. [Authentication & Authorization](#1-authentication--authorization)
2. [Auto-Assignment](#2-auto-assignment)
3. [Status State Machine](#3-status-state-machine)
4. [Automatic History Recording](#4-automatic-history-recording)
5. [Automatic Alert Generation](#5-automatic-alert-generation)
6. [Statistics](#6-statistics)
7. [Validation](#7-validation)
8. [Ranh giới transaction & đồng thời](#8-ranh-giới-transaction--đồng-thời)
9. [Bảng edge case tổng hợp](#9-bảng-edge-case-tổng-hợp)

---

## 1. Authentication & Authorization

### 1.1 Luồng JWT
1. `POST /api/auth/login` → tìm member theo email → `BCryptPasswordEncoder.matches(raw, hash)`.
2. Đúng → sinh **access JWT** 15 phút (chứa `sub=memberId`, `role`, `exp`) và **opaque refresh token** 7 ngày bằng `SecureRandom`.
3. Chỉ SHA-256 hash của refresh token được lưu trong `refresh_tokens`. `POST /api/auth/refresh` revoke token cũ bằng thao tác **nguyên tử** `UPDATE ... WHERE token_hash=? AND revoked_at IS NULL AND expires_at > now()` (hoặc lock chính row token), rồi chỉ khi update-count = 1 mới phát cặp mới trong cùng transaction. Vì vậy hai refresh đồng thời chỉ có một request thành công; request còn lại trả 401.
4. `POST /api/auth/logout` nhận refresh token, revoke token nếu tồn tại; endpoint trả 200 idempotent để không tiết lộ token tồn tại. Access JWT đã phát hành không bị blacklist, nên chỉ còn hiệu lực tối đa 15 phút.
5. Mọi business request sau đi qua `JwtAuthenticationFilter`: đọc header `Authorization: Bearer`, verify chữ ký + hạn, nạp `Authentication` (với authorities `ROLE_ADMIN`...) vào `SecurityContext`. Token sai/hết hạn → 401; thiếu quyền → 403.

### 1.2 Hai tầng phân quyền
| Tầng | Cơ chế | Ví dụ |
|---|---|---|
| **Role-level** (thô) | `@PreAuthorize("hasRole('ADMIN')")` trên method controller | Chỉ ADMIN gọi `/requests/stats` |
| **Ownership-level** (mịn) | Kiểm tra trong service: resource có thuộc về user không | DEVELOPER chỉ đổi status request **được gán cho mình** |

> **Vì sao cần cả 2?** Role-level không đủ: 2 developer cùng role nhưng chỉ được đụng việc của riêng mình. Ownership check phải nằm ở **service** (nơi có dữ liệu), không nằm ở URL. Nói được điều này = hiểu sâu phân quyền.

### 1.3 Ma trận truy cập dữ liệu request (áp trước mọi filter khác)
```
GET /api/requests theo role:
  ADMIN      → SELECT * FROM requests
  CLIENT     → WHERE client_id = :me
  DEVELOPER  → WHERE assigned_developer_id = :me
```
Truy cập chi tiết `/requests/{id}`, `/requests/{id}/history` và `/requests/{id}/summary`: dùng **cùng một** `RequestAccessPolicy.assertCanRead(request, currentMember)`. Nếu không thuộc phạm vi trên → **403**; request không tồn tại → **404**.

## 2. Auto-Assignment

### 2.1 Luật (BR-08)
- Gán cho DEVELOPER có **`currentTaskCount` nhỏ nhất** (`currentTaskCount` = số request đang gán & **chưa DONE**).
- **Hoà** → chọn người **hoàn thành gần nhất** (`last_completed_at` lớn nhất).
- Developer **chưa từng hoàn thành** (`last_completed_at = null`) → xếp **cuối** trong nhóm hoà.

### 2.2 Pseudocode
```
function autoAssign(request):
    developers = members WHERE role = DEVELOPER ORDER BY id FOR UPDATE
    if developers is empty:
        throw NoAvailableDeveloperException            # → HTTP 422

    for dev in developers:
        dev.taskCount = COUNT(requests
                              WHERE assigned_developer_id = dev.id
                                AND status != DONE)     # tính ĐỘNG, không lưu cột

    minCount = min(dev.taskCount for dev in developers)
    tied     = [dev for dev in developers if dev.taskCount == minCount]

    if tied.size == 1:
        chosen = tied[0]
    else:
        # hoàn thành gần nhất trước; null (chưa từng xong) xếp cuối
        chosen = tied.sortedByDesc(dev -> dev.last_completed_at ?? MIN_TIME)[0]

    assert request.version == command.expectedVersion # stale client → 409
    request.assigned_developer_id = chosen.id
    saveAndFlush(request)                               # @Version là guard thứ hai
    recordHistory(request, changedBy=currentAdmin,
                  fromStatus=null, toStatus=null,
                  memo="auto-assigned to " + chosen.name)   # BR-09
    createAlert(target=chosen, type=ASSIGNED,
                message="You were assigned: " + request.title)  # BR-11
    return request
```

### 2.3 Ví dụ minh hoạ (để demo/slide)
| Developer | currentTaskCount | last_completed_at | Kết quả |
|---|---|---|---|
| Dev A | 2 | 2026-07-01 | |
| Dev B | 1 | 2026-06-20 | ← chọn (count nhỏ nhất) |
| Dev C | 1 | 2026-06-30 | (hoà với B nhưng...) |

> Nếu B và C đều count = 1 → chọn **C** vì `last_completed_at` mới hơn (2026-06-30 > 2026-06-20). Đây là điểm hay để đưa vào slide 6.

> `FOR UPDATE` khóa danh sách developer theo cùng một thứ tự chỉ trong transaction auto-assign. Điều này serialize thao tác chọn tải, tránh hai request đồng thời cùng chọn một developer; không dùng pessimistic lock cho luồng đọc/status thông thường.

### 2.4 Thủ công (auto=false)
Body có `developerId` → kiểm tra member đó tồn tại & role = DEVELOPER → gán trực tiếp (vẫn ghi history + alert). Sai → 400/404.

## 3. Status State Machine

### 3.1 Sơ đồ trạng thái
```
        ┌─────────┐   allow   ┌───────────────┐   allow   ┌──────┐
        │ PENDING │──────────▶│  IN_PROGRESS  │──────────▶│ DONE │
        └─────────┘           └───────────────┘           └──────┘
             │                        │                       │
             │  ✗ PENDING→DONE        │  ✗ IN_PROGRESS→PENDING │  ✗ DONE→*
             ▼ (nhảy cóc)             ▼ (lùi)                  ▼ (bất biến)
          BLOCKED 409             BLOCKED 409              BLOCKED 409
```

### 3.2 Bảng transition
| Từ \ Sang | PENDING | IN_PROGRESS | DONE |
|---|:---:|:---:|:---:|
| **PENDING** | — | ✅ | ✗ (nhảy cóc) |
| **IN_PROGRESS** | ✗ (lùi) | — | ✅ |
| **DONE** | ✗ | ✗ | — |

Vi phạm → ném `InvalidStatusTransitionException` → **HTTP 409** với message rõ (`"Invalid status transition: DONE -> IN_PROGRESS"`).

### 3.3 Cài đặt (enum tự chứa luật — ADR-07)
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
Trong service:
```java
if (request.getVersion() != command.expectedVersion()) {
    throw new VersionConflictException();
}
if (request.getAssignedDeveloperId() == null) {
    throw new RequestNotAssignedException(); // 409
}
if (!request.getStatus().canTransitionTo(next)) {
    throw new InvalidStatusTransitionException(request.getStatus(), next);
}
```

> **Vì sao đặt luật trong enum?** Luật tập trung 1 chỗ → dễ test (`canTransitionTo` là hàm thuần), dễ giải thích, không rải rác if-else khắp service. Khi thêm trạng thái mới chỉ sửa 1 map.

### 3.4 Khi chuyển sang DONE
Ngoài đổi status: cập nhật `member.last_completed_at = now()` cho developer được gán → phục vụ tie-break auto-assign (§2). Đặt cùng transaction.

## 4. Automatic History Recording

### 4.1 Luật (BR-09)
Mỗi khi **đổi status** hoặc **gán developer** → tự tạo 1 bản ghi `request_histories`, **cùng `@Transactional`** với thao tác chính (nguyên tử: lỗi ghi history → rollback cả thao tác).

| Hành động | from_status | to_status | memo ví dụ |
|---|---|---|---|
| Auto-assign | null | null | "auto-assigned to Dev One" |
| Manual assign | null | null | "assigned to Dev Two by Admin" |
| Đổi status | trạng thái cũ | trạng thái mới | "start working" (memo người dùng nhập) |

> `from/to = null` khi gán vì status **không đổi** — phân biệt rõ 2 loại sự kiện trong cùng bảng lịch sử.

### 4.2 Điểm đặt code
`HistoryService.record(request, changedBy, from, to, memo)` — gọi từ `AssignService` và `StatusService`. Không rải logic ghi history ở controller.

## 5. Automatic Alert Generation

### 5.1 Luật (BR-10..12)
| Sự kiện | Người nhận | alert_type |
|---|---|---|
| Tạo request `priority = HIGH` | **tất cả** ADMIN | `HIGH_PRIORITY_REGISTERED` |
| Gán developer | developer được gán | `ASSIGNED` |
| Đổi status | CLIENT tạo request | `STATUS_CHANGED` |

### 5.2 Nguyên tắc
- Tạo alert nằm **cùng transaction** với hành động gây ra nó (vd tạo request + alert HIGH → cùng commit/rollback).
- `AlertService.create(targetMemberId, requestId, type, message)` là điểm vào duy nhất; các module khác (`request`, `workflow`) gọi qua interface này (giữ ranh giới bounded context — xem [ARCHITECTURE.md](./ARCHITECTURE.md#5-c%E1%BA%A5u-tr%C3%BAc-package-package-by-feature)).
- Alert chỉ lưu DB (không gửi email/Kakao thật — out-of-scope). Người dùng đọc qua `GET /api/alerts`.

### 5.3 Pseudocode tạo request HIGH
```
function createRequest(dto, currentClient):
    validate(dto)                                  # §7
    request = save(new Request(dto, client=currentClient, status=PENDING))
    if request.priority == HIGH:
        for admin in members WHERE role = ADMIN:
            AlertService.create(admin.id, request.id,
                                HIGH_PRIORITY_REGISTERED,
                                "New HIGH request: " + request.title)   # BR-10
    return request                                 # tất cả trong 1 @Transactional
```

## 6. Statistics

`GET /api/requests/stats` (ADMIN) trả:
| Chỉ số | Công thức |
|---|---|
| `total` | `COUNT(requests)` |
| `completed` | `COUNT(requests WHERE status = DONE)` |
| `completionRate` | `completed / total` (total=0 → 0, tránh chia 0) |
| `byCategory` | `GROUP BY category → COUNT` |
| `byDeveloper` | mỗi developer: `assignedCount` (đang gán) + `doneCount` (đã DONE) |

> **Edge case:** `total = 0` → completionRate = 0 (không ném lỗi chia 0). Đây là loại lỗi rubric để ý ("unhandled exceptions").

## 7. Validation

| Đối tượng | Luật | Vi phạm |
|---|---|---|
| Login | email đúng định dạng, password không rỗng | 400 |
| Member create | email unique + đúng format, password 4–72 ký tự, name 1–100 ký tự; DTO công khai không có `role`, field lạ bị reject | 400 / 409 (trùng email) |
| Request create | title không rỗng (≤200), category ∈ enum, priority ∈ enum | 400 |
| Status update | status ∈ enum | 400 (sai enum) / 409 (transition sai) |
| Assign | auto=false thì developerId bắt buộc & phải là DEVELOPER | 400 / 404 |

Cơ chế: Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@NotNull`) trên DTO + `@Valid` ở controller → sai bắt bởi `GlobalExceptionHandler` → 400 với message field cụ thể.

## 8. Ranh giới transaction & đồng thời

### 8.1 Transaction (`@Transactional` ở tầng service)
Mỗi thao tác nghiệp vụ là **một** transaction gồm: thao tác chính + history + alert. Ví dụ đổi status = {update request, record history, create alert} → hoặc tất cả thành công, hoặc rollback toàn bộ. Đảm bảo không có history/alert "mồ côi".

### 8.2 Optimistic lock (NFR-07, ADR-05)
- `Request` trả `version` trong response; mọi lệnh assign/status bắt buộc gửi `expectedVersion`.
- Service so version trước khi sửa để bắt stale request tuần tự; JPA `@Version` vẫn là guard ở DB cho hai transaction đồng thời. Bất kỳ guard nào fail → **409** ("Request was modified by someone else, refresh and retry").
- Vì sao optimistic (không pessimistic)? Tần suất ghi thấp → tránh khoá DB làm chậm; xung đột hiếm nên "phát hiện & báo retry" rẻ hơn "khoá phòng ngừa".

## 9. Bảng edge case tổng hợp

> Chuẩn bị sẵn cho Q&A — giám khảo hay hỏi "nếu... thì sao?".

| Tình huống | Xử lý | Status |
|---|---|---|
| Auto-assign khi không có DEVELOPER nào | `NoAvailableDeveloperException` | 422 |
| Đổi status `DONE → IN_PROGRESS` | `InvalidStatusTransitionException` | 409 |
| Đổi status `PENDING → DONE` | chặn (nhảy cóc) | 409 |
| DEVELOPER đổi status request không phải của mình | ownership check fail | 403 |
| CLIENT xem request người khác | phạm vi role fail | 403 |
| 2 dev đổi status cùng lúc | optimistic lock | 409 (1 người) |
| Tạo request thiếu title | validation | 400 |
| Đăng ký email đã tồn tại | unique constraint | 409 |
| GET request id không tồn tại | `EntityNotFoundException` | 404 |
| Stats khi chưa có request nào | completionRate = 0 | 200 |
| LLM lỗi mạng/parse | fallback rule/mock | 200 (không làm chết API) |
| Token hết hạn giữa chừng | filter reject | 401 |
| Refresh token đã revoke/hết hạn khi refresh | reject, yêu cầu login lại | 401 |
| Hai refresh cùng một token | chỉ request revoke nguyên tử thành công được phát token mới | 200 / 401 |
| Đổi `PENDING → IN_PROGRESS` khi chưa gán developer | `RequestNotAssignedException` | 409 |
| Gọi history/summary của request không thuộc mình | `RequestAccessPolicy` | 403 |
| PATCH với `expectedVersion` cũ | `VersionConflictException` | 409 |

---

*Luồng dữ liệu tổng thể & sequence diagram: [ARCHITECTURE.md §7](./ARCHITECTURE.md#7-sequence-diagram-c%C3%A1c-lu%E1%BB%93ng-ch%C3%ADnh). Chi tiết LLM: [LLM.md](./LLM.md).*
