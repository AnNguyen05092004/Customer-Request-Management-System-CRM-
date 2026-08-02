import dayjs from 'dayjs';
import type { Category, Priority, RequestStatus, Role } from '../types/api';

export const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrator',
  DEVELOPER: 'Developer',
  CLIENT: 'Client',
};

export const statusLabels: Record<RequestStatus, string> = {
  PENDING: 'Pending',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
};

export const categoryLabels: Record<Category, string> = {
  BUG: 'Bug',
  FEATURE: 'Feature',
  INQUIRY: 'Inquiry',
};

export const priorityLabels: Record<Priority, string> = {
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
};

export function formatDateTime(value: string): string {
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('MMM D, YYYY · HH:mm') : '—';
}

export function formatRequestId(id: number): string {
  return `REQ-${String(id).padStart(4, '0')}`;
}
