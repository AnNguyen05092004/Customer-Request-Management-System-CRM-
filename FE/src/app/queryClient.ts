import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '../lib/apiError';

function shouldRetry(failureCount: number, error: Error): boolean {
  if (error instanceof ApiError && error.status && [400, 401, 403, 404, 409, 422].includes(error.status)) return false;
  return failureCount < 1;
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
      retry: shouldRetry,
    },
    mutations: { retry: false },
  },
});
