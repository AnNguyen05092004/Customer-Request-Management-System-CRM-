import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchAlerts, markAlertRead } from './api';

export const alertKeys = {
  all: ['alerts'] as const,
  list: (isRead?: boolean) => ['alerts', 'list', isRead ?? 'all'] as const,
};

export function useAlerts(isRead?: boolean, poll = false) {
  return useQuery({
    queryKey: alertKeys.list(isRead),
    queryFn: ({ signal }) => fetchAlerts(isRead, signal),
    refetchInterval: poll ? 15_000 : false,
  });
}

export function useMarkAlertRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAlertRead,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: alertKeys.all });
    },
  });
}
