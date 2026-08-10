# Bzcom CRM Frontend

React SPA cho Bzcom CRM, đặt trực tiếp trong `FE/`. API contract chuẩn nằm tại
[`../docs/openapi.yaml`](../docs/openapi.yaml); thiết kế và quyết định frontend nằm tại
[`../docs/FRONTEND.md`](../docs/FRONTEND.md).

## Yêu cầu

- Node.js 22 (`.nvmrc`; yêu cầu `>=22.12 <23`)
- Backend Bzcom CRM ở `http://localhost:8080` khi cần Auth/Member thật

## Chạy local

```bash
cd FE
cp .env.example .env.local
npm ci
npm run dev
```

Mở <http://localhost:5173>. Backend dev đã cho phép origin này qua CORS.

### Chạy toàn bộ bằng Docker

Từ thư mục gốc repository:

```bash
cp .env.example .env       # tùy chọn; dùng được default nếu bỏ qua
docker compose up --build
```

Mở <http://localhost:5173>. Nginx phục vụ SPA và reverse proxy `/api` tới backend trong
Docker network, vì vậy browser không phụ thuộc hostname nội bộ và không gặp lỗi CORS.
Swagger vẫn truy cập trực tiếp tại <http://localhost:8080/swagger-ui.html>.

```bash
docker compose ps          # cả db/backend/frontend phải healthy
docker compose down        # giữ volume PostgreSQL
```

Không dùng `docker compose down -v` trừ khi muốn xóa dữ liệu và seed lại từ đầu.

Biến môi trường:

```text
VITE_API_BASE_URL=http://localhost:8080/api
VITE_REQUEST_DATA_MODE=api
```

- `api` (**mặc định**): Request List/Detail/History gọi backend thật. Không fallback âm thầm
  sang mock khi backend lỗi.
- `demo`: chỉ bật rõ ràng khi làm UI cô lập; Auth/Member vẫn gọi backend thật, còn Request
  List/Detail dùng dữ liệu deterministic có nhãn `Demo data`. Demo mô phỏng phạm vi role:
  ADMIN thấy tất cả, DEVELOPER thấy việc của developer seed, CLIENT thấy request của client seed.

## Quality gate

```bash
npm run verify
```

Lệnh trên chạy lint, TypeScript strict check, unit/component tests và production build.
CI tương ứng là `Frontend CI / verify`.

### Dependency audit note

Ngày 02/08/2026, `npm audit` báo advisory high `GHSA-qwww-vcr4-c8h2` cho React Router
7.18.2. Advisory này chỉ áp dụng cho **RSC Mode action execution**; ứng dụng này là Vite
declarative SPA, không bật RSC, SSR, server action hoặc React Router framework mode. Bản
7.11.0 mà npm gợi ý hạ xuống có nhiều advisory XSS/DoS phạm vi rộng hơn, nên dự án giữ
7.18.2 và cần nâng lên bản vá mới ngay khi upstream phát hành. Không chạy
`npm audit fix --force` để downgrade mù quáng.

## Tài khoản demo backend

Mật khẩu demo là `1234`:

- `admin@bzcom.com`
- `dev1@bzcom.com`
- `dev2@bzcom.com`
- `client1@bzcom.com`

## Cấu trúc

```text
src/
├── app/                  # router, query client, app error boundary
├── auth/                 # session, context, role/protected guards
├── components/           # layout, tags, states dùng chung
├── config/               # runtime env
├── features/
│   ├── auth/
│   ├── members/
│   └── requests/
├── lib/                  # Axios clients, envelope/error handling
├── styles/
├── test/
├── types/                # OpenAPI-aligned types
└── utils/
```

Page không gọi Axios trực tiếp: `Page → query hook → API function → apiClient`.

## Handoff các phần FE còn lại

- Request read flow hiện đã chạy ở `api` mode theo mặc định và đã nối list/filter/page/detail/history.
- Request detail đã nối assign/status/history/AI summary; mọi workflow mutation gửi
  `expectedVersion` từ response mới nhất và invalidate cache liên quan.
- Request create đã nối AI category/priority suggestion và được giới hạn cho CLIENT.
- Alert bell và `/alerts` đã nối polling, filter, mark-read và request deep-link.
- ADMIN dashboard `/stats` đã nối KPI, category donut và developer workload bars.
- Member D thêm Alert/LLM features và query invalidation liên quan.
- Mọi API/type change phải bám `docs/openapi.yaml` và có test trong cùng PR.

Không commit `.env.local`, token, API key, `node_modules`, `dist` hoặc `coverage`.
