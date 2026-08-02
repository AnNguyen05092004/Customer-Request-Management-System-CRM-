import type { Role, TokenResponse } from '../types/api';

const STORAGE_KEY = 'bzcom.crm.session';

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  role: Role;
}

function isRole(value: unknown): value is Role {
  return value === 'ADMIN' || value === 'DEVELOPER' || value === 'CLIENT';
}

function isSession(value: unknown): value is AuthSession {
  if (typeof value !== 'object' || value === null) return false;
  const candidate = value as Partial<AuthSession>;
  return (
    typeof candidate.accessToken === 'string' &&
    candidate.accessToken.length > 0 &&
    typeof candidate.refreshToken === 'string' &&
    candidate.refreshToken.length >= 32 &&
    candidate.tokenType === 'Bearer' &&
    isRole(candidate.role)
  );
}

export const sessionStore = {
  read(): AuthSession | null {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed: unknown = JSON.parse(raw);
      if (!isSession(parsed)) {
        window.localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return parsed;
    } catch {
      window.localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  },

  write(tokens: TokenResponse): AuthSession {
    const session: AuthSession = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      tokenType: tokens.tokenType,
      role: tokens.role,
    };
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    return session;
  },

  clear(): void {
    window.localStorage.removeItem(STORAGE_KEY);
  },
};

export const AUTH_SESSION_CHANGED = 'bzcom:auth-session-changed';
export const AUTH_SESSION_EXPIRED = 'bzcom:auth-session-expired';

export function notifySessionChanged(): void {
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED));
}

export function notifySessionExpired(): void {
  window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED));
}
