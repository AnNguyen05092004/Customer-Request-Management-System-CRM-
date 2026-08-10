import axios, { type InternalAxiosRequestConfig } from 'axios';
import { sessionStore, notifySessionChanged, notifySessionExpired } from '../auth/session';
import { env } from '../config/env';
import type { ApiResponse, TokenResponse } from '../types/api';
import { ApiError, toApiError } from './apiError';

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

export const publicApiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

interface RefreshAttempt {
  refreshToken: string;
  promise: Promise<TokenResponse>;
}

let refreshInFlight: RefreshAttempt | null = null;

async function refreshTokens(usedRefreshToken: string): Promise<TokenResponse> {
  const response = await publicApiClient.post<ApiResponse<TokenResponse>>('/auth/refresh', {
    refreshToken: usedRefreshToken,
  });
  const tokens = response.data.data;
  if (sessionStore.read()?.refreshToken !== usedRefreshToken) {
    throw new ApiError('The session changed while tokens were being refreshed.', 401);
  }
  sessionStore.write(tokens);
  notifySessionChanged();
  return tokens;
}

apiClient.interceptors.request.use((config) => {
  const session = sessionStore.read();
  if (session) config.headers.Authorization = `${session.tokenType} ${session.accessToken}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) return Promise.reject(toApiError(error));

    const original = error.config as RetryableRequestConfig | undefined;
    if (error.response?.status === 401 && original && !original._retry && sessionStore.read()) {
      original._retry = true;
      let refreshedTokens: TokenResponse | null = null;
      const currentSession = sessionStore.read();
      if (!currentSession) return Promise.reject(toApiError(error));

      const attempt =
        refreshInFlight ??
        (refreshInFlight = {
          refreshToken: currentSession.refreshToken,
          promise: refreshTokens(currentSession.refreshToken),
        });
      try {
        refreshedTokens = await attempt.promise;
      } catch (refreshError) {
        // Do not let an old request clear a newer login (or restore a logged-out session).
        if (sessionStore.read()?.refreshToken === attempt.refreshToken) {
          sessionStore.clear();
          notifySessionExpired();
        }
        return Promise.reject(toApiError(refreshError));
      } finally {
        if (refreshInFlight === attempt) refreshInFlight = null;
      }

      if (sessionStore.read()?.accessToken !== refreshedTokens.accessToken) {
        return Promise.reject(new ApiError('The session changed while the request was being retried.', 401));
      }

      original.headers.Authorization = `${refreshedTokens.tokenType} ${refreshedTokens.accessToken}`;
      try {
        return await apiClient(original);
      } catch (retryError) {
        if (sessionStore.read()?.accessToken === refreshedTokens.accessToken) {
          sessionStore.clear();
          notifySessionExpired();
        }
        return Promise.reject(toApiError(retryError));
      }
    }

    return Promise.reject(toApiError(error));
  },
);

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const response = await request;
    return response.data.data;
  } catch (error) {
    throw toApiError(error);
  }
}
