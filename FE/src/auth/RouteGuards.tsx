import type { ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../types/api';
import { useAuth } from './useAuth';

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />;
  }
  return <Outlet />;
}

export function RoleRoute({ allow }: { allow: Role[] }) {
  const { role } = useAuth();
  if (!role || !allow.includes(role)) return <Navigate to="/403" replace />;
  return <Outlet />;
}

export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) return <Navigate to="/requests" replace />;
  return children;
}
