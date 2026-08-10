import { createContext } from 'react';
import type { LoginRequest, Role } from '../types/api';
import type { AuthSession } from './session';

export interface AuthContextValue {
  session: AuthSession | null;
  isAuthenticated: boolean;
  role: Role | null;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
