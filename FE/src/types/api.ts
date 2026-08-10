export type Role = 'ADMIN' | 'DEVELOPER' | 'CLIENT';
export type Category = 'BUG' | 'FEATURE' | 'INQUIRY';
export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';
export type RequestStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';
export type AlertType = 'ASSIGNED' | 'STATUS_CHANGED' | 'HIGH_PRIORITY_REGISTERED';

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  role: Role;
}

export interface MemberCreateRequest {
  email: string;
  password: string;
  name: string;
}

export interface MemberResponse {
  id: number;
  email: string;
  name: string;
  role: Role;
  createdAt: string;
}

export interface RequestCreateRequest {
  title: string;
  description?: string;
  category: Category;
  priority: Priority;
}

export interface RequestResponse {
  id: number;
  title: string;
  description: string | null;
  category: Category;
  priority: Priority;
  status: RequestStatus;
  clientId: number;
  assignedDeveloperId: number | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export type AssignRequest =
  | { auto: true; expectedVersion: number }
  | { auto: false; developerId: number; expectedVersion: number };

export interface StatusUpdateRequest {
  status: RequestStatus;
  memo?: string;
  expectedVersion: number;
}

export interface HistoryResponse {
  id: number;
  requestId: number;
  changedBy: number;
  fromStatus: RequestStatus | null;
  toStatus: RequestStatus | null;
  memo: string | null;
  changedAt: string;
}

export interface SummaryResponse {
  summary: string;
}

export interface DescriptionRequest {
  description: string;
}

export interface ClassifyResult {
  category: Category;
  confidence: number;
  reason: string;
}

export interface PriorityResult {
  priority: Priority;
  confidence: number;
  reason: string;
}
