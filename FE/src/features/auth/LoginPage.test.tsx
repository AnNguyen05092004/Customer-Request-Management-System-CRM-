import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AuthContextValue } from '../../auth/authContextDefinition';
import { LoginPage } from './LoginPage';

const login = vi.fn<AuthContextValue['login']>();

vi.mock('../../auth/useAuth', () => ({
  useAuth: () => ({
    session: null,
    isAuthenticated: false,
    role: null,
    login,
    logout: vi.fn(),
  }),
}));

describe('LoginPage', () => {
  beforeEach(() => login.mockReset());

  it('validates required fields before calling the API', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await user.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByText('Enter your email address.')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('submits normalized form values to AuthContext', async () => {
    login.mockResolvedValueOnce();
    const user = userEvent.setup();
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'admin@bzcom.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: '1234' } });
    await user.click(screen.getByRole('button', { name: 'Log in' }));
    await waitFor(() => expect(login).toHaveBeenCalledWith({ email: 'admin@bzcom.com', password: '1234' }));
  });

  it('shows a safe authentication error returned by the context', async () => {
    login.mockRejectedValueOnce(new Error('Email or password is incorrect'));
    const user = userEvent.setup();
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'admin@bzcom.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } });
    await user.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByText('Email or password is incorrect')).toBeInTheDocument();
  });
});
