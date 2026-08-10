import { apiClient, unwrap } from '../../lib/apiClient';
import type { AlertResponse, ApiResponse } from '../../types/api';

export function fetchAlerts(isRead?: boolean, signal?: AbortSignal): Promise<AlertResponse[]> {
  return unwrap(apiClient.get<ApiResponse<AlertResponse[]>>('/alerts', {
    params: isRead === undefined ? undefined : { isRead },
    signal,
  }));
}

export function markAlertRead(id: number): Promise<null> {
  return unwrap(apiClient.patch<ApiResponse<null>>(`/alerts/${id}/read`));
}
