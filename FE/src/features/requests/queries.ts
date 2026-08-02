import { keepPreviousData, useQuery } from '@tanstack/react-query';
import type { Role } from '../../types/api';
import type { RequestFilter } from './api';
import { fetchRequest, fetchRequestHistory, fetchRequests } from './api';

export const requestKeys = {
  all: ['requests'] as const,
  list: (filter: RequestFilter, role: Role) => ['requests', 'list', role, filter] as const,
  detail: (id: number, role: Role) => ['requests', 'detail', role, id] as const,
  history: (id: number, role: Role) => ['requests', 'history', role, id] as const,
};

export function useRequests(filter: RequestFilter, role: Role) {
  return useQuery({
    queryKey: requestKeys.list(filter, role),
    queryFn: ({ signal }) => fetchRequests(filter, signal, role),
    placeholderData: keepPreviousData,
  });
}

export function useRequest(id: number, role: Role) {
  return useQuery({
    queryKey: requestKeys.detail(id, role),
    queryFn: ({ signal }) => fetchRequest(id, signal, role),
    enabled: id > 0,
  });
}

export function useRequestHistory(id: number, role: Role) {
  return useQuery({
    queryKey: requestKeys.history(id, role),
    queryFn: ({ signal }) => fetchRequestHistory(id, signal, role),
    enabled: id > 0,
  });
}
