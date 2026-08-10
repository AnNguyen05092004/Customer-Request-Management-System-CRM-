import { apiClient, unwrap } from '../../lib/apiClient';
import type { ApiResponse, StatsResponse } from '../../types/api';

export function fetchRequestStats(signal?: AbortSignal): Promise<StatsResponse> {
  return unwrap(apiClient.get<ApiResponse<StatsResponse>>('/requests/stats', { signal }));
}
