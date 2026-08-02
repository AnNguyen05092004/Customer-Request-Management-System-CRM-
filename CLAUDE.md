# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

# Bzcom CRM — Project-Specific Instructions

> Phần này bổ sung cho các rule LLM chung ở trên và là working agreement bắt buộc khi
> thành viên hoặc coding agent thay đổi repository. Mục tiêu là giữ chuỗi
> **task → docs/contract → code → test → Pull Request** luôn truy vết được và đồng bộ.

## P1. Nguyên tắc đồng bộ

1. Đọc đủ context liên quan trước khi sửa; không suy đoán contract hoặc nghiệp vụ.
2. Code phải bám tài liệu đã thống nhất. Không tự đổi API, schema hoặc business rule để
   implementation thuận tiện hơn.
3. Tài liệu không bất biến. Nếu phương án hiện tại sai, thiếu, không an toàn hoặc kém thực
   tế, đề xuất phương án tốt hơn và cập nhật **docs + code + test trong cùng PR**.
4. Không để trạng thái “code đúng nhưng docs cũ” hoặc “docs mới nhưng code chưa theo”. Nếu
   buộc phải chia nhiều PR, PR đầu chỉ thêm thay đổi tương thích ngược hoặc feature flag;
   ghi rõ dependency và thứ tự merge.
5. Chỉ tick task `[x]` sau khi đạt toàn bộ Definition of Done, test pass và tài liệu liên
   quan đã đồng bộ. Làm một phần thì giữ `[ ]` và ghi rõ `Trạng thái:` cùng phần còn thiếu.

## P2. Nguồn chuẩn theo từng loại quyết định

Không dùng một thứ tự ưu tiên duy nhất cho mọi vấn đề. Mỗi tài liệu là nguồn chuẩn cho một
phạm vi:

| Phạm vi | Nguồn chuẩn | Tài liệu hỗ trợ |
|---|---|---|
| HTTP: path, method, auth, parameter, body, response, status | [`docs/openapi.yaml`](./docs/openapi.yaml) | [`docs/OPENAPI.md`](./docs/OPENAPI.md) |
| Nghiệp vụ, quyền dữ liệu, state machine, alert, history, concurrency | [`docs/BUSINESS_LOGIC.md`](./docs/BUSINESS_LOGIC.md) | [`docs/ANALYZE.md`](./docs/ANALYZE.md) |
| Data model, quan hệ, constraint, index | [`docs/ERD.md`](./docs/ERD.md) | Flyway trong `BE/src/main/resources/db/migration/` |
| Kiến trúc, module boundary, dependency, profile/triển khai | [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) | [`docs/BACKEND_CODING_RULES.md`](./docs/BACKEND_CODING_RULES.md) |
| Convention và test Java/Spring Boot | [`docs/BACKEND_CODING_RULES.md`](./docs/BACKEND_CODING_RULES.md) | Code hiện có đã review |
| Kiến trúc, route, state, API client và UX frontend | [`docs/FRONTEND.md`](./docs/FRONTEND.md) | [`FE/README.md`](./FE/README.md) |
| LLM input/output, prompt, verification, fallback | [`docs/LLM.md`](./docs/LLM.md) | OpenAPI và business logic |
| Owner, dependency, phạm vi và DoD | [`docs/TASKS.md`](./docs/TASKS.md) | [`docs/TEAM_DEVELOPMENT_GUIDE.md`](./docs/TEAM_DEVELOPMENT_GUIDE.md) |
| Branch, commit, review và CI | [`docs/GIT_WORKFLOW.md`](./docs/GIT_WORKFLOW.md) | `.github/workflows/` |
| Môi trường và cách chạy | [`docs/TEAM_DEVELOPMENT_GUIDE.md`](./docs/TEAM_DEVELOPMENT_GUIDE.md) | `docs/README.md`, README của BE/FE |

Quy ước:

- `openapi.yaml` là HTTP contract canonical; Swagger annotation/runtime phải khớp.
- Flyway là lịch sử schema thực thi. `ERD.md`, migration và JPA entity phải mô tả cùng một
  mô hình. Không sửa migration đã chạy ở môi trường dùng chung; tạo migration version mới.
- `TASKS.md` là tracker và đường dẫn tới spec, không phải nơi tạo thêm một bản business rule
  hoặc API contract.
- Code đang chạy không tự động trở thành nguồn chuẩn nếu lệch contract đã chốt.
- Ví dụ trong tài liệu hỗ trợ không được mâu thuẫn với nguồn canonical tương ứng.

## P3. Khi yêu cầu, docs và code mâu thuẫn

Không chọn âm thầm một phía:

1. Xác định phạm vi mâu thuẫn và nguồn chuẩn trong bảng P2.
2. Kiểm tra yêu cầu bài toán, acceptance criteria, bảo mật, dữ liệu cũ và module phụ thuộc.
3. Nếu implementation sai/thiếu: sửa code và test để theo contract hiện tại.
4. Nếu docs/design chưa hợp lý: ghi lý do và tác động; thống nhất với owner/leader trước
   breaking change hoặc thay đổi xuyên module.
5. Cập nhật nguồn canonical trước hoặc trong cùng PR, rồi cập nhật tài liệu dẫn xuất, code,
   migration, test và task bị ảnh hưởng.
6. Ghi decision, breaking change và migration note trong PR; reviewer kiểm hai chiều
   docs ↔ code.

Ưu tiên tính đúng, an toàn và khả năng vận hành thực tế hơn việc giữ thiết kế cũ chỉ vì nó
đã được viết ra. Tuy nhiên không được “sửa tiện tay” một quyết định đã thống nhất.

## P4. Traceability bắt buộc

```text
TASKS.md (T-x.y + Refs + Depends on + DoD)
        ↓
canonical docs / OpenAPI / ERD
        ↓
implementation BE và/hoặc FE
        ↓
unit + integration/contract test phù hợp
        ↓
PR: task, refs, docs sync, test và tác động
```

### Trước khi code

1. Chọn task ID trong `docs/TASKS.md`; đọc `Depends on`, `Refs`, `Việc` và `DoD`.
2. Mở trực tiếp mọi link trong `Refs`, không chỉ đọc mô tả task.
3. Tìm code/test liên quan và kiểm tra `develop` mới nhất để không tạo lại code đã có.
4. Định nghĩa success criteria kiểm chứng được. Task thiếu ref/DoD thì sửa trong cùng PR.
5. Xác định file dùng chung, module owner và dependency với thành viên khác.

### Trong khi code

1. Giữ package-by-feature và chiều phụ thuộc trong `ARCHITECTURE.md`.
2. Viết/cập nhật test cho main flow, validation, quyền và edge case có tác động.
3. Nếu đổi thiết kế, cập nhật docs ngay trong lúc làm, không để tới cuối dự án.
4. Không copy contract sang nhiều nơi; link tới canonical source, chỉ thêm giải thích thuộc
   vai trò của tài liệu đó.
5. Không đưa task ID vào comment production code; code phải tự mô tả nghiệp vụ.

### Trước khi mở PR

1. So diff với task và bỏ thay đổi ngoài phạm vi.
2. Chạy quality gate phù hợp ở P7.
3. Kiểm mọi Markdown link vừa thêm/sửa: file đích tồn tại, anchor đúng, dùng relative path.
4. Đối chiếu ma trận P5 và cập nhật artifact cần thiết.
5. Cập nhật task status/dependency đúng thực tế.
6. PR ghi task ID, refs, docs đã kiểm/sửa, test đã chạy và breaking change nếu có.

## P5. Ma trận tác động cần kiểm tra

| Loại thay đổi | Artifact bắt buộc kiểm tra/cập nhật |
|---|---|
| API path/method/auth/DTO/status/error | `openapi.yaml`, `OPENAPI.md` nếu có ví dụ/quy ước, controller/DTO/Swagger, FE type/hook, contract/integration test, `TASKS.md` |
| Table/column/FK/index/constraint | Flyway migration mới, `ERD.md`, JPA entity/repository, PostgreSQL integration test; OpenAPI/FE nếu public data đổi |
| Business rule/permission/status transition | `BUSINESS_LOGIC.md`, `ANALYZE.md` nếu acceptance criteria đổi, service/policy/test; OpenAPI nếu external behavior đổi |
| Auth/security/token/CORS | OpenAPI security/response, `BUSINESS_LOGIC.md`, `ARCHITECTURE.md`, BE/FE implementation và security test |
| Module/layer/package/dependency | `ARCHITECTURE.md`, `BACKEND_CODING_RULES.md`, code/test, owner/ref trong `TASKS.md` |
| Frontend route/state/API/UX | `FRONTEND.md`, FE type/hook/page/test; chỉ đổi OpenAPI khi HTTP contract thực sự đổi |
| LLM input/output/prompt/fallback | `LLM.md`, OpenAPI, service/controller, mock/fallback test và FE liên quan |
| Profile/env/Docker/run command | `ARCHITECTURE.md`, `.env.example`, compose/Dockerfile, `docs/README.md`, team guide, README BE/FE |
| CI/quality gate/Git workflow | Workflow thật, `GIT_WORKFLOW.md`, team guide, PR template và `TASKS.md` nếu DoD đổi |
| Task status/dependency | `TASKS.md`: `Refs`, `Depends on`, owner, trạng thái và DoD liên quan |

Không phải PR nào cũng sửa mọi file trong một hàng. Reviewer phải thấy các mục đã được
kiểm tra; chỉ sửa file khi nội dung thực sự thay đổi.

## P6. Quy tắc implementation chính

### Backend

- Java 21, Spring Boot, package-by-feature trong `com.bzcom.crm`.
- Controller chỉ xử lý HTTP/validation/delegation; nghiệp vụ và transaction ở service.
- Không trả entity qua API; dùng DTO và mapper.
- PostgreSQL + Flyway; integration test không thay production behavior bằng H2.
- Response envelope, status, validation và authorization phải đúng contract.
- Phân quyền có route/method security và kiểm tra data scope trong service.
- Không log password, JWT, refresh token, secret hoặc dữ liệu nhạy cảm.
- Tuân thủ [`docs/BACKEND_CODING_RULES.md`](./docs/BACKEND_CODING_RULES.md).

### Frontend

- TypeScript strict; không dùng `any` để né type contract.
- HTTP đi qua API client/hook dùng chung; page/component không tự tạo axios client.
- Route guard phục vụ UX; backend vẫn cưỡng chế authorization.
- Server state dùng TanStack Query; cache key/invalidation nhất quán theo feature.
- Demo adapter khi BE chưa sẵn sàng phải được đánh dấu rõ, không làm đổi contract dự kiến.
- Tuân thủ [`docs/FRONTEND.md`](./docs/FRONTEND.md).

## P7. Quality gate

Backend hoặc migration/contract backend:

```bash
cd BE
./mvnw -B verify
```

Frontend hoặc type/API hook mà FE dùng:

```bash
cd FE
npm ci                 # lần đầu hoặc khi package-lock.json đổi
npm run verify
```

Compose, proxy, CORS, profile hoặc tích hợp BE–FE:

```bash
docker compose up --build --wait
docker compose ps
docker compose down
```

Endpoint thay đổi phải đối chiếu `docs/openapi.yaml`; schema phải kiểm chứng trên PostgreSQL
qua integration test. Không tuyên bố pass nếu chưa chạy; ghi phần chưa kiểm chứng và lý do.

## P8. Git và Pull Request

- Tạo branch mới từ `develop`; không tiếp tục commit lên feature branch đã merge.
- Không push thẳng `main`/`develop`; không force-push nhánh người khác.
- Commit nhỏ, một ý nghĩa, theo `docs/GIT_WORKFLOW.md`.
- Không commit `.env`, secret, output build, IDE config hoặc dữ liệu local.
- Không sửa/xóa thay đổi của người khác để làm sạch worktree.
- PR vào `develop` cần review, CI xanh và không còn conflict.

PR description tối thiểu:

```markdown
## Task và phạm vi
- Task: T-x.y
- Refs: docs/...#section, docs/openapi.yaml

## Thay đổi
- ...

## Đồng bộ contract/docs
- [ ] Không đổi contract/docs
- [ ] Đã cập nhật: ...
- Breaking change/migration note: ...

## Kiểm chứng
- `cd BE && ./mvnw -B verify`
- `cd FE && npm run verify`
- Kiểm thử thủ công: ...
```

## P9. Definition of Done chung

Một thay đổi chỉ hoàn thành khi:

- đạt acceptance criteria và DoD của task;
- code, docs, OpenAPI, ERD/migration và FE/BE contract không mâu thuẫn;
- có test tương xứng rủi ro và quality gate liên quan pass;
- không lộ secret, không có lỗ hổng phân quyền, không làm mất dữ liệu;
- link task → docs còn đúng, dependency/task status đã cập nhật;
- PR đủ quyết định, tác động và cách kiểm chứng để reviewer làm việc độc lập.

Thiếu một mục nghĩa là công việc vẫn **in progress**; ghi rõ phần còn thiếu thay vì đánh dấu
hoàn thành hoặc che giấu giới hạn kiểm chứng.
