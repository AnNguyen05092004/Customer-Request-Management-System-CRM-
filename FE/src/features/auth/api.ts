import { publicApiClient, unwrap } from '../../lib/apiClient';
import { toApiError } from '../../lib/apiError';
import type { ApiResponse, LoginRequest, TokenResponse } from '../../types/api';

export function login(request: LoginRequest): Promise<TokenResponse> {
  return unwrap(publicApiClient.post<ApiResponse<TokenResponse>>('/auth/login', request));
}

export async function logout(refreshToken: string): Promise<void> {
  try {
    await publicApiClient.post<ApiResponse<null>>('/auth/logout', { refreshToken });
  } catch (error) {
    throw toApiError(error);
  }
}
