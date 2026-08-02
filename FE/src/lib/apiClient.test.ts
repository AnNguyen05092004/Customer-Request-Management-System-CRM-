import MockAdapter from 'axios-mock-adapter';
import { afterEach, describe, expect, it } from 'vitest';
import { sessionStore } from '../auth/session';
import type { ApiResponse, MemberResponse, TokenResponse } from '../types/api';
import { apiClient, publicApiClient, unwrap } from './apiClient';

const businessMock = new MockAdapter(apiClient);
const publicMock = new MockAdapter(publicApiClient);

afterEach(() => {
  businessMock.reset();
  publicMock.reset();
});

describe('apiClient refresh interceptor', () => {
  it('uses one refresh request for concurrent 401 responses and retries both calls', async () => {
    sessionStore.write({
      accessToken: 'expired-access',
      refreshToken: 'r'.repeat(43),
      tokenType: 'Bearer',
      role: 'ADMIN',
    });

    const rotatedTokens: TokenResponse = {
      accessToken: 'new-access',
      refreshToken: 'n'.repeat(43),
      tokenType: 'Bearer',
      role: 'ADMIN',
    };
    let refreshCalls = 0;
    let businessCalls = 0;

    publicMock.onPost('/auth/refresh').reply(async () => {
      refreshCalls += 1;
      await new Promise((resolve) => window.setTimeout(resolve, 20));
      return [200, { status: 200, message: 'success', data: rotatedTokens } satisfies ApiResponse<TokenResponse>];
    });

    businessMock.onGet('/members').reply(() => {
      businessCalls += 1;
      if (businessCalls <= 2) return [401, { status: 401, message: 'Unauthorized', data: null }];
      return [200, { status: 200, message: 'success', data: [] } satisfies ApiResponse<MemberResponse[]>];
    });

    const [first, second] = await Promise.all([
      unwrap(apiClient.get<ApiResponse<MemberResponse[]>>('/members')),
      unwrap(apiClient.get<ApiResponse<MemberResponse[]>>('/members')),
    ]);

    expect(first).toEqual([]);
    expect(second).toEqual([]);
    expect(refreshCalls).toBe(1);
    expect(businessCalls).toBe(4);
    expect(sessionStore.read()?.accessToken).toBe('new-access');
  });

  it('does not restore a session when logout happens during an in-flight refresh', async () => {
    sessionStore.write({
      accessToken: 'expired-access',
      refreshToken: 'r'.repeat(43),
      tokenType: 'Bearer',
      role: 'ADMIN',
    });
    publicMock.onPost('/auth/refresh').reply(async () => {
      await new Promise((resolve) => window.setTimeout(resolve, 30));
      return [
        200,
        {
          status: 200,
          message: 'success',
          data: {
            accessToken: 'must-not-be-restored',
            refreshToken: 'n'.repeat(43),
            tokenType: 'Bearer',
            role: 'ADMIN',
          },
        } satisfies ApiResponse<TokenResponse>,
      ];
    });
    businessMock.onGet('/members').reply(401, { status: 401, message: 'Unauthorized', data: null });

    const pendingRequest = unwrap(apiClient.get<ApiResponse<MemberResponse[]>>('/members'));
    await new Promise((resolve) => window.setTimeout(resolve, 5));
    sessionStore.clear();

    await expect(pendingRequest).rejects.toMatchObject({ status: 401 });
    expect(sessionStore.read()).toBeNull();
  });

  it('does not clear a newer login when an old refresh finishes later', async () => {
    sessionStore.write({
      accessToken: 'old-expired-access',
      refreshToken: 'r'.repeat(43),
      tokenType: 'Bearer',
      role: 'ADMIN',
    });
    publicMock.onPost('/auth/refresh').reply(async () => {
      await new Promise((resolve) => window.setTimeout(resolve, 30));
      return [
        200,
        {
          status: 200,
          message: 'success',
          data: {
            accessToken: 'old-refreshed-access',
            refreshToken: 'n'.repeat(43),
            tokenType: 'Bearer',
            role: 'ADMIN',
          },
        } satisfies ApiResponse<TokenResponse>,
      ];
    });
    businessMock.onGet('/members').reply(401, { status: 401, message: 'Unauthorized', data: null });

    const pendingRequest = unwrap(apiClient.get<ApiResponse<MemberResponse[]>>('/members'));
    await new Promise((resolve) => window.setTimeout(resolve, 5));
    sessionStore.write({
      accessToken: 'new-login-access',
      refreshToken: 'l'.repeat(43),
      tokenType: 'Bearer',
      role: 'DEVELOPER',
    });

    await expect(pendingRequest).rejects.toMatchObject({ status: 401 });
    expect(sessionStore.read()).toMatchObject({
      accessToken: 'new-login-access',
      refreshToken: 'l'.repeat(43),
      role: 'DEVELOPER',
    });
  });
});
