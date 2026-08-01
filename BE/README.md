# Bzcom CRM Backend

Backend Spring Boot của Bzcom CRM. HTTP contract chuẩn nằm tại
[`../docs/openapi.yaml`](../docs/openapi.yaml); quy tắc triển khai nằm tại
[`../docs/BACKEND_CODING_RULES.md`](../docs/BACKEND_CODING_RULES.md).

## Yêu cầu

- Java 21
- Docker Desktop (bắt buộc cho PostgreSQL local và integration test)

Không cần cài Maven hoặc PostgreSQL trực tiếp trên máy.

## Chạy local

Tại thư mục `BE/`:

```bash
cp .env.example .env
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

PostgreSQL container được publish ở `localhost:5433` để tránh xung đột với PostgreSQL
cài trực tiếp thường dùng cổng `5432`. Có thể đổi bằng `POSTGRES_HOST_PORT` trong `.env`.

Swagger UI: <http://localhost:8080/swagger-ui.html>
Health check: <http://localhost:8080/actuator/health>

Profile `dev` và `docker` nạp seed demo; migration schema dùng chung không chứa tài khoản
mẫu. Profile `prod` bắt buộc nhận `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
từ environment và chỉ chạy migration schema. Mật khẩu của bốn tài khoản demo trong tài
liệu là `1234`.

## Quality gate

```bash
./mvnw spotless:apply  # chạy trước khi commit nếu formatter báo lỗi
./mvnw -B verify       # compile + unit + integration + Spotless + JaCoCo
```

Integration test dùng PostgreSQL 16 qua Testcontainers, vì vậy Docker daemon phải đang
chạy. Báo cáo coverage được tạo tại `target/site/jacoco/index.html`.

## Quy tắc migration

- File trong `src/main/resources/db/migration/` chạy ở mọi môi trường và không được sửa
  sau khi đã merge/deploy. Thay đổi schema bằng migration version mới.
- File trong `src/main/resources/db/demo/` chỉ chạy với profile `dev`/`docker`.
- JPA dùng `ddl-auto=validate`; entity không được tự ý thay schema.
