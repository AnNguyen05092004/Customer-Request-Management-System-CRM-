import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { login as loginRequest, logout as logoutRequest } from '../features/auth/api';
import type { LoginRequest } from '../types/api';
import {
  AUTH_SESSION_CHANGED,
  AUTH_SESSION_EXPIRED,
  notifySessionChanged,
  sessionStore,
  type AuthSession,
} from './session';
import { AuthContext, type AuthContextValue } from './authContextDefinition';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => sessionStore.read());

  useEffect(() => {
    const syncSession = () => setSession(sessionStore.read());
    const clearSession = () => setSession(null);

    window.addEventListener(AUTH_SESSION_CHANGED, syncSession);
    window.addEventListener(AUTH_SESSION_EXPIRED, clearSession);
    window.addEventListener('storage', syncSession);
    return () => {
      window.removeEventListener(AUTH_SESSION_CHANGED, syncSession);
      window.removeEventListener(AUTH_SESSION_EXPIRED, clearSession);
      window.removeEventListener('storage', syncSession);
    };
  }, []);

  const login = useCallback(async (request: LoginRequest) => {
    const tokens = await loginRequest(request);
    const nextSession = sessionStore.write(tokens);
    setSession(nextSession);
    notifySessionChanged();
  }, []);

  const logout = useCallback(async () => {
    const current = sessionStore.read();
    try {
      if (current) await logoutRequest(current.refreshToken);
    } catch {
      // Local logout must remain reliable when the network is unavailable. The remote
      // refresh token will expire naturally; no token value is retained or logged here.
    } finally {
      sessionStore.clear();
      setSession(null);
      notifySessionChanged();
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: session !== null,
      role: session?.role ?? null,
      login,
      logout,
    }),
    [login, logout, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
