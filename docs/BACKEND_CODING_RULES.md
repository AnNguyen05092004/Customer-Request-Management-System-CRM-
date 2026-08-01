# Backend Coding Rules — Java Spring Boot

> Quy ước bắt buộc khi code backend Bzcom CRM. Mục tiêu là để 4 người tạo ra một codebase nhất quán, dễ review và bám đúng API contract — không phải để thêm ceremony.

## 1. Nguồn chuẩn và nguyên tắc làm việc

- API contract chuẩn là [openapi.yaml](./openapi.yaml). Không tự đổi path, method, request/response field hoặc HTTP status trong code.
- Luật nghiệp vụ chuẩn là [ANALYZE.md](./ANALYZE.md) và [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md).
- Mỗi thay đổi contract phải cập nhật **OpenAPI + docs liên quan + FE type + test** trong cùng PR.
- Không code theo assumption im lặng. Nếu rule chưa rõ, ghi câu hỏi vào PR/issue trước khi implement.
- Ưu tiên code đơn giản, rõ ràng, test được; không thêm generic framework, CQRS, event bus, cache hoặc abstraction chưa cần cho MVP.

## 2. Cấu trúc package và chiều phụ thuộc

Code dùng package-by-feature:

```text
com.bzcom.crm
├── common/       # response, exception, BaseTimeEntity, utility thật sự dùng chung
├── config/       # Security/OpenAPI/JPA/CORS configuration
├── auth/         # auth controller, service, JWT, refresh token
├── member/       # member controller, service, repository, entity, DTO, mapper
├── request/      # request core, filter, stats
├── workflow/     # assign, status, history
├── alert/        # alerts
└── llm/          # LLM facade, provider implementations, prompts
```

Trong mỗi feature, đặt `controller`, `service`, `repository`, `entity`, `dto/request`, `dto/response`, `mapper` chỉ khi cần. Chiều phụ thuộc duy nhất:

```text
Controller → Service → Repository → Entity
```

- Controller không gọi repository, không tự kiểm ownership, không chứa transaction/business rule.
- Repository chỉ chứa query/persistence; không gọi service khác.
- Một feature không sửa entity của feature khác trực tiếp. Giao tiếp qua public service/interface nhỏ, ví dụ `AlertService.create(...)`.
- `common` không được phụ thuộc vào feature; tránh biến thành “thùng rác tiện đâu ném đó”.

## 3. Controller và API

- Controller mỏng: nhận DTO, `@Valid`, lấy current user, gọi service, trả `ApiResponse`.
- Không trả JPA entity, `Page`, `Optional`, exception hoặc password ra HTTP.
- Route/method/status phải đúng OpenAPI. Tạo mới dùng `201`, đọc/cập nhật dùng `200`.
- JSON dùng `camelCase`; enum viết hoa; thời gian trả ISO-8601 UTC (`Instant`/offset có `Z` khi có thể).
- Mọi endpoint business cần authentication, trừ đúng bốn POST: login, refresh, logout, register CLIENT; Swagger được permit theo config.
- `refresh` và `logout` nhận refresh token body, không dùng/không yêu cầu Bearer access token.
- `@PreAuthorize` kiểm role thô; ownership (client/dev chỉ đọc hoặc sửa resource của mình) luôn kiểm ở service qua policy dùng chung.

Ví dụ controller đúng:

```java
@PatchMapping("/api/requests/{id}/status")
public ResponseEntity<ApiResponse<RequestResponse>> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest body,
        @AuthenticationPrincipal CurrentUser currentUser) {
    return ResponseEntity.ok(ApiResponse.success(statusService.update(id, body, currentUser)));
}
```

## 4. DTO, mapping và validation

- DTO request/response dùng `record` nếu không cần mutable. Tên rõ mục đích: `RequestCreateRequest`, `AssignRequest`, `RequestResponse`.
- Request DTO và response DTO tách riêng. Tuyệt đối không dùng `RequestResponse`, `Map<String,Object>` hoặc `Partial`-style DTO để nhận create/update.
- Entity không xuất hiện trong chữ ký controller.
- Validation ở request DTO bằng Jakarta Validation: `@NotBlank`, `@Email`, `@Size`, `@NotNull`, `@Positive`, `@Min`.
- Validation cross-field đặt trong service hoặc custom validator: `auto=false` bắt buộc `developerId`; `auto=true` không nhận developerId.
- Public registration DTO không có `role`; cấu hình `spring.jackson.deserialization.fail-on-unknown-properties=true` để reject field lạ. Không “ignore” input có thể leo quyền.
- Mapper MapStruct khai báo `@Mapper(config = CentralMapperConfig.class)` để thống nhất Spring constructor injection và fail compile khi bỏ sót target field. Các field hệ thống (`id`, `clientId`, `status`, `version`, timestamps, password hash) do service kiểm soát, không map trực tiếp từ client.
- Response không bao giờ có password/hash, JWT secret, refresh raw token trong log, hay entity lazy graph.

## 5. Service, transaction và nghiệp vụ

- Service method đặt theo use case: `create`, `assign`, `updateStatus`, `refresh`, `markRead`; tránh tên mơ hồ như `processData`.
- `@Transactional` đặt ở public service method thay đổi dữ liệu. Controller không có `@Transactional`.
- Một use case phải atomic: thay đổi request + history + alert cùng commit hoặc cùng rollback.
- Read-only query phức tạp có thể dùng `@Transactional(readOnly = true)`; không dùng transaction rộng bao quanh HTTP/LLM call.
- Không gọi LLM/external HTTP khi đang giữ DB lock. Lấy/xác thực dữ liệu trước, gọi provider có timeout, sau đó lưu nếu use case cần.
- Không bắt `Exception` rồi nuốt lỗi. Chỉ bắt lỗi có cách xử lý xác định (ví dụ LLM timeout → fallback).

### Quy tắc CRM bắt buộc

- `POST /members` luôn tạo `CLIENT`.
- Chỉ CLIENT tạo request; `clientId` lấy từ JWT, status luôn `PENDING`.
- ADMIN assign; DEVELOPER chỉ update status của request được gán cho mình.
- `PENDING → IN_PROGRESS → DONE`; request chưa có assignee không được vào `IN_PROGRESS`.
- Assign/status bắt buộc `expectedVersion`; response phải trả `version` mới.
- Assign/status tự tạo một history. Tạo HIGH tự alert ADMIN; assign alert dev; status alert client.
- `lastCompletedAt` chỉ cập nhật khi request có assignee chuyển `DONE`.

## 6. Persistence và PostgreSQL

- Database chuẩn là PostgreSQL 16. Không viết SQL theo H2 rồi hy vọng production chạy được.
- Chỉ Flyway tạo/đổi schema. Không dùng `ddl-auto=update`; dev/test dùng migrate.
- Migration bất biến: đã merge/chạy ở môi trường chung thì không sửa `V1`; tạo `V2__...sql`, `V3__...sql` mới.
- Mọi FK ghi rõ `ON DELETE RESTRICT` để giữ audit trail.
- Entity enum dùng `@Enumerated(EnumType.STRING)`. Không dùng ordinal.
- Entity request có `@Version`; không tự tăng version bằng tay.
- Timestamp dùng một quy ước nhất quán trong code và DB; dùng clock injectable nếu test thời gian cần deterministic.
- Repository query có tên diễn đạt ý nghĩa. Native SQL chỉ khi JPA/Specification không diễn đạt rõ; kèm test PostgreSQL.
- Không để N+1 trong list/detail. Chọn projection, fetch join hoặc query phù hợp và phân trang đúng.

### Refresh token

- Access JWT 15 phút; opaque refresh token 7 ngày từ `SecureRandom`.
- Chỉ SHA-256 hash refresh token được lưu. Không log raw refresh token.
- Refresh rotation phải atomic: conditional revoke `revoked_at IS NULL AND expires_at > now()` hoặc lock đúng row token. Chỉ khi exactly one row bị revoke mới phát cặp token mới.
- Logout idempotent: revoke nếu token tồn tại, luôn trả 200 hợp lệ.

## 7. Security và lỗi

- Password dùng `BCryptPasswordEncoder`; không tự hash SHA/MD5 password.
- JWT secret/API key/password DB chỉ qua environment/config, không commit `.env` hoặc log.
- JWT filter chỉ tạo `Authentication` từ access token hợp lệ; token lỗi/hết hạn ở endpoint bảo vệ trả 401.
- Không tin `role`, `clientId`, `assignedDeveloperId`, `status` hay `version` từ client nếu contract không cho phép.
- Mọi lỗi đi qua `@RestControllerAdvice` và envelope `{status,message,data:null}`. `ErrorCode` phải có mã ổn định, message an toàn và HTTP status; `BusinessException` immutable chỉ mang `ErrorCode`/ngữ cảnh an toàn. Không lộ stack trace, SQL hoặc secret.
- Dùng exception có nghĩa: `ResourceNotFoundException`, `ForbiddenException`, `VersionConflictException`, `InvalidStatusTransitionException`, `RequestNotAssignedException`.
- Map status nhất quán: validation 400, unauthenticated 401, forbidden 403, not found 404, conflict 409, auto-assign không có dev 422.

## 8. Đồng thời

- Assign/status: service kiểm `expectedVersion` trước khi sửa; JPA `@Version` là guard thứ hai. Cả hai failure map 409.
- Auto-assign lock danh sách DEVELOPER theo `id ASC` chỉ trong transaction chọn tải. Không pessimistic-lock mọi request đọc/status.
- Refresh dùng atomic conditional revoke như §6; không chỉ `find → if !revoked → save`.
- Test race condition khi contract yêu cầu: hai status/assign với cùng version và hai refresh với cùng token.

## 9. Test bắt buộc

- Mỗi task backend phải có tối thiểu một main-flow test và một test lỗi/quyền liên quan.
- Unit test (JUnit 5 + Mockito) cho service/enum thuần: state machine, tie-break auto-assign, mapper/validation logic, fallback LLM.
- Controller-slice test: dùng `@WebMvcTest` + `MockMvc`, mock service dưới controller; kiểm DTO validation, envelope, HTTP status. Không cần khởi full context cho case này.
- Integration test (`@SpringBootTest` + MockMvc + Testcontainers PostgreSQL) cho controller, Security, Flyway, repository query và transaction.
- Không mock chính thứ cần kiểm chứng: PostgreSQL migration/query, `@Version`, Security filter, refresh rotation phải có integration test.
- Test tối thiểu cho auth: login đúng/sai, endpoint bảo vệ 401, role 403, refresh rotation, refresh song song chỉ một 200, logout revoke.
- Test tối thiểu workflow: ownership 403, request chưa assign 409, transition sai 409, version cũ 409, history/alert cùng transaction.
- Tên test mô tả hành vi: `shouldReturn409WhenUpdatingUnassignedRequest()`; Arrange–Act–Assert rõ ràng.
- Test không phụ thuộc thứ tự/seed global; mỗi test tự dựng dữ liệu cần thiết.

### Maven quality gate

- `cd BE && ./mvnw -B verify` là lệnh local/CI chuẩn: compile, unit/integration test, Spotless check và JaCoCo report.
- Dùng Spotless để format/import order tự động; không format bằng tay cả repo trong PR feature.
- JaCoCo dùng để phát hiện vùng quan trọng chưa test; không đặt coverage percentage gate cho MVP nếu nó làm team tốn thời gian, nhưng service workflow/auth mới phải có test.

## 10. Java style và review

- Java 21, Spring Boot 3.5.5; formatter và import do Spotless trong `BE/pom.xml` quyết định. Không commit file IDE cá nhân.
- Tên class PascalCase; method/field camelCase; constant UPPER_SNAKE_CASE; boolean bắt đầu `is/has/can`.
- Một public class/record mỗi file. Method ngắn, guard clause sớm; không lồng nhiều `if`.
- Ưu tiên constructor injection; không dùng field injection hoặc `@Autowired` field.
- Lombok chỉ dùng nơi giảm boilerplate rõ ràng; entity không dùng `@Data` (tránh equals/hashCode/toString gây lazy load). DTO record thường không cần Lombok.
- Không dùng wildcard import, magic number/string; enum/error code/constant cho giá trị lặp lại.
- Comment giải thích **vì sao**, không diễn tả lại code hiển nhiên. TODO phải có owner/issue hoặc bỏ.

## 11. Checklist trước khi tạo PR

- [ ] Đúng OpenAPI path, DTO, status, envelope và quyền.
- [ ] Controller mỏng; business logic nằm service; entity không lộ ra API.
- [ ] Validation, 401/403/404/409/422 và exception handler đã có test phù hợp.
- [ ] Migration mới (nếu đổi schema), chạy được trên Testcontainers PostgreSQL.
- [ ] Không có secret, token/password raw trong code/log/test fixture.
- [ ] `cd BE && ./mvnw -B verify` xanh; OpenAPI runtime snapshot không lệch `openapi.yaml`.
- [ ] PR nhỏ, một mục đích; mô tả test đã chạy và ảnh hưởng contract nếu có.

---

*Quy tắc này bổ sung cho [ARCHITECTURE.md](./ARCHITECTURE.md), [TASKS.md](./TASKS.md) và [openapi.yaml](./openapi.yaml). Khi mâu thuẫn, `openapi.yaml` quyết định HTTP contract; business rules quyết định hành vi.*
