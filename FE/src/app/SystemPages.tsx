import { Button, Result } from 'antd';

export function ForbiddenPage() {
  return (
    <Result
      status="403"
      title="Access denied"
      subTitle="Your account does not have permission to view this page."
      extra={<Button type="primary" href="/requests">Back to requests</Button>}
    />
  );
}

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="Page not found"
      subTitle="The page you requested does not exist."
      extra={<Button type="primary" href="/requests">Back to requests</Button>}
    />
  );
}
