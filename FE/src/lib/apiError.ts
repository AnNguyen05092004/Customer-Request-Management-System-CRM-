import axios from 'axios';
import type { ApiResponse } from '../types/api';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number | null,
    readonly cause?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;

  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    const status = error.response?.status ?? null;
    const message =
      error.response?.data?.message ??
      (error.code === 'ECONNABORTED'
        ? 'The server took too long to respond.'
        : 'Unable to connect to the server. Please try again.');
    return new ApiError(message, status, error);
  }

  if (error instanceof Error) return new ApiError(error.message, null, error);
  return new ApiError('An unexpected error occurred.', null, error);
}
