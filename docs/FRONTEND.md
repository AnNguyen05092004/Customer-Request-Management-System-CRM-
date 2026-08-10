# FRONTEND — Thiết kế React SPA

> Thiết kế frontend cho **Bzcom CRM**: một Single Page Application React phủ **toàn bộ** REST API của backend, dùng làm lớp trình diễn (demo) trực quan bên cạnh Swagger UI.
>
> **Lưu ý về rubric:** đề bài chấm backend; frontend **không có tiêu chí rubric riêng**. FE là **lớp giá trị gia tăng** (demo ấn tượng ở slide 9, thể hiện năng lực full-stack). Nguyên tắc: **backend + Swagger là xương sống đảm bảo điểm; chỉ làm FE sau khi backend chạy ổn**, và không để FE lấn giờ backend.

---

## Mục lục
1. [Phạm vi & bản đồ tính năng](#1-phạm-vi--bản-đồ-tính-năng)
2. [Tech stack & lý do](#2-tech-stack--lý-do)
3. [Cấu trúc thư mục](#3-cấu-trúc-thư-mục)
4. [Kiến trúc tầng FE](#4-kiến-trúc-tầng-fe)
5. [TypeScript types (khớp DTO backend)](#5-typescript-types-khớp-dto-backend)
6. [Tầng API client (axios + envelope)](#6-tầng-api-client-axios--envelope)
7. [Xác thực & phân quyền (JWT)](#7-xác-thực--phân-quyền-jwt)
8. [Data fetching với TanStack Query](#8-data-fetching-với-tanstack-query)
9. [Routing & bản đồ trang](#9-routing--bản-đồ-trang)
10. [Đặc tả từng trang](#10-đặc-tả-từng-trang)
11. [Xử lý lỗi & thông báo](#11-xử-lý-lỗi--thông-báo)
12. [Cấu hình, chạy & Docker](#12-cấu-hình-chạy--docker)
13. [Phân công & thứ tự làm](#13-phân-công--thứ-tự-làm)

---

## 1. Phạm vi & bản đồ tính năng

Phủ toàn bộ endpoint trong [openapi.yaml](./openapi.yaml). UI hiển thị **theo role** (ẩn/hiện menu & nút theo quyền — khớp [ma trận phân quyền](./OPENAPI.md#8-ma-trận-phân-quyền)).

| Nhóm | Màn hình | Endpoint dùng | Role thấy |
|---|---|---|---|
| Auth | Login/refresh/logout | `POST /auth/login`, `/auth/refresh`, `/auth/logout` | tất cả |
| Member | Đăng ký; danh sách member; chi tiết | `POST/GET /members`, `GET /members/{id}` | đăng ký: public · list: ADMIN |
| Request | Danh sách (filter/sort/paging); chi tiết; tạo; gán; đổi status; history | toàn bộ `/requests/*` | theo role |
| Stats | Dashboard thống kê + biểu đồ | `GET /requests/stats` | ADMIN |
| Alert | Chuông thông báo + danh sách + đánh dấu đọc | `GET /alerts`, `PATCH /alerts/{id}/read` | tất cả |
| LLM | Nút "AI phân loại / gợi ý priority" trong form; "AI tóm tắt" ở chi tiết | `/requests/classify`, `/suggest-priority`, `/{id}/summary` | classify/priority: authenticated; summary: quyền đọc request |

## 2. Tech stack & lý do

| Lớp | Công nghệ | Lý do |
|---|---|---|
| Runtime | **Node.js 22 LTS** | Khóa bằng `.nvmrc` và `engines` để local/CI giống nhau |
| Build tool | **Vite 7** | Ổn định trên Node 22, dev server nhanh, cấu hình tối giản |
| Framework | **React 19** | Bản stable hiện hành, component model phổ biến |
| Ngôn ngữ | **TypeScript 5.9 strict** | Type-safe khi khớp DTO backend; dùng dòng ổn định tương thích tooling |
| Data fetching | **TanStack Query (React Query) v5** | Cache, loading/error state, invalidation sau mutation — bỏ hẳn boilerplate `useState/useEffect` |
| HTTP | **axios** | Interceptor gắn JWT + xử lý 401 tập trung |
| Routing | **React Router v7** | Declarative nested route + protected route theo role |
| UI kit | **Ant Design v5** | Sẵn Table (paging/sort/filter), Form (validation), Tag, Badge, message — hợp CRM, lên UI rất nhanh |
| Biểu đồ | **Ant Design Charts** (hoặc Recharts) | Vẽ dashboard stats |
| Form | **Ant Design Form** + rule | Validation client khớp validation backend |

> **Vì sao Ant Design (không Tailwind/shadcn)?** CRM = nhiều bảng + form + filter. Ant Design cho sẵn `Table` với phân trang/sắp xếp/lọc server-side, `Form` với validation → **tiết kiệm hàng giờ** so với tự ghép component. Đây là lựa chọn tối ưu thời gian cho bài OJT.

> **Vì sao React Query?** Backend đã có sẵn envelope + phân trang; React Query lo cache + refetch + invalidate (vd sau khi đổi status → tự refetch danh sách). Không cần Redux.

## 3. Cấu trúc thư mục

Feature-based, song song với package-by-feature của backend (dễ đối chiếu, mỗi người sở hữu 1 feature):

```
FE/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
├── .env.example              # VITE_API_BASE_URL=http://localhost:8080/api
├── .nvmrc                    # Node 22
├── ui_design_specification/  # ảnh/code thiết kế tham chiếu, không phải runtime source
└── src/
    ├── main.tsx              # mount providers
    ├── app/                  # router + queryClient + error boundary
    ├── config/               # runtime env đã validate
    ├── lib/
    │   ├── apiClient.ts      # axios instances + refresh/retry + unwrap envelope
    │   └── apiError.ts       # chuẩn hóa lỗi transport/backend
    ├── types/
    │   └── api.ts            # types khớp DTO backend (§5)
    ├── auth/
    │   ├── AuthContext.tsx   # lưu token + user role, login/logout
    │   ├── session.ts        # validated single-key local session store
    │   ├── useAuth.ts
    │   └── RouteGuards.tsx   # guard đăng nhập + role
    ├── components/
    │   ├── AppLayout.tsx     # sidebar/menu theo role + header responsive
    │   ├── PageHeader.tsx
    │   ├── PageStates.tsx
    │   └── ResourceTags.tsx  # Tag màu theo status/priority/category
    ├── features/
    │   ├── auth/
    │   │   ├── api.ts
    │   │   └── LoginPage.tsx
    │   ├── members/
    │   │   ├── api.ts        # hàm gọi HTTP
    │   │   ├── queries.ts    # hooks React Query
    │   │   └── MemberListPage.tsx, MemberDetailPage.tsx, RegisterPage.tsx
    │   ├── requests/
    │   │   ├── api.ts, queries.ts, demoData.ts
    │   │   └── RequestListPage.tsx, RequestDetailPage.tsx
    ├── test/                 # Vitest setup/helpers
    └── utils/
        └── format.ts         # format ngày, enum → nhãn tiếng Việt
```

Request API có hai mode rõ ràng: `VITE_REQUEST_DATA_MODE=api` là mặc định và gọi backend thật;
`demo` chỉ bật rõ ràng khi phát triển UI cô lập, dùng dữ liệu deterministic và luôn hiện nhãn
**Demo data**. Không fallback âm thầm từ API sang mock vì điều đó có thể che lỗi contract.

## 4. Kiến trúc tầng FE

```
┌─────────────────────────────────────────────────┐
│  Pages (features/*/*Page.tsx)                     │  UI + gọi hooks
├─────────────────────────────────────────────────┤
│  Hooks React Query (features/*/api.ts)            │  useQuery/useMutation + cache
├─────────────────────────────────────────────────┤
│  API functions (features/*/api.ts)                │  gọi apiClient, trả data đã unwrap
├─────────────────────────────────────────────────┤
│  apiClient (axios)                                │  gắn JWT, unwrap {status,message,data},
│                                                   │  xử lý 401/403 tập trung
└─────────────────────────────────────────────────┘
   Cross-cutting: AuthContext (token+role) · AppLayout (menu theo role) · antd message (toast lỗi)
```

**Luật:** Page không gọi axios trực tiếp → luôn qua hook. Hook gọi API function. API function là nơi duy nhất biết đường dẫn endpoint → đổi API chỉ sửa 1 chỗ.

## 5. TypeScript types (khớp DTO backend)

`src/types/api.ts` — khớp 1-1 với schema trong [openapi.yaml](./openapi.yaml):

```typescript
export type Role = 'ADMIN' | 'DEVELOPER' | 'CLIENT';
export type Category = 'BUG' | 'FEATURE' | 'INQUIRY';
export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';
export type RequestStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';
export type AlertType = 'ASSIGNED' | 'STATUS_CHANGED' | 'HIGH_PRIORITY_REGISTERED';

// Envelope thống nhất từ backend
export interface ApiResponse<T> { status: number; message: string; data: T; }

export interface PageResult<T> {
  content: T[]; page: number; size: number; totalElements: number; totalPages: number;
}

export interface TokenResponse {
  accessToken: string; refreshToken: string; tokenType: string; role: Role;
}

export interface MemberResponse { id: number; email: string; name: string; role: Role; createdAt: string; }

export interface RequestResponse {
  id: number; title: string; description: string | null;
  category: Category; priority: Priority; status: RequestStatus;
  clientId: number; assignedDeveloperId: number | null; version: number;
  createdAt: string; updatedAt: string;
}

export interface RequestCreateRequest {
  title: string; description?: string; category: Category; priority: Priority;
}

export type AssignRequest =
  | { auto: true; expectedVersion: number }
  | { auto: false; developerId: number; expectedVersion: number };

export interface HistoryResponse {
  id: number; requestId: number; changedBy: number;
  fromStatus: RequestStatus | null; toStatus: RequestStatus | null;
  memo: string | null; changedAt: string;
}

export interface AlertResponse {
  id: number; requestId: number; alertType: AlertType;
  message: string; isRead: boolean; createdAt: string;
}

export interface StatsResponse {
  total: number; completed: number; completionRate: number;
  byCategory: Record<Category, number>;
  byDeveloper: { developerId: number; developerName: string; assignedCount: number; doneCount: number }[];
}

export interface ClassifyResult { category: Category; confidence: number; reason: string; }
```

> Nếu muốn "auto-sync" tuyệt đối, có thể sinh types từ `openapi.yaml` bằng `openapi-typescript`. Với phạm vi OJT, viết tay như trên là đủ và nhanh.

## 6. Tầng API client (axios + envelope)

`src/lib/apiClient.ts`:

```typescript
export const publicApiClient = axios.create({ baseURL: env.apiBaseUrl, timeout: 10_000 });
export const apiClient = axios.create({ baseURL: env.apiBaseUrl, timeout: 10_000 });
let refreshInFlight: Promise<TokenResponse> | null = null;

apiClient.interceptors.request.use((config) => {
  const session = sessionStore.read();
  if (session) config.headers.Authorization = `${session.tokenType} ${session.accessToken}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as RetryableRequestConfig | undefined;
    if (error.response?.status === 401 && original && !original._retry && sessionStore.read()) {
      original._retry = true;
      try {
        refreshInFlight ??= refreshTokens();
        const tokens = await refreshInFlight;
        original.headers.Authorization = `${tokens.tokenType} ${tokens.accessToken}`;
        return apiClient(original);
      } catch (refreshError) {
        sessionStore.clear();
        notifySessionExpired();
        return Promise.reject(toApiError(refreshError));
      } finally {
        refreshInFlight = null;
      }
    }
    return Promise.reject(toApiError(error));
  },
);
```

Ví dụ API function (`features/requests/api.ts`):

```typescript
import { apiClient, unwrap } from '../../lib/apiClient';
import type { ApiResponse, PageResponse, RequestResponse } from '../../types/api';

export interface RequestFilter {
  page: number; size: number; sort: string;
  status?: string; category?: string; priority?: string; keyword?: string;
}

export function fetchRequests(filter: RequestFilter, signal?: AbortSignal) {
  if (env.isRequestDemoMode) return fetchDemoRequests(filter, signal);
  return unwrap(apiClient.get<ApiResponse<PageResponse<RequestResponse>>>('/requests', {
    params: filter, signal,
  }));
}
```

## 7. Xác thực & phân quyền (JWT)

**Lưu token:** một object session duy nhất dưới key `bzcom.crm.session` trong `localStorage`,
đọc/validate qua `auth/session.ts`; feature không truy cập raw key. Cách này đủ cho demo OJT
(production nên dùng httpOnly cookie để giảm rủi ro XSS). Khi access token nhận 401,
interceptor chỉ gọi một lần `POST /auth/refresh` cho mọi request đang chờ, thay nguyên tử cả
cặp token rồi retry đúng một lần. Refresh fail thì xóa session và AuthContext đưa người dùng
về login. Auth endpoints dùng `publicApiClient`, không gửi Bearer access token cũ.

`AuthContext` giữ `{ isAuthenticated, role, login(), logout() }`:

```typescript
const login = async (request: LoginRequest) => {
  const tokens = await loginRequest(request); // publicApiClient
  setSession(sessionStore.write(tokens));
};
const logout = async () => {
  const current = sessionStore.read();
  try { if (current) await logoutRequest(current.refreshToken); }
  catch { /* vẫn đăng xuất local nếu mạng/backend lỗi */ }
  finally { sessionStore.clear(); setSession(null); }
};
```

**Guard đăng nhập và role:**

```typescript
// auth/RouteGuards.tsx
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Outlet />;
}
export function RoleRoute({ allow }: { allow: Role[] }) {
  const { role } = useAuth();
  return role && allow.includes(role) ? <Outlet /> : <Navigate to="/403" replace />;
}
```

> FE guard chỉ để **UX** (ẩn nút/trang không được phép). Bảo mật thật vẫn ở backend (401/403). Nói rõ điều này = hiểu đúng vai trò client-side auth.

## 8. Data fetching với TanStack Query

**Query key** đặt theo tài nguyên + tham số → tự cache & refetch đúng:

```typescript
// features/requests/queries.ts
export const useRequests = (filter: RequestFilter) =>
  useQuery({ queryKey: ['requests', filter], queryFn: () => fetchRequests(filter) });

export const useRequest = (id: number) =>
  useQuery({ queryKey: ['request', id], queryFn: () => fetchRequest(id) });

export const useUpdateStatus = (id: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { status: string; memo?: string; expectedVersion: number }) => updateStatus(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['request', id] });     // refetch chi tiết
      qc.invalidateQueries({ queryKey: ['requests'] });        // refetch danh sách
      qc.invalidateQueries({ queryKey: ['alerts'] });          // status changed → có alert mới
      message.success('Cập nhật trạng thái thành công');
    },
  });
};
```

> **Điểm hay:** sau khi đổi status, invalidate `['alerts']` → chuông tự cập nhật (vì backend sinh alert STATUS_CHANGED). Thể hiện luồng tự động hoá của backend một cách trực quan.

## 9. Routing & bản đồ trang

Đây là bản đồ route của frontend. Request read APIs và workflow detail (assign/status/history/
AI summary) đã được nối; create request, alerts và statistics là các lát cắt tiếp theo và phải
bám contract đã được xác nhận.

```typescript
// App.tsx
<Routes>
  <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
  <Route path="/register" element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />
  <Route element={<ProtectedRoute />}>
    <Route element={<AppLayout />}>
      <Route index element={<Navigate to="/requests" />} />
      <Route path="/requests" element={<RequestListPage />} />
      <Route path="/requests/:id" element={<RequestDetailPage />} />
      <Route element={<RoleRoute allow={['ADMIN']} />}>
        <Route path="/members" element={<MemberListPage />} />
        <Route path="/members/:id" element={<MemberDetailPage />} />
      </Route>
      <Route path="/403" element={<ForbiddenPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Route>
  </Route>
</Routes>
```

**Menu sidebar theo role** (trong `AppLayout`):
| Menu item | ADMIN | DEVELOPER | CLIENT |
|---|:---:|:---:|:---:|
| Requests | ✅ | ✅ | ✅ |
| Tạo request | | | ✅ |
| Members | ✅ | | |
| Thống kê | ✅ | | |
| Alerts (chuông) | ✅ | ✅ | ✅ |

## 10. Đặc tả từng trang

### LoginPage
Form email + password → `login()`. Thành công → điều hướng `/requests`. Lỗi 401 → hiện `message.error` với message từ backend.

### RequestListPage (trang trung tâm)
- `Table` Ant Design: cột Title, Category (Tag), Priority (Tag màu), Status (StatusTag), AssignedDeveloper, CreatedAt, Action.
- **Filter bar:** Select status/category/priority + Input keyword → đổi filter → refetch (server-side).
- **Phân trang & sort server-side:** `Table` `pagination` + `onChange` → map sang `page/size/sort` gọi backend (không sort client-side vì dữ liệu phân trang).
- Backend đã lọc theo role → FE không cần lọc thêm, chỉ hiển thị.
- Nút "Tạo request" chỉ hiện với CLIENT.

### RequestDetailPage
- Hiển thị đầy đủ field + timeline **history** (`GET /{id}/history`) dạng `Timeline` Ant Design.
- **ADMIN:** nút "Gán developer" → Modal chọn auto (switch) hoặc chọn dev từ Select → `assignRequest`.
- **ADMIN/DEVELOPER (được gán):** nút "Đổi trạng thái" → chỉ hiện các status **hợp lệ kế tiếp** (client đọc state machine: PENDING→[IN_PROGRESS], IN_PROGRESS→[DONE], DONE→[]) để tránh gửi request chắc chắn 409. Nếu vẫn 409 (đồng thời) → hiện message.
- Nút "AI tóm tắt" → `GET /{id}/summary` → hiển thị kết quả; backend vẫn kiểm quyền đọc request.

### RequestCreatePage (CLIENT)
- Form: title, description, category, priority (validation khớp backend: title bắt buộc...).
- Nút **"AI gợi ý"**: gọi `POST /classify` + `/suggest-priority` với description → tự điền category/priority (kèm hiển thị confidence + reason) → **người dùng xác nhận** trước khi submit (đúng nguyên tắc "LLM chỉ gợi ý", [BR-13](./ANALYZE.md#7-business-rules-br)). Mọi lệnh assign/status gửi `expectedVersion` từ `RequestResponse.version`; 409 thì refetch detail và yêu cầu người dùng xác nhận lại.

### StatsDashboardPage (ADMIN)
- Card số: total, completed, completionRate (%).
- Pie chart theo category; Bar chart theo developer (assignedCount vs doneCount).
- Đây là màn "wow" cho slide demo.

### MemberListPage / MemberDetailPage (ADMIN)
- Table member; RegisterPage công khai để tạo member (test đủ role cho demo).

### AlertBell / AlertListPage
- `AlertBell` ở header: `Badge` đếm alert `isRead=false` (poll `GET /alerts?isRead=false` mỗi ~15s hoặc refetch sau mutation). Popover liệt kê alert; click → đánh dấu đọc (`PATCH /{id}/read`) + điều hướng tới request liên quan.

## 11. Xử lý lỗi & thông báo

- Lỗi mạng/nghiệp vụ: interceptor reject với `message` từ envelope backend → hook bắt và `message.error(err.message)` (Ant Design toast). **Không** hiện lỗi kỹ thuật thô.
- 401 → tự về `/login`. 403 → trang `/403` hoặc toast "Không đủ quyền".
- Loading: React Query `isLoading` → `Skeleton`/`Spin`. Empty: `Empty` của Ant Design.
- Validation: Form rule client (bắt buộc, độ dài) chặn trước; backend vẫn là nguồn chân lý (400).

## 12. Cấu hình, chạy & Docker

`.env.example`:
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_REQUEST_DATA_MODE=api
```

Chạy dev:
```bash
cd FE
npm ci
npm run dev            # http://localhost:5173; gọi trực tiếp backend :8080 qua CORS
```

Quality gate local/CI:

```bash
npm run verify         # lint + typecheck + unit/component test + production build
```

**CORS:** backend hiện đã cho phép origin `http://localhost:5173` (dev), các method
GET/POST/PATCH/DELETE và header `Authorization` trong `SecurityConfig`.

**Dockerfile (multi-stage → Nginx):** build-time dùng `VITE_API_BASE_URL=/api`; Nginx phục
vụ SPA và reverse proxy `/api` tới service `backend`, nên bản Docker dùng same-origin và
không phụ thuộc CORS của browser.

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=/api
ARG VITE_REQUEST_DATA_MODE=api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_REQUEST_DATA_MODE=${VITE_REQUEST_DATA_MODE}
RUN npm run build
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
```
`nginx.conf`: SPA fallback (`try_files $uri $uri/ /index.html`), cache immutable cho asset
đã hash và `location /api/ { proxy_pass http://backend:8080; }`.

**Root `compose.yaml`** (xem [ARCHITECTURE.md §10](./ARCHITECTURE.md#10-kiến-trúc-triển-khai-docker)):
```yaml
  frontend:
    build:
      context: ./FE
      args:
        VITE_API_BASE_URL: /api
        VITE_REQUEST_DATA_MODE: ${VITE_REQUEST_DATA_MODE:-api}
    ports: ["5173:80"]
    depends_on:
      backend:
        condition: service_healthy
```
→ `docker compose up --build` chạy cả 3: frontend + app + db và FE gọi API thật theo mặc định.

## 13. Phân công & thứ tự làm

> **Nguyên tắc:** FE chỉ bắt đầu khi API contract ([openapi.yaml](./openapi.yaml)) đã chốt (Phase 0) và backend có endpoint chạy được (dù mock). Ưu tiên đúng thứ tự để không chặn nhau.

| Thứ tự | Việc | Phụ thuộc |
|---|---|---|
| 1 | Setup Vite + TS + antd + router + React Query + apiClient + AuthContext | — (làm được sớm; Request dùng demo mode có nhãn) |
| 2 | Login + ProtectedRoute/RoleRoute + AppLayout (menu theo role) | #1 + `/auth/login` backend |
| 3 | RequestListPage (Table + filter + paging) | `/requests` backend |
| 4 | RequestDetailPage (history + assign + status) | `/requests/{id}`, `/assign`, `/status`, `/history` |
| 5 | RequestCreatePage + nút AI gợi ý | `/requests`, `/classify` |
| 6 | AlertBell + AlertListPage | `/alerts` |
| 7 | StatsDashboardPage (charts) | `/requests/stats` |
| 8 | Member pages + Register | `/members` |

**Gợi ý người làm:** vì backend đã chia A/B/C/D, FE có thể do **1–2 người phụ trách sau khi phần backend của họ xong** (thường A hoặc D rảnh sớm), hoặc mỗi người làm trang FE ứng với feature backend mình đã làm (dễ vì hiểu rõ API đó nhất). Ghi rõ đóng góp FE ở slide 11.

**Rủi ro cần nhớ:** nếu quỹ thời gian eo hẹp → cắt theo thứ tự ngược (#8 → #7 → ...), giữ tối thiểu #1–#5 để demo trọn luồng chính. **Tuyệt đối không hi sinh chất lượng backend để làm FE** — backend mới là thứ được chấm.

### 13.1 Branch triển khai và bằng chứng Git Flow

Các lát cắt UI lớn được phát triển tuần tự từ `develop`, merge bằng PR và **giữ branch trên
remote sau merge** để có thể trình bày network graph/lịch sử phát triển với doanh nghiệp:

| Thứ tự | Branch | Task/phạm vi | Quality gate trước PR |
|---|---|---|---|
| 1 | `feature/request-workflow-ui` | T-5.3/T-5.4: list acceptance, assign, status, history, AI summary | FE verify + workflow smoke |
| 2 | `feature/request-create-ai-ui` | T-5.5: form CLIENT + classify/priority suggestion | FE verify + create smoke |
| 3 | `feature/alerts-ui` | T-5.6: badge, polling, popover/list, mark-read/deep-link | FE verify + alert smoke |
| 4 | `feature/stats-dashboard-ui` | T-5.7: KPI/charts ADMIN | FE verify + stats smoke |
| 5 | `test/frontend-acceptance` | role/edge case/a11y/responsive, docs/task sync | FE verify + Docker full-stack smoke |

Không tái sử dụng branch đã merge cho lát cắt mới. Nếu repository bật tự động xóa head
branch sau merge, leader tạo lại branch cùng tên tại merge commit hoặc tắt tùy chọn đó trước
khi merge các PR UI.

**Dependency security note (02/08/2026):** React Router 7.18.2 hiện bị `npm audit` gắn
advisory high `GHSA-qwww-vcr4-c8h2`, nhưng advisory chỉ áp dụng RSC Mode; FE này là Vite
declarative SPA, không dùng RSC/SSR/server actions/framework mode. Không downgrade về
7.11.0 vì bản đó có nhiều advisory XSS/DoS phạm vi rộng hơn; theo dõi và nâng ngay khi có
bản vá upstream.

---

*API mà FE tiêu thụ: [openapi.yaml](./openapi.yaml) & [OPENAPI.md](./OPENAPI.md). Luật nghiệp vụ FE cần tôn trọng (state machine, LLM chỉ gợi ý): [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md).*
