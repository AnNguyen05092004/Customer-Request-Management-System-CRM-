import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it } from 'vitest';
import { apiClient } from '../../lib/apiClient';
import type { AlertResponse, ApiResponse } from '../../types/api';
import { fetchAlerts, markAlertRead } from './api';

const apiMock = new MockAdapter(apiClient);
const alert: AlertResponse = {
  id: 3,
  requestId: 9,
  alertType: 'ASSIGNED',
  message: 'Request assigned to you.',
  isRead: false,
  createdAt: '2026-08-10T10:00:00Z',
};

afterEach(() => apiMock.reset());

describe('alert API contract', () => {
  it('omits isRead for all alerts and serializes it for filtered views', async () => {
    apiMock.onGet('/alerts').reply((config) => {
      expect(config.params).toBeUndefined();
      return [200, { status: 200, message: 'success', data: [alert] } satisfies ApiResponse<AlertResponse[]>];
    });
    await expect(fetchAlerts()).resolves.toEqual([alert]);

    apiMock.reset();
    apiMock.onGet('/alerts').reply((config) => {
      expect(config.params).toEqual({ isRead: false });
      return [200, { status: 200, message: 'success', data: [alert] } satisfies ApiResponse<AlertResponse[]>];
    });
    await fetchAlerts(false);
  });

  it('marks an owned alert read through its canonical sub-resource', async () => {
    apiMock.onPatch('/alerts/3/read').reply(200, { status: 200, message: 'success', data: null } satisfies ApiResponse<null>);
    await expect(markAlertRead(3)).resolves.toBeNull();
  });
});
