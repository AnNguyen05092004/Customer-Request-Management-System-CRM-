# GIT_WORKFLOW — Quy trình cộng tác Git

> Chiến lược nhánh, quy ước commit, quy trình PR và CI cho **Bzcom CRM**. Rubric: *"branch, PR, commit convention thực thi thật"* ↔ *"push thẳng main, commit vô nghĩa"*.
>
> Phục vụ **Slide 10 — Git Flow**. Đây là tiêu chí dễ ăn điểm nếu **làm đúng ngay từ đầu**, và mất trắng nếu để cuối mới sửa.

---

## Mục lục
1. [Branch strategy](#1-branch-strategy)
2. [Commit convention](#2-commit-convention)
3. [Quy trình Pull Request](#3-quy-trình-pull-request)
4. [Branch protection](#4-branch-protection)
5. [GitHub Actions CI](#5-github-actions-ci)
6. [PR template](#6-pr-template)
7. [Chống xung đột merge (cho nhóm 4 người)](#7-chống-xung-đột-merge-cho-nhóm-4-người)
8. [Chuẩn bị bằng chứng cho slide 10](#8-chuẩn-bị-bằng-chứng-cho-slide-10)

---

## 1. Branch strategy

```
main       ← release cuối (chỉ merge từ develop qua PR; luôn ở trạng thái demo được)
  ▲
develop    ← nhánh tích hợp (mọi feature merge vào đây)
  ▲
feature/*  ← nhánh cá nhân, tách từ develop
```

| Nhánh | Ai được push | Merge vào |
|---|---|---|
| `main` | Không ai push trực tiếp | ← `develop` (PR, cuối dự án) |
| `develop` | Không ai push trực tiếp | ← `feature/*` (PR) |
| `feature/{tên}` | Chủ nhánh | → `develop` qua PR |

**Đặt tên feature branch** (khớp phân công 4 người):
```
feature/auth-jwt          (A — auth, member, common, config)
feature/request-crud      (B — request CRUD, filter, stats)
feature/workflow-logic    (C — assign, status, history)
feature/alert-llm         (D — alert, llm, swagger, CI)
```

Vòng đời một nhánh:
```bash
git checkout develop && git pull
git checkout -b feature/request-crud
# ... code + commit ...
git push -u origin feature/request-crud
# → mở PR trên GitHub vào develop
```

## 2. Commit convention

Format: `type: mô tả ngắn (tiếng Anh hoặc Việt, thì hiện tại)`

| type | Dùng khi | Ví dụ |
|---|---|---|
| `feat` | Thêm tính năng | `feat: add auto-assign algorithm` |
| `fix` | Sửa lỗi | `fix: block PENDING to DONE transition` |
| `docs` | Tài liệu | `docs: update ERD relationships` |
| `refactor` | Tái cấu trúc, không đổi hành vi | `refactor: extract HistoryService` |
| `test` | Thêm/sửa test | `test: cover invalid status transition` |
| `chore` | Cấu hình, build, CI | `chore: add docker-compose` |

**Quy tắc:**
- Mỗi commit là **một thay đổi logic** (không commit "misc", "update", "fix bug" trống rỗng — đây chính là "meaningless commits" bị trừ điểm).
- Dòng đầu ≤ 72 ký tự, mô tả **làm gì**, không phải "đã sửa file X".
- Commit thường xuyên, nhỏ → dễ review, dễ revert.

## 3. Quy trình Pull Request

1. Push feature branch → mở PR vào `develop`.
2. Điền **PR template** (§6): mô tả thay đổi + checklist.
3. **Bắt buộc ≥1 review** của thành viên khác mới được merge (đề yêu cầu).
4. Reviewer kiểm: logic đúng? có test? có Swagger? theo convention?
5. CI phải **xanh** (build + test pass) mới merge được.
6. Merge → xoá feature branch.

> **Ai review ai** (gợi ý cho 4 người): A↔B, C↔D chéo nhau; phần logic nặng (C) nên có 2 người xem. Không tự approve PR của chính mình.

## 4. Branch protection

Cấu hình trên GitHub (`Settings → Branches → Add rule`) cho `main` **và** `develop`:
- ☑ Require a pull request before merging
- ☑ Require approvals: **1**
- ☑ Require status checks to pass (chọn status check `Backend CI / verify`)
- ☑ Do not allow bypassing the above settings
- ☑ Restrict who can push (không ai push trực tiếp)

> **Vì sao chặn cả `develop`?** Để không ai lỡ tay push thẳng, buộc mọi thay đổi qua PR + CI → đúng tinh thần "thực thi thật" mà rubric đòi hỏi. Chụp màn hình rule này đưa vào slide 10.

## 5. GitHub Actions CI

`.github/workflows/backend-ci.yml` — chạy Maven Wrapper trong `BE/` trên **mỗi PR** vào `develop`/`main`:

```yaml
name: Backend CI
on:
  pull_request:
    branches: [main, develop]
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build & test
        working-directory: BE
        run: ./mvnw -B verify
```

> Unit test không cần DB; integration test dùng **Testcontainers PostgreSQL** nên GitHub Actions phải có Docker (ubuntu-latest đáp ứng). Nhờ vậy Flyway, PostgreSQL SQL và transaction được kiểm chứng trên đúng dialect production, không có false confidence do H2. PR fail test hiện **đỏ** ngay → không merge được.

## 6. PR template

`.github/pull_request_template.md`:

```markdown
## Thay đổi gì
- 

## Loại thay đổi
- [ ] feat  - [ ] fix  - [ ] refactor  - [ ] docs  - [ ] test  - [ ] chore

## Checklist
- [ ] Đã test luồng chính (có unit/integration test)
- [ ] Đã cập nhật Swagger annotation (nếu đổi API)
- [ ] Trả đúng response envelope + HTTP status
- [ ] Không có breaking change (hoặc đã ghi rõ bên dưới)
- [ ] CI xanh

## Ghi chú cho reviewer
- 
```

## 7. Chống xung đột merge (cho nhóm 4 người)

| Rủi ro | Phòng ngừa |
|---|---|
| Sửa cùng file | **Package-by-feature**: mỗi người 1 package (xem [ARCHITECTURE.md](./ARCHITECTURE.md#5-c%E1%BA%A5u-tr%C3%BAc-package-package-by-feature)) |
| Xung đột file dùng chung (`ApiResponse`, `SecurityConfig`, `pom.xml`) | **A dựng nền & merge trước** ở Phase 1; người khác `git pull develop` trước khi code |
| API contract lệch nhau | **Chốt [openapi.yaml](./openapi.yaml) ở Phase 0** trước khi ai code |
| Develop bị "trôi" xa feature | Rebase/merge `develop` vào feature branch mỗi ngày |
| Merge dồn phút chót | Merge sớm & nhỏ, không dồn 1 PR khổng lồ cuối dự án |

**Quy tắc vàng:** trước khi bắt đầu ngày làm việc → `git checkout develop && git pull && git checkout feature/xxx && git merge develop`.

## 8. Chuẩn bị bằng chứng cho slide 10

Rubric chấm "làm thật", nên **chụp lại bằng chứng**:
- [ ] Ảnh **network graph** các nhánh (Insights → Network) — thấy feature branch tách & merge.
- [ ] Danh sách **PR đã merge** (kèm review approval).
- [ ] Ảnh **branch protection rule**.
- [ ] Ảnh **CI xanh** trên một PR (và 1 PR từng đỏ rồi sửa xanh — thể hiện CI hoạt động thật).
- [ ] Một **vấn đề collaboration đã gặp** + cách giải quyết (vd conflict ở `pom.xml`, cách xử lý) — slide này rubric thích vì cho thấy làm việc thật.

**Câu chốt slide 10:**
> *"Chúng em không push thẳng main: mọi thay đổi qua feature branch → PR → review ≥1 người → CI xanh mới merge. Branch protection ép đúng quy trình này. Nhờ package-by-feature và chốt API contract từ đầu, 4 người làm song song mà gần như không đụng độ merge."*

---

*Phân công thành viên chi tiết: [kế hoạch tổng thể §10](../Bzcom_CRM_Ke_Hoach_Thiet_Ke.md#10-phân-công-nhóm-4-người).*
