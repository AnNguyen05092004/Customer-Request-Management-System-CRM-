## Mục tiêu

<!-- Task ID và kết quả cần đạt, ví dụ T-2.B2 -->

## Thay đổi

-

## Cách kiểm thử

- [ ] `./mvnw -B verify`
- [ ] Luồng chính
- [ ] Luồng lỗi/quyền truy cập liên quan

## Contract checklist

- [ ] Không lệch `docs/openapi.yaml`
- [ ] Response dùng `ApiResponse`/`PageResponse`
- [ ] Không trả entity hoặc dữ liệu nhạy cảm
- [ ] Migration mới là append-only; không sửa migration đã merge
- [ ] Không commit secret
