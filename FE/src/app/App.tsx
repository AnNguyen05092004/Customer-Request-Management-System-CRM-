import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute, PublicOnlyRoute, RoleRoute } from '../auth/RouteGuards';
import { AppLayout } from '../components/AppLayout';
import { PageLoading } from '../components/PageStates';
import { ForbiddenPage, NotFoundPage } from './SystemPages';

const LoginPage = lazy(() => import('../features/auth/LoginPage').then((module) => ({ default: module.LoginPage })));
const RegisterPage = lazy(() => import('../features/members/RegisterPage').then((module) => ({ default: module.RegisterPage })));
const MemberListPage = lazy(() => import('../features/members/MemberListPage').then((module) => ({ default: module.MemberListPage })));
const MemberDetailPage = lazy(() => import('../features/members/MemberDetailPage').then((module) => ({ default: module.MemberDetailPage })));
const RequestListPage = lazy(() => import('../features/requests/RequestListPage').then((module) => ({ default: module.RequestListPage })));
const RequestDetailPage = lazy(() => import('../features/requests/RequestDetailPage').then((module) => ({ default: module.RequestDetailPage })));
const RequestCreatePage = lazy(() => import('../features/requests/RequestCreatePage').then((module) => ({ default: module.RequestCreatePage })));

export function App() {
  return (
    <Suspense fallback={<PageLoading rows={6} />}>
      <Routes>
        <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
        <Route path="/register" element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/requests" replace />} />
            <Route path="/requests" element={<RequestListPage />} />
            <Route path="/requests/:id" element={<RequestDetailPage />} />
            <Route element={<RoleRoute allow={['CLIENT']} />}>
              <Route path="/requests/new" element={<RequestCreatePage />} />
            </Route>
            <Route element={<RoleRoute allow={['ADMIN']} />}>
              <Route path="/members" element={<MemberListPage />} />
              <Route path="/members/:id" element={<MemberDetailPage />} />
            </Route>
            <Route path="/403" element={<ForbiddenPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Suspense>
  );
}
