## Task và phạm vi

- Task: <!-- Ví dụ: T-2.B2 -->
- Refs: <!-- Link section trong docs/TASKS.md và canonical docs liên quan -->

## Thay đổi

-

## Đồng bộ contract/docs

- [ ] Không thay đổi contract/docs
- [ ] Đã cập nhật các nguồn liên quan: <!-- openapi.yaml, ERD, business logic, ... -->
- [ ] Đã kiểm tra link task ↔ docs; relative link và anchor còn hợp lệ
- Breaking change/migration note: <!-- Không có, hoặc mô tả rõ tác động/thứ tự rollout -->

## Kiểm chứng

- [ ] Backend: `cd BE && ./mvnw -B verify` (nếu liên quan)
- [ ] Frontend: `cd FE && npm run verify` (nếu liên quan)
- [ ] Đã kiểm tra luồng chính
- [ ] Đã kiểm tra luồng lỗi/quyền truy cập liên quan

## Contract checklist

- [ ] Không lệch `docs/openapi.yaml`
- [ ] Response dùng `ApiResponse`/`PageResponse`
- [ ] Không trả entity hoặc dữ liệu nhạy cảm
- [ ] Migration mới là append-only; không sửa migration đã merge
- [ ] Không commit secret
- [ ] Chỉ tick task hoàn thành khi đạt toàn bộ DoD
