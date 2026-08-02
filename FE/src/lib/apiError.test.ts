import { AxiosError, AxiosHeaders } from 'axios';
import { describe, expect, it } from 'vitest';
import type { ApiResponse } from '../types/api';
import { ApiError, toApiError } from './apiError';

describe('toApiError', () => {
  it('keeps an existing ApiError', () => {
    const error = new ApiError('Forbidden', 403);
    expect(toApiError(error)).toBe(error);
  });

  it('uses the backend response envelope message and status', () => {
    const error = new AxiosError<ApiResponse<null>>(
      'Request failed',
      'ERR_BAD_REQUEST',
      { headers: new AxiosHeaders() },
      undefined,
      {
        data: { status: 409, message: 'Email already exists', data: null },
        status: 409,
        statusText: 'Conflict',
        headers: {},
        config: { headers: new AxiosHeaders() },
      },
    );
    expect(toApiError(error)).toMatchObject({ message: 'Email already exists', status: 409 });
  });
});
