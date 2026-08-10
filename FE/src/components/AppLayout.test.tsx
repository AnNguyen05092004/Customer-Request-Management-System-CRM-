import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '../auth/useAuth';
import { AppLayout } from './AppLayout';

vi.mock('../auth/useAuth', () => ({ useAuth: vi.fn() }));
vi.mock('../features/alerts/AlertBell', () => ({ AlertBell: () => <button type="button">Alerts</button> }));

const logout = vi.fn<() => Promise<void>>();

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/requests']}>
      <Routes>
        <Route path="/requests" element={<AppLayout />}>
          <Route index element={<><div>Request content</div><LocationProbe /></>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}{location.search}</div>;
}

describe('AppLayout account menu', () => {
  beforeEach(() => {
    logout.mockReset();
    logout.mockResolvedValue();
    vi.mocked(useAuth).mockReturnValue({
      session: null,
      isAuthenticated: true,
      role: 'ADMIN',
      login: vi.fn(),
      logout,
    });
  });

  it('opens the account menu and logs out', async () => {
    const user = userEvent.setup();
    renderLayout();

    const menuButton = screen.getByRole('button', { name: 'Mở menu tài khoản' });
    expect(menuButton).toHaveAttribute('aria-expanded', 'false');

    await user.click(menuButton);

    expect(menuButton).toHaveAttribute('aria-expanded', 'true');
    await user.click(screen.getByRole('menuitem', { name: 'Đăng xuất' }));

    expect(logout).toHaveBeenCalledOnce();
    expect(await screen.findByText('Login page')).toBeInTheDocument();
  });

  it('closes the account menu with Escape', async () => {
    const user = userEvent.setup();
    renderLayout();

    const menuButton = screen.getByRole('button', { name: 'Mở menu tài khoản' });
    await user.click(menuButton);
    expect(screen.getByRole('menu')).toBeInTheDocument();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(menuButton).toHaveAttribute('aria-expanded', 'false');
  });

  it('sends global search terms to the server-backed request list URL', async () => {
    const user = userEvent.setup();
    renderLayout();

    await user.type(screen.getByRole('searchbox', { name: 'Search requests' }), ' login timeout ');
    await user.keyboard('{Enter}');

    expect(screen.getByTestId('location')).toHaveTextContent('/requests?keyword=login%20timeout');
  });
});
