import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it } from 'vitest';
import { apiClient } from '../../lib/apiClient';
import type { ApiResponse, RequestResponse, SummaryResponse } from '../../types/api';
import { assignRequest, fetchRequestSummary, updateRequestStatus } from './api';

const apiMock = new MockAdapter(apiClient);

const request: RequestResponse = {
  id: 9,
  title: 'Login fails',
  description: 'The login endpoint returns 500.',
  category: 'BUG',
  priority: 'HIGH',
  status: 'PENDING',
  clientId: 4,
  assignedDeveloperId: null,
  version: 0,
  createdAt: '2026-08-10T08:00:00Z',
  updatedAt: '2026-08-10T08:00:00Z',
};

afterEach(() => apiMock.reset());

describe('request workflow API contract', () => {
  it('sends expectedVersion for auto and manual assignment', async () => {
    apiMock.onPatch('/requests/9/assign').reply((config) => {
      expect(JSON.parse(config.data as string)).toEqual({ auto: true, expectedVersion: 0 });
      return [200, { status: 200, message: 'success', data: request } satisfies ApiResponse<RequestResponse>];
    });
    await expect(assignRequest(9, { auto: true, expectedVersion: 0 })).resolves.toEqual(request);

    apiMock.reset();
    apiMock.onPatch('/requests/9/assign').reply((config) => {
      expect(JSON.parse(config.data as string)).toEqual({ auto: false, developerId: 2, expectedVersion: 3 });
      return [200, { status: 200, message: 'success', data: request } satisfies ApiResponse<RequestResponse>];
    });
    await assignRequest(9, { auto: false, developerId: 2, expectedVersion: 3 });
  });

  it('updates status with a bounded memo and optimistic version', async () => {
    apiMock.onPatch('/requests/9/status').reply((config) => {
      expect(JSON.parse(config.data as string)).toEqual({ status: 'IN_PROGRESS', memo: 'Started', expectedVersion: 1 });
      return [200, { status: 200, message: 'success', data: request } satisfies ApiResponse<RequestResponse>];
    });

    await updateRequestStatus(9, { status: 'IN_PROGRESS', memo: 'Started', expectedVersion: 1 });
  });

  it('loads the AI summary through the canonical sub-resource', async () => {
    const summary = { summary: 'Login currently fails with a server error.' };
    apiMock.onGet('/requests/9/summary').reply(200, {
      status: 200,
      message: 'success',
      data: summary,
    } satisfies ApiResponse<SummaryResponse>);

    await expect(fetchRequestSummary(9)).resolves.toEqual(summary);
  });
});
