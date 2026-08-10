import { useQuery } from '@tanstack/react-query';
import { fetchRequestStats } from './api';

export const statsKeys = { all: ['request-stats'] as const };

export function useRequestStats() {
  return useQuery({ queryKey: statsKeys.all, queryFn: ({ signal }) => fetchRequestStats(signal) });
}
