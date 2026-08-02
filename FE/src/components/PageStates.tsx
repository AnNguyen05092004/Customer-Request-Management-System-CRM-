import { Alert, Button, Empty, Result, Skeleton } from 'antd';
import type { ReactNode } from 'react';
import { ApiError } from '../lib/apiError';

export function PageLoading({ rows = 4 }: { rows?: number }) {
  return (
    <div className="surface-card page-state" aria-label="Loading">
      <Skeleton active paragraph={{ rows }} />
    </div>
  );
}

export function ErrorAlert({ error, action }: { error: unknown; action?: ReactNode }) {
  const message = error instanceof Error ? error.message : 'Unable to load this content.';
  return <Alert type="error" showIcon message="Something went wrong" description={message} action={action} />;
}

export function EmptyState({ title, description }: { title: string; description?: string }) {
  return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<><strong>{title}</strong>{description ? <p>{description}</p> : null}</>} />;
}

export function ResourceError({ error, resource = 'resource' }: { error: unknown; resource?: string }) {
  if (error instanceof ApiError && error.status === 404) {
    return (
      <Result
        status="404"
        title="Not found"
        subTitle={`The requested ${resource} does not exist or is no longer available.`}
        extra={<Button type="primary" href="/requests">Back to requests</Button>}
      />
    );
  }
  if (error instanceof ApiError && error.status === 403) {
    return (
      <Result
        status="403"
        title="Access denied"
        subTitle={`Your account does not have permission to view this ${resource}.`}
        extra={<Button type="primary" href="/requests">Back to requests</Button>}
      />
    );
  }
  return <ErrorAlert error={error} />;
}
