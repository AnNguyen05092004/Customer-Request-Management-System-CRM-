import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it } from 'vitest';
import { apiClient } from '../../lib/apiClient';
import type { ApiResponse, StatsResponse } from '../../types/api';
import { fetchRequestStats } from './api';

const apiMock = new MockAdapter(apiClient);

afterEach(() => apiMock.reset());

describe('statistics API contract', () => {
  it('loads the canonical ADMIN statistics endpoint', async () => {
    const stats: StatsResponse = {
      total: 8,
      completed: 3,
      completionRate: 0.375,
      byCategory: { BUG: 4, FEATURE: 3, INQUIRY: 1 },
      byDeveloper: [{ developerId: 2, developerName: 'Dev One', assignedCount: 4, doneCount: 2 }],
    };
    apiMock.onGet('/requests/stats').reply(200, { status: 200, message: 'success', data: stats } satisfies ApiResponse<StatsResponse>);
    await expect(fetchRequestStats()).resolves.toEqual(stats);
  });
});
