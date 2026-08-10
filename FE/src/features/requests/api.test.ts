import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import type * as RequestApi from './api';

let fetchRequest: typeof RequestApi.fetchRequest;
let fetchRequests: typeof RequestApi.fetchRequests;

beforeAll(async () => {
  vi.stubEnv('VITE_REQUEST_DATA_MODE', 'demo');
  vi.resetModules();
  ({ fetchRequest, fetchRequests } = await import('./api'));
});

afterAll(() => {
  vi.unstubAllEnvs();
  vi.resetModules();
});

describe('request demo adapter', () => {
  it('applies contract filters and pagination', async () => {
    const page = await fetchRequests({
      page: 0,
      size: 1,
      sort: 'createdAt,desc',
      category: 'BUG',
      priority: 'HIGH',
    });
    expect(page.totalElements).toBe(2);
    expect(page.content).toHaveLength(1);
    expect(page.content[0]).toMatchObject({ category: 'BUG', priority: 'HIGH', id: 8021 });
  });

  it('searches title and description case-insensitively', async () => {
    const page = await fetchRequests({ page: 0, size: 10, sort: 'id,asc', keyword: 'KRW' });
    expect(page.content.map((request) => request.id)).toEqual([8023]);
  });

  it('returns an explicit 404 for an unknown demo request', async () => {
    await expect(fetchRequest(9999)).rejects.toMatchObject({ status: 404 });
  });

  it('scopes demo results to the role persona just like the planned backend contract', async () => {
    const filter = { page: 0, size: 10, sort: 'id,asc' };
    const developerPage = await fetchRequests(filter, undefined, 'DEVELOPER');
    const clientPage = await fetchRequests(filter, undefined, 'CLIENT');

    expect(developerPage.content.every((request) => request.assignedDeveloperId === 2)).toBe(true);
    expect(clientPage.content.every((request) => request.clientId === 4)).toBe(true);
    await expect(fetchRequest(8022, undefined, 'CLIENT')).rejects.toMatchObject({ status: 403 });
  });
});
