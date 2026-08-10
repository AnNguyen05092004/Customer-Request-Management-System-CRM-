import type { RequestStatus } from '../../types/api';

const transitions: Record<RequestStatus, readonly RequestStatus[]> = {
  PENDING: ['IN_PROGRESS'],
  IN_PROGRESS: ['DONE'],
  DONE: [],
};

export function allowedNextStatuses(status: RequestStatus): readonly RequestStatus[] {
  return transitions[status];
}
