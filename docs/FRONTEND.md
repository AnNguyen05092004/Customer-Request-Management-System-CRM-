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
| Build tool | **Vite** | Dev server cực nhanh, cấu hình tối giản |
| Framework | **React 18** | Chuẩn phổ biến |
| Ngôn ngữ | **TypeScript** | Type-safe khi khớp DTO backend → bắt lỗi lúc compile, tự động gợi ý field |
| Data fetching | **TanStack Query (React Query) v5** | Cache, loading/error state, invalidation sau mutation — bỏ hẳn boilerplate `useState/useEffect` |
| HTTP | **axios** | Interceptor gắn JWT + xử lý 401 tập trung |
| Routing | **React Router v6** | Nested route + protected route theo role |
| UI kit | **Ant Design v5** | Sẵn Table (paging/sort/filter), Form (validation), Tag, Badge, message — hợp CRM, lên UI rất nhanh |
| Biểu đồ | **Ant Design Charts** (hoặc Recharts) | Vẽ dashboard stats |
| Form | **Ant Design Form** + rule | Validation client khớp validation backend |

> **Vì sao Ant Design (không Tailwind/shadcn)?** CRM = nhiều bảng + form + filter. Ant Design cho sẵn `Table` với phân trang/sắp xếp/lọc server-side, `Form` với validation → **tiết kiệm hàng giờ** so với tự ghép component. Đây là lựa chọn tối ưu thời gian cho bài OJT.

> **Vì sao React Query?** Backend đã có sẵn envelope + phân trang; React Query lo cache + refetch + invalidate (vd sau khi đổi status → tự refetch danh sách). Không cần Redux.

## 3. Cấu trúc thư mục

Feature-based, song song với package-by-feature của backend (dễ đối chiếu, mỗi người sở hữu 1 feature):

```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
├── .env.example              # VITE_API_BASE_URL=http://localhost:8080/api
├── Dockerfile                # multi-stage build → nginx phục vụ static
├── nginx.conf                # SPA fallback + proxy /api (tuỳ chọn)
└── src/
    ├── main.tsx              # mount React, QueryClientProvider, AntdApp, Router
    ├── App.tsx               # định nghĩa routes
    ├── config/
    │   └── queryClient.ts
    ├── lib/
    │   └── apiClient.ts      # axios instance + interceptor + unwrap envelope
    ├── types/
    │   └── api.ts            # types khớp DTO backend (§5)
    ├── auth/
    │   ├── AuthContext.tsx   # lưu token + user role, login/logout
    │   ├── useAuth.ts
    │   └── ProtectedRoute.tsx # guard theo role
    ├── components/
    │   ├── AppLayout.tsx     # sidebar menu (ẩn/hiện theo role) + header + AlertBell
    │   ├── AlertBell.tsx     # Badge chuông + popover danh sách alert
    │   └── StatusTag.tsx     # Tag màu theo status/priority/category
    ├── features/
    │   ├── auth/pages/LoginPage.tsx
    │   ├── members/
    │   │   ├── api.ts        # hàm gọi + hooks React Query
    │   │   └── pages/MemberListPage.tsx, MemberDetailPage.tsx, RegisterPage.tsx
    │   ├── requests/
    │   │   ├── api.ts
    │   │   └── pages/RequestListPage.tsx, RequestDetailPage.tsx, RequestCreatePage.tsx
    │   ├── stats/
    │   │   ├── api.ts
    │   │   └── pages/StatsDashboardPage.tsx
    │   └── alerts/
    │       └── api.ts
    └── utils/
        └── format.ts         # format ngày, enum → nhãn tiếng Việt
```

## 4. Kiến trúc tầng FE

```
┌─────────────────────────────────────────────────┐
│  Pages (features/*/pages)                         │  UI + gọi hooks
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
  id: number; title: string; description: string;
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
  memo: string; changedAt: string;
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
import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse, TokenResponse } from '../types/api';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // http://localhost:8080/api
});

// Client riêng, không interceptor: refresh/logout không được gửi Bearer access token cũ.
const authClient = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL });
const isAuthPath = (url = '') => ['/auth/login', '/auth/refresh', '/auth/logout'].some((p) => url.includes(p));
let refreshInFlight: Promise<TokenResponse> | null = null;

// Gắn JWT vào mọi request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token && !isAuthPath(config.url)) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Unwrap envelope + xử lý lỗi tập trung
apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError<ApiResponse<null>>) => {
    const status = error.response?.status;
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    if (status === 401 && original && !original._retry && !isAuthPath(original.url)) {
      original._retry = true;
      try {
        refreshInFlight ??= authClient.post<ApiResponse<TokenResponse>>('/auth/refresh', {
          refreshToken: localStorage.getItem('refreshToken'),
        }).then((res) => res.data.data);
        const tokens = await refreshInFlight;
        localStorage.setItem('accessToken', tokens.accessToken);
        localStorage.setItem('refreshToken', tokens.refreshToken);
        localStorage.setItem('role', tokens.role);
        original.headers.Authorization = `Bearer ${tokens.accessToken}`;
        return apiClient(original);
      } catch {
        // Refresh thất bại: phiên đã hết hoặc refresh token bị revoke.
      } finally {
        refreshInFlight = null;
      }
    }
    if (status === 401) {
      localStorage.clear();
      window.location.href = '/login'; // token hết hạn → về login
    }
    // message lỗi từ envelope backend {status,message,data:null}
    const msg = error.response?.data?.message ?? 'Có lỗi xảy ra';
    return Promise.reject(new Error(msg));
  }
);

// Helper unwrap data
export async function unwrap<T>(p: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const res = await p;
  return res.data.data;
}
```

Ví dụ API function (`features/requests/api.ts`):

```typescript
import { apiClient, unwrap } from '../../lib/apiClient';
import type { AssignRequest, PageResult, RequestCreateRequest, RequestResponse } from '../../types/api';

export interface RequestFilter {
  page?: number; size?: number; sort?: string;
  status?: string; category?: string; priority?: string; keyword?: string;
}

export const fetchRequests = (f: RequestFilter) =>
  unwrap<PageResult<RequestResponse>>(apiClient.get('/requests', { params: f }));

export const fetchRequest = (id: number) =>
  unwrap<RequestResponse>(apiClient.get(`/requests/${id}`));

export const createRequest = (body: RequestCreateRequest) =>
  unwrap<RequestResponse>(apiClient.post('/requests', body));

export const assignRequest = (id: number, body: AssignRequest) =>
  unwrap<RequestResponse>(apiClient.patch(`/requests/${id}/assign`, body));

export const updateStatus = (id: number, body: { status: string; memo?: string; expectedVersion: number }) =>
  unwrap<RequestResponse>(apiClient.patch(`/requests/${id}/status`, body));
```

## 7. Xác thực & phân quyền (JWT)

**Lưu token:** `localStorage` (`accessToken`, `refreshToken`, `role`). Đủ cho demo OJT (nói rõ trade-off: production nên dùng httpOnly cookie chống XSS). Khi access token nhận 401, interceptor chỉ gọi một lần `POST /auth/refresh`, atomically thay cả hai token rồi retry request; refresh fail thì xóa state và về login. Không tự retry `/auth/refresh` để tránh loop.

`AuthContext` giữ `{ isAuthenticated, role, login(), logout() }`:

```typescript
// auth/AuthContext.tsx (rút gọn)
const login = async (email: string, password: string) => {
  const data = await unwrap<TokenResponse>(apiClient.post('/auth/login', { email, password }));
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('role', data.role);
  setRole(data.role);
};
const logout = async () => {
  await authClient.post('/auth/logout', { refreshToken: localStorage.getItem('refreshToken') }).catch(() => {});
  localStorage.clear(); setRole(null);
};
```

**Protected route theo role:**

```typescript
// auth/ProtectedRoute.tsx
export function ProtectedRoute({ allow }: { allow?: Role[] }) {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (allow && role && !allow.includes(role)) return <Navigate to="/403" replace />;
  return <Outlet />;
}
```

> FE guard chỉ để **UX** (ẩn nút/trang không được phép). Bảo mật thật vẫn ở backend (401/403). Nói rõ điều này = hiểu đúng vai trò client-side auth.

## 8. Data fetching với TanStack Query

**Query key** đặt theo tài nguyên + tham số → tự cache & refetch đúng:

```typescript
// features/requests/api.ts (hooks)
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

```typescript
// App.tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />
  <Route element={<ProtectedRoute />}>
    <Route element={<AppLayout />}>
      <Route index element={<Navigate to="/requests" />} />
      <Route path="/requests" element={<RequestListPage />} />
      <Route path="/requests/new" element={<ProtectedRoute allow={['CLIENT']} />}>
        <Route index element={<RequestCreatePage />} />
      </Route>
      <Route path="/requests/:id" element={<RequestDetailPage />} />
      <Route path="/alerts" element={<AlertListPage />} />
      <Route element={<ProtectedRoute allow={['ADMIN']} />}>
        <Route path="/members" element={<MemberListPage />} />
        <Route path="/members/:id" element={<MemberDetailPage />} />
        <Route path="/stats" element={<StatsDashboardPage />} />
      </Route>
    </Route>
  </Route>
  <Route path="/403" element={<ForbiddenPage />} />
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
```

Chạy dev:
```bash
cd frontend
npm install
npm run dev            # http://localhost:5173, proxy tới backend :8080
```

**CORS:** backend cần cho phép origin `http://localhost:5173` (dev) — thêm `CorsConfig` ở Spring (allow `localhost:5173`, methods GET/POST/PATCH/DELETE, header Authorization). Ghi chú này gửi cho member A (config).

**Dockerfile (multi-stage → nginx):**
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
```
`nginx.conf`: SPA fallback (`try_files $uri /index.html`) + (tuỳ chọn) proxy `/api` sang service backend.

**Thêm vào docker-compose** (mở rộng compose ở [ARCHITECTURE.md §10](./ARCHITECTURE.md#10-kiến-trúc-triển-khai-docker)):
```yaml
  frontend:
    build: ./frontend
    ports: ["5173:80"]
    depends_on: [app]
```
→ `docker compose up --build` chạy cả 3: frontend + app + db. Demo một lệnh.

## 13. Phân công & thứ tự làm

> **Nguyên tắc:** FE chỉ bắt đầu khi API contract ([openapi.yaml](./openapi.yaml)) đã chốt (Phase 0) và backend có endpoint chạy được (dù mock). Ưu tiên đúng thứ tự để không chặn nhau.

| Thứ tự | Việc | Phụ thuộc |
|---|---|---|
| 1 | Setup Vite + TS + antd + router + React Query + apiClient + AuthContext | — (làm được sớm, dùng mock/JSON) |
| 2 | Login + ProtectedRoute + AppLayout (menu theo role) | #1 + `/auth/login` backend |
| 3 | RequestListPage (Table + filter + paging) | `/requests` backend |
| 4 | RequestDetailPage (history + assign + status) | `/requests/{id}`, `/assign`, `/status`, `/history` |
| 5 | RequestCreatePage + nút AI gợi ý | `/requests`, `/classify` |
| 6 | AlertBell + AlertListPage | `/alerts` |
| 7 | StatsDashboardPage (charts) | `/requests/stats` |
| 8 | Member pages + Register | `/members` |

**Gợi ý người làm:** vì backend đã chia A/B/C/D, FE có thể do **1–2 người phụ trách sau khi phần backend của họ xong** (thường A hoặc D rảnh sớm), hoặc mỗi người làm trang FE ứng với feature backend mình đã làm (dễ vì hiểu rõ API đó nhất). Ghi rõ đóng góp FE ở slide 11.

**Rủi ro cần nhớ:** nếu quỹ thời gian eo hẹp → cắt theo thứ tự ngược (#8 → #7 → ...), giữ tối thiểu #1–#5 để demo trọn luồng chính. **Tuyệt đối không hi sinh chất lượng backend để làm FE** — backend mới là thứ được chấm.

---

*API mà FE tiêu thụ: [openapi.yaml](./openapi.yaml) & [OPENAPI.md](./OPENAPI.md). Luật nghiệp vụ FE cần tôn trọng (state machine, LLM chỉ gợi ý): [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md).*
