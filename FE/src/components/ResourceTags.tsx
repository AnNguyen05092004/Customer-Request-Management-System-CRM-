import { ArrowDown, ArrowUp, Minus } from 'lucide-react';
import { Tag } from 'antd';
import type { Category, Priority, RequestStatus, Role } from '../types/api';
import { categoryLabels, priorityLabels, roleLabels, statusLabels } from '../utils/format';

const statusColors: Record<RequestStatus, string> = {
  PENDING: 'default',
  IN_PROGRESS: 'blue',
  DONE: 'green',
};

const categoryColors: Record<Category, string> = {
  BUG: 'red',
  FEATURE: 'blue',
  INQUIRY: 'purple',
};

const roleColors: Record<Role, string> = {
  ADMIN: 'geekblue',
  DEVELOPER: 'cyan',
  CLIENT: 'default',
};

export function StatusTag({ status }: { status: RequestStatus }) {
  return <Tag color={statusColors[status]}>{statusLabels[status]}</Tag>;
}

export function CategoryTag({ category }: { category: Category }) {
  return <Tag color={categoryColors[category]}>{categoryLabels[category]}</Tag>;
}

export function RoleTag({ role }: { role: Role }) {
  return <Tag color={roleColors[role]}>{roleLabels[role]}</Tag>;
}

export function PriorityLabel({ priority }: { priority: Priority }) {
  const Icon = priority === 'HIGH' ? ArrowUp : priority === 'LOW' ? ArrowDown : Minus;
  return (
    <span className={`priority priority--${priority.toLowerCase()}`}>
      <Icon aria-hidden size={15} />
      {priorityLabels[priority]}
    </span>
  );
}
