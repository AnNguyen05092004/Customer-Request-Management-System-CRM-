import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/apiError';
import type { AssignRequest, Role, StatusUpdateRequest } from '../../types/api';
import type { RequestFilter } from './api';
import {
  assignRequest,
  fetchRequest,
  fetchRequestHistory,
  fetchRequests,
  fetchRequestSummary,
  updateRequestStatus,
} from './api';

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

function useWorkflowInvalidation() {
  const queryClient = useQueryClient();
  const invalidateWorkflow = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: requestKeys.all }),
      queryClient.invalidateQueries({ queryKey: ['alerts'] }),
    ]);
  };
  const refreshOnConflict = (error: Error) => {
    if (error instanceof ApiError && error.status === 409) {
      void queryClient.invalidateQueries({ queryKey: requestKeys.all });
    }
  };
  return { invalidateWorkflow, refreshOnConflict };
}

export function useAssignRequest(id: number) {
  const { invalidateWorkflow, refreshOnConflict } = useWorkflowInvalidation();
  return useMutation({
    mutationFn: (request: AssignRequest) => assignRequest(id, request),
    onSuccess: invalidateWorkflow,
    onError: refreshOnConflict,
  });
}

export function useUpdateRequestStatus(id: number) {
  const { invalidateWorkflow, refreshOnConflict } = useWorkflowInvalidation();
  return useMutation({
    mutationFn: (request: StatusUpdateRequest) => updateRequestStatus(id, request),
    onSuccess: invalidateWorkflow,
    onError: refreshOnConflict,
  });
}

export function useRequestSummary(id: number) {
  return useMutation({ mutationFn: () => fetchRequestSummary(id) });
}
