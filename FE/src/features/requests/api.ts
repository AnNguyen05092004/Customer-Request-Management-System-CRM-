import { env } from '../../config/env';
import { apiClient, unwrap } from '../../lib/apiClient';
import { ApiError } from '../../lib/apiError';
import type {
  ApiResponse,
  AssignRequest,
  Category,
  ClassifyResult,
  DescriptionRequest,
  HistoryResponse,
  PageResponse,
  Priority,
  PriorityResult,
  RequestCreateRequest,
  RequestResponse,
  RequestStatus,
  Role,
  StatusUpdateRequest,
  SummaryResponse,
} from '../../types/api';
import { demoHistories, demoRequests } from './demoData';

export interface RequestFilter {
  page: number;
  size: number;
  sort: string;
  keyword?: string;
  status?: RequestStatus;
  category?: Category;
  priority?: Priority;
}

function abortableDelay(signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(resolve, 120);
    signal?.addEventListener(
      'abort',
      () => {
        window.clearTimeout(timer);
        reject(new DOMException('Request aborted', 'AbortError'));
      },
      { once: true },
    );
  });
}

function sortDemoRequests(items: RequestResponse[], sort: string): RequestResponse[] {
  const [field = 'createdAt', direction = 'desc'] = sort.split(',');
  const multiplier = direction === 'asc' ? 1 : -1;
  return [...items].sort((left, right) => {
    const leftValue = field === 'id' ? left.id : Date.parse(left.createdAt);
    const rightValue = field === 'id' ? right.id : Date.parse(right.createdAt);
    return (leftValue - rightValue) * multiplier;
  });
}

function canReadDemoRequest(request: RequestResponse, role: Role): boolean {
  if (role === 'ADMIN') return true;
  if (role === 'DEVELOPER') return request.assignedDeveloperId === 2;
  return request.clientId === 4;
}

export async function fetchRequests(
  filter: RequestFilter,
  signal?: AbortSignal,
  demoRole: Role = 'ADMIN',
): Promise<PageResponse<RequestResponse>> {
  if (!env.isRequestDemoMode) {
    return unwrap(
      apiClient.get<ApiResponse<PageResponse<RequestResponse>>>('/requests', {
        params: filter,
        signal,
      }),
    );
  }

  await abortableDelay(signal);
  const keyword = filter.keyword?.trim().toLowerCase();
  const filtered = demoRequests.filter(
    (request) =>
      canReadDemoRequest(request, demoRole) &&
      (!filter.status || request.status === filter.status) &&
      (!filter.category || request.category === filter.category) &&
      (!filter.priority || request.priority === filter.priority) &&
      (!keyword || request.title.toLowerCase().includes(keyword) || request.description?.toLowerCase().includes(keyword)),
  );
  const sorted = sortDemoRequests(filtered, filter.sort);
  const start = filter.page * filter.size;
  return {
    content: sorted.slice(start, start + filter.size),
    page: filter.page,
    size: filter.size,
    totalElements: sorted.length,
    totalPages: Math.ceil(sorted.length / filter.size),
  };
}

export async function fetchRequest(id: number, signal?: AbortSignal, demoRole: Role = 'ADMIN'): Promise<RequestResponse> {
  if (!env.isRequestDemoMode) {
    return unwrap(apiClient.get<ApiResponse<RequestResponse>>(`/requests/${id}`, { signal }));
  }
  await abortableDelay(signal);
  const request = demoRequests.find((item) => item.id === id);
  if (!request) throw new ApiError(`Request ${id} was not found.`, 404);
  if (!canReadDemoRequest(request, demoRole)) throw new ApiError('You do not have permission to view this request.', 403);
  return request;
}

export async function fetchRequestHistory(
  id: number,
  signal?: AbortSignal,
  demoRole: Role = 'ADMIN',
): Promise<HistoryResponse[]> {
  if (!env.isRequestDemoMode) {
    return unwrap(apiClient.get<ApiResponse<HistoryResponse[]>>(`/requests/${id}/history`, { signal }));
  }
  await abortableDelay(signal);
  const request = demoRequests.find((item) => item.id === id);
  if (!request) throw new ApiError(`Request ${id} was not found.`, 404);
  if (!canReadDemoRequest(request, demoRole)) throw new ApiError('You do not have permission to view this request.', 403);
  return demoHistories[id] ?? [];
}

export function assignRequest(id: number, request: AssignRequest): Promise<RequestResponse> {
  return unwrap(apiClient.patch<ApiResponse<RequestResponse>>(`/requests/${id}/assign`, request));
}

export function updateRequestStatus(id: number, request: StatusUpdateRequest): Promise<RequestResponse> {
  return unwrap(apiClient.patch<ApiResponse<RequestResponse>>(`/requests/${id}/status`, request));
}

export function fetchRequestSummary(id: number): Promise<SummaryResponse> {
  return unwrap(apiClient.get<ApiResponse<SummaryResponse>>(`/requests/${id}/summary`));
}

export function createRequest(request: RequestCreateRequest): Promise<RequestResponse> {
  return unwrap(apiClient.post<ApiResponse<RequestResponse>>('/requests', request));
}

export function classifyRequest(request: DescriptionRequest): Promise<ClassifyResult> {
  return unwrap(apiClient.post<ApiResponse<ClassifyResult>>('/requests/classify', request));
}

export function suggestRequestPriority(request: DescriptionRequest): Promise<PriorityResult> {
  return unwrap(apiClient.post<ApiResponse<PriorityResult>>('/requests/suggest-priority', request));
}
