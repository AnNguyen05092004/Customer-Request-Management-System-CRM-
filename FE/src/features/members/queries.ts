import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { MemberCreateRequest } from '../../types/api';
import { fetchMember, fetchMembers, registerMember } from './api';

export const memberKeys = {
  all: ['members'] as const,
  detail: (id: number) => ['members', id] as const,
};

export function useMembers(enabled = true) {
  return useQuery({ queryKey: memberKeys.all, queryFn: fetchMembers, enabled });
}

export function useMember(id: number) {
  return useQuery({ queryKey: memberKeys.detail(id), queryFn: () => fetchMember(id), enabled: id > 0 });
}

export function useRegisterMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: MemberCreateRequest) => registerMember(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: memberKeys.all });
    },
  });
}
