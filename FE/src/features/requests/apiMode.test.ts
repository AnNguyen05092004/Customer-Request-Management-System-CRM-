import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ApiResponse, PageResponse, RequestResponse } from '../../types/api';

afterEach(() => {
  vi.unstubAllEnvs();
  vi.resetModules();
});

describe('request API mode', () => {
  it('calls the OpenAPI path with server-side filter and pagination parameters', async () => {
    vi.stubEnv('VITE_REQUEST_DATA_MODE', 'api');
    vi.resetModules();
    const [{ fetchRequests }, { apiClient }] = await Promise.all([import('./api'), import('../../lib/apiClient')]);
    const mock = new MockAdapter(apiClient);
    const response: PageResponse<RequestResponse> = {
      content: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };
    mock.onGet('/requests').reply((config) => {
      expect(config.params).toMatchObject({
        page: 1,
        size: 20,
        sort: 'createdAt,desc',
        status: 'PENDING',
        keyword: 'timeout',
      });
      return [200, { status: 200, message: 'success', data: response } satisfies ApiResponse<PageResponse<RequestResponse>>];
    });

    await expect(
      fetchRequests({ page: 1, size: 20, sort: 'createdAt,desc', status: 'PENDING', keyword: 'timeout' }),
    ).resolves.toEqual(response);
    mock.restore();
  });
});
