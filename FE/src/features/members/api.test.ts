import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it } from 'vitest';
import { sessionStore } from '../../auth/session';
import { apiClient, publicApiClient } from '../../lib/apiClient';
import type { ApiResponse, MemberResponse } from '../../types/api';
import { fetchMember, fetchMembers, registerMember } from './api';

const businessMock = new MockAdapter(apiClient);
const publicMock = new MockAdapter(publicApiClient);

const member: MemberResponse = {
  id: 4,
  email: 'client@example.com',
  name: 'Client Example',
  role: 'CLIENT',
  createdAt: '2026-08-02T00:00:00Z',
};

afterEach(() => {
  businessMock.reset();
  publicMock.reset();
});

describe('member API contract', () => {
  it('registers through the public client without a role field', async () => {
    publicMock.onPost('/members').reply((config) => {
      const body = JSON.parse(config.data as string) as Record<string, unknown>;
      expect(body).toEqual({ email: 'client@example.com', password: '1234', name: 'Client Example' });
      expect(body).not.toHaveProperty('role');
      return [201, { status: 201, message: 'created', data: member } satisfies ApiResponse<MemberResponse>];
    });

    await expect(
      registerMember({ email: 'client@example.com', password: '1234', name: 'Client Example' }),
    ).resolves.toEqual(member);
  });

  it('uses the authenticated client for member list and detail', async () => {
    sessionStore.write({
      accessToken: 'access',
      refreshToken: 'r'.repeat(43),
      tokenType: 'Bearer',
      role: 'ADMIN',
    });
    businessMock.onGet('/members').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer access');
      return [200, { status: 200, message: 'success', data: [member] } satisfies ApiResponse<MemberResponse[]>];
    });
    businessMock.onGet('/members/4').reply(200, {
      status: 200,
      message: 'success',
      data: member,
    } satisfies ApiResponse<MemberResponse>);

    await expect(fetchMembers()).resolves.toEqual([member]);
    await expect(fetchMember(4)).resolves.toEqual(member);
  });
});
