import { describe, expect, it } from 'vitest';
import type { TokenResponse } from '../types/api';
import { sessionStore } from './session';

const tokens: TokenResponse = {
  accessToken: 'access-token',
  refreshToken: 'r'.repeat(43),
  tokenType: 'Bearer',
  role: 'ADMIN',
};

describe('sessionStore', () => {
  it('round-trips a valid token response', () => {
    sessionStore.write(tokens);
    expect(sessionStore.read()).toEqual(tokens);
  });

  it('removes malformed storage instead of trusting it', () => {
    window.localStorage.setItem('bzcom.crm.session', JSON.stringify({ accessToken: 'x', role: 'ROOT' }));
    expect(sessionStore.read()).toBeNull();
    expect(window.localStorage.length).toBe(0);
  });

  it('rejects a tampered authorization scheme', () => {
    window.localStorage.setItem(
      'bzcom.crm.session',
      JSON.stringify({ ...tokens, tokenType: 'Basic' }),
    );
    expect(sessionStore.read()).toBeNull();
  });

  it('clears the current session', () => {
    sessionStore.write(tokens);
    sessionStore.clear();
    expect(sessionStore.read()).toBeNull();
  });
});
