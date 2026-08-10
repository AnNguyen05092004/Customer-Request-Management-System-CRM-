import {
  Button,
  Card,
  Grid,
  Input,
  Pagination,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  type TableProps,
} from 'antd';
import { Plus, RotateCcw, Search as SearchIcon } from 'lucide-react';
import { useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { PageHeader } from '../../components/PageHeader';
import { EmptyState, ErrorAlert, PageLoading } from '../../components/PageStates';
import { CategoryTag, PriorityLabel, StatusTag } from '../../components/ResourceTags';
import { env } from '../../config/env';
import type { Category, Priority, RequestResponse, RequestStatus } from '../../types/api';
import { formatDateTime, formatRequestId } from '../../utils/format';
import type { RequestFilter } from './api';
import { useRequests } from './queries';

const DEFAULT_PAGE_SIZE = 10;
const REQUEST_STATUSES: RequestStatus[] = ['PENDING', 'IN_PROGRESS', 'DONE'];
const CATEGORIES: Category[] = ['BUG', 'FEATURE', 'INQUIRY'];
const PRIORITIES: Priority[] = ['HIGH', 'MEDIUM', 'LOW'];

function positiveInteger(value: string | null, fallback: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function enumParam<T extends string>(value: string | null, allowed: readonly T[]): T | undefined {
  return value && allowed.includes(value as T) ? (value as T) : undefined;
}

function sortParam(value: string | null): string {
  return value && ['createdAt,asc', 'createdAt,desc', 'id,asc', 'id,desc'].includes(value)
    ? value
    : 'createdAt,desc';
}

export function RequestListPage() {
  const { role } = useAuth();
  const screens = Grid.useBreakpoint();
  const [searchParams, setSearchParams] = useSearchParams();

  const filter = useMemo<RequestFilter>(
    () => ({
      page: positiveInteger(searchParams.get('page'), 1) - 1,
      size: positiveInteger(searchParams.get('size'), DEFAULT_PAGE_SIZE),
      sort: sortParam(searchParams.get('sort')),
      keyword: searchParams.get('keyword') || undefined,
      status: enumParam(searchParams.get('status'), REQUEST_STATUSES),
      category: enumParam(searchParams.get('category'), CATEGORIES),
      priority: enumParam(searchParams.get('priority'), PRIORITIES),
    }),
    [searchParams],
  );

  const requests = useRequests(filter, role ?? 'CLIENT');

  function updateParams(changes: Record<string, string | undefined>, resetPage = true) {
    const next = new URLSearchParams(searchParams);
    Object.entries(changes).forEach(([key, value]) => {
      if (value) next.set(key, value);
      else next.delete(key);
    });
    if (resetPage) next.delete('page');
    setSearchParams(next, { replace: true });
  }

  const columns: TableProps<RequestResponse>['columns'] = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 112,
      sorter: true,
      sortOrder: filter.sort.startsWith('id,') ? (filter.sort.endsWith('asc') ? 'ascend' : 'descend') : null,
      render: (id: number) => <Typography.Text code>{formatRequestId(id)}</Typography.Text>,
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      width: 280,
      render: (title: string, request) => <Link className="request-title-link" to={`/requests/${request.id}`}>{title}</Link>,
    },
    { title: 'Category', dataIndex: 'category', key: 'category', render: (value: Category) => <CategoryTag category={value} /> },
    { title: 'Priority', dataIndex: 'priority', key: 'priority', render: (value: Priority) => <PriorityLabel priority={value} /> },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (value: RequestStatus) => <StatusTag status={value} /> },
    { title: 'Client', dataIndex: 'clientId', key: 'clientId', render: (id: number) => `Member #${id}` },
    {
      title: 'Assigned dev',
      dataIndex: 'assignedDeveloperId',
      key: 'assignedDeveloperId',
      render: (id: number | null) => id ? `Developer #${id}` : <Typography.Text type="secondary">Unassigned</Typography.Text>,
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: true,
      sortOrder: filter.sort.startsWith('createdAt,') ? (filter.sort.endsWith('asc') ? 'ascend' : 'descend') : null,
      render: (value: string) => formatDateTime(value),
    },
  ];

  function handleTableChange(
    pagination: Parameters<NonNullable<TableProps<RequestResponse>['onChange']>>[0],
    _filters: Parameters<NonNullable<TableProps<RequestResponse>['onChange']>>[1],
    sorter: Parameters<NonNullable<TableProps<RequestResponse>['onChange']>>[2],
  ) {
    const singleSorter = Array.isArray(sorter) ? sorter[0] : sorter;
    const sortField = singleSorter?.field === 'id' ? 'id' : 'createdAt';
    const direction = singleSorter?.order === 'ascend' ? 'asc' : 'desc';
    updateParams(
      {
        page: pagination.current && pagination.current > 1 ? String(pagination.current) : undefined,
        size: pagination.pageSize && pagination.pageSize !== DEFAULT_PAGE_SIZE ? String(pagination.pageSize) : undefined,
        sort: `${sortField},${direction}`,
      },
      false,
    );
  }

  const hasFilters = Boolean(filter.keyword || filter.status || filter.category || filter.priority);

  return (
    <div className="page-stack">
      <PageHeader
        title="Requests"
        description="Manage and track incoming client requests across their full lifecycle."
        actions={
          role === 'CLIENT' ? (
            <Tooltip title="Create Request will be enabled when Request mutation API is integrated.">
              <Button type="primary" icon={<Plus size={16} />} disabled>Create request</Button>
            </Tooltip>
          ) : undefined
        }
      />

      {env.isRequestDemoMode ? (
        <div className="demo-banner" role="status">
          <Tag color="gold">Demo data</Tag>
          Explicit demo mode is enabled. Filters, pagination and details use deterministic OpenAPI-shaped data.
        </div>
      ) : null}

      <section className="surface-card request-list-card">
        <div className="request-filters">
          <Input.Search
            key={filter.keyword ?? 'empty-keyword'}
            defaultValue={filter.keyword}
            onChange={(event) => {
              if (!event.target.value && filter.keyword) updateParams({ keyword: undefined });
            }}
            onSearch={(value) => updateParams({ keyword: value.trim() || undefined })}
            prefix={<SearchIcon aria-hidden size={16} />}
            allowClear
            placeholder="Search title or description"
            aria-label="Search requests by title or description"
            className="request-filters__search"
          />
          <Select<RequestStatus>
            allowClear
            value={filter.status}
            onChange={(value) => updateParams({ status: value })}
            placeholder="All statuses"
            aria-label="Filter requests by status"
            options={[
              { value: 'PENDING', label: 'Pending' },
              { value: 'IN_PROGRESS', label: 'In progress' },
              { value: 'DONE', label: 'Done' },
            ]}
          />
          <Select<Category>
            allowClear
            value={filter.category}
            onChange={(value) => updateParams({ category: value })}
            placeholder="All categories"
            aria-label="Filter requests by category"
            options={[
              { value: 'BUG', label: 'Bug' },
              { value: 'FEATURE', label: 'Feature' },
              { value: 'INQUIRY', label: 'Inquiry' },
            ]}
          />
          <Select<Priority>
            allowClear
            value={filter.priority}
            onChange={(value) => updateParams({ priority: value })}
            placeholder="All priorities"
            aria-label="Filter requests by priority"
            options={[
              { value: 'HIGH', label: 'High' },
              { value: 'MEDIUM', label: 'Medium' },
              { value: 'LOW', label: 'Low' },
            ]}
          />
          {hasFilters ? (
            <Button
              icon={<RotateCcw size={15} />}
              onClick={() => {
                setSearchParams({}, { replace: true });
              }}
            >
              Reset
            </Button>
          ) : null}
        </div>

        {requests.error ? <ErrorAlert error={requests.error} action={<Button onClick={() => void requests.refetch()}>Retry</Button>} /> : null}
        {requests.isLoading ? <PageLoading rows={6} /> : null}

        {!requests.isLoading && requests.data && requests.data.content.length === 0 ? (
          <EmptyState title="No requests found" description="Try changing or clearing the current filters." />
        ) : null}

        {!requests.isLoading && requests.data && requests.data.content.length > 0 && screens.md ? (
          <Table<RequestResponse>
            rowKey="id"
            columns={columns}
            dataSource={requests.data.content}
            loading={requests.isFetching}
            scroll={{ x: 1180 }}
            pagination={{
              current: requests.data.page + 1,
              pageSize: requests.data.size,
              total: requests.data.totalElements,
              showSizeChanger: true,
              pageSizeOptions: [5, 10, 20],
              showTotal: (total, range) => `${range[0]}–${range[1]} of ${total}`,
            }}
            onChange={handleTableChange}
          />
        ) : null}

        {!requests.isLoading && requests.data && requests.data.content.length > 0 && !screens.md ? (
          <div className="request-card-list">
            {requests.data.content.map((request) => (
              <Card key={request.id} size="small">
                <Space direction="vertical" size="small" style={{ width: '100%' }}>
                  <Space wrap><Typography.Text code>{formatRequestId(request.id)}</Typography.Text><StatusTag status={request.status} /></Space>
                  <Link className="request-title-link" to={`/requests/${request.id}`}>{request.title}</Link>
                  <Space wrap><CategoryTag category={request.category} /><PriorityLabel priority={request.priority} /></Space>
                  <Typography.Text type="secondary">{request.assignedDeveloperId ? `Developer #${request.assignedDeveloperId}` : 'Unassigned'} · {formatDateTime(request.createdAt)}</Typography.Text>
                </Space>
              </Card>
            ))}
            <Pagination
              current={requests.data.page + 1}
              pageSize={requests.data.size}
              total={requests.data.totalElements}
              hideOnSinglePage
              showSizeChanger={false}
              onChange={(page) => updateParams({ page: page > 1 ? String(page) : undefined }, false)}
            />
          </div>
        ) : null}
      </section>
    </div>
  );
}
