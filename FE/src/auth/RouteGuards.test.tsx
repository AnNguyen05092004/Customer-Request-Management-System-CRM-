import { render, screen } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext, type AuthContextValue } from './authContextDefinition';
import { ProtectedRoute, RoleRoute } from './RouteGuards';

const baseContext: AuthContextValue = {
  session: null,
  isAuthenticated: false,
  role: null,
  login: vi.fn(),
  logout: vi.fn(),
};

function renderRoutes(context: AuthContextValue, initialPath = '/members') {
  return render(
    <AuthContext.Provider value={context}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<div>Login screen</div>} />
          <Route path="/403" element={<div>Forbidden screen</div>} />
          <Route element={<ProtectedRoute />}>
            <Route element={<RoleRoute allow={['ADMIN']} />}>
              <Route path="/members" element={<><Outlet /><div>Member screen</div></>} />
            </Route>
            <Route element={<RoleRoute allow={['CLIENT']} />}>
              <Route path="/requests/new" element={<div>Create request screen</div>} />
            </Route>
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('route guards', () => {
  it('redirects unauthenticated visitors to login', () => {
    renderRoutes(baseContext);
    expect(screen.getByText('Login screen')).toBeInTheDocument();
  });

  it('redirects authenticated non-admin users to forbidden', () => {
    renderRoutes({ ...baseContext, isAuthenticated: true, role: 'CLIENT' });
    expect(screen.getByText('Forbidden screen')).toBeInTheDocument();
  });

  it('allows administrators into member routes', () => {
    renderRoutes({ ...baseContext, isAuthenticated: true, role: 'ADMIN' });
    expect(screen.getByText('Member screen')).toBeInTheDocument();
  });

  it('allows clients and blocks administrators on the request create route', () => {
    const clientView = renderRoutes({ ...baseContext, isAuthenticated: true, role: 'CLIENT' }, '/requests/new');
    expect(screen.getByText('Create request screen')).toBeInTheDocument();
    clientView.unmount();

    renderRoutes({ ...baseContext, isAuthenticated: true, role: 'ADMIN' }, '/requests/new');
    expect(screen.getByText('Forbidden screen')).toBeInTheDocument();
  });
});
