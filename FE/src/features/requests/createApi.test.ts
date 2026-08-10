import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it } from 'vitest';
import { apiClient } from '../../lib/apiClient';
import type { ApiResponse, ClassifyResult, PriorityResult, RequestResponse } from '../../types/api';
import { classifyRequest, createRequest, suggestRequestPriority } from './api';

const apiMock = new MockAdapter(apiClient);

afterEach(() => apiMock.reset());

describe('request create and AI API contract', () => {
  it('creates a request without adding fields outside the OpenAPI contract', async () => {
    const created: RequestResponse = {
      id: 12,
      title: 'Export report',
      description: 'Need a monthly CSV export.',
      category: 'FEATURE',
      priority: 'MEDIUM',
      status: 'PENDING',
      clientId: 4,
      assignedDeveloperId: null,
      version: 0,
      createdAt: '2026-08-10T09:00:00Z',
      updatedAt: '2026-08-10T09:00:00Z',
    };
    apiMock.onPost('/requests').reply((config) => {
      expect(JSON.parse(config.data as string)).toEqual({
        title: 'Export report',
        description: 'Need a monthly CSV export.',
        category: 'FEATURE',
        priority: 'MEDIUM',
      });
      return [201, { status: 201, message: 'created', data: created } satisfies ApiResponse<RequestResponse>];
    });

    await expect(createRequest({
      title: 'Export report',
      description: 'Need a monthly CSV export.',
      category: 'FEATURE',
      priority: 'MEDIUM',
    })).resolves.toEqual(created);
  });

  it('uses the same description for category and priority suggestions', async () => {
    const classification: ClassifyResult = { category: 'BUG', confidence: 0.91, reason: 'An error is described.' };
    const priority: PriorityResult = { priority: 'HIGH', confidence: 0.86, reason: 'The issue blocks login.' };
    apiMock.onPost('/requests/classify').reply((config) => {
      expect(JSON.parse(config.data as string)).toEqual({ description: 'Login returns an error.' });
      return [200, { status: 200, message: 'success', data: classification } satisfies ApiResponse<ClassifyResult>];
    });
    apiMock.onPost('/requests/suggest-priority').reply((config) => {
      expect(JSON.parse(config.data as string)).toEqual({ description: 'Login returns an error.' });
      return [200, { status: 200, message: 'success', data: priority } satisfies ApiResponse<PriorityResult>];
    });

    await expect(classifyRequest({ description: 'Login returns an error.' })).resolves.toEqual(classification);
    await expect(suggestRequestPriority({ description: 'Login returns an error.' })).resolves.toEqual(priority);
  });
});
