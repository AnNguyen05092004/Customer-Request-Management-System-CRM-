import { App, Button, Card, List, Segmented, Space, Tag, Typography } from 'antd';
import { Bell, CheckCheck, ExternalLink } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../components/PageHeader';
import { EmptyState, ErrorAlert, PageLoading } from '../../components/PageStates';
import type { AlertResponse, AlertType } from '../../types/api';
import { formatDateTime, formatRequestId } from '../../utils/format';
import { useAlerts, useMarkAlertRead } from './queries';

type AlertFilter = 'all' | 'unread' | 'read';

const alertTypeLabels: Record<AlertType, string> = {
  ASSIGNED: 'Assignment',
  STATUS_CHANGED: 'Status change',
  HIGH_PRIORITY_REGISTERED: 'High priority',
};

export function AlertListPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [filter, setFilter] = useState<AlertFilter>('all');
  const isRead = filter === 'all' ? undefined : filter === 'read';
  const alerts = useAlerts(isRead, true);
  const markRead = useMarkAlertRead();

  async function openAlert(alert: AlertResponse) {
    try {
      if (!alert.isRead) await markRead.mutateAsync(alert.id);
      void navigate(`/requests/${alert.requestId}`);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Unable to open this alert.');
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Notifications"
        description="Follow request assignments, workflow changes and new high-priority requests."
        actions={(
          <Segmented<AlertFilter>
            value={filter}
            onChange={setFilter}
            options={[
              { label: 'All', value: 'all' },
              { label: 'Unread', value: 'unread' },
              { label: 'Read', value: 'read' },
            ]}
          />
        )}
      />
      <Card className="detail-card alert-list-card">
        {alerts.isLoading ? <PageLoading rows={6} /> : null}
        {alerts.error ? <ErrorAlert error={alerts.error} action={<Button onClick={() => void alerts.refetch()}>Retry</Button>} /> : null}
        {!alerts.isLoading && !alerts.error && !alerts.data?.length ? (
          <EmptyState title="No notifications" description="There are no notifications in this view." />
        ) : null}
        {alerts.data?.length ? (
          <List
            dataSource={alerts.data}
            renderItem={(alert) => (
              <List.Item
                className={alert.isRead ? 'notification-row' : 'notification-row notification-row--unread'}
                actions={[
                  <Button key="open" type="link" icon={<ExternalLink size={15} />} onClick={() => void openAlert(alert)}>Open request</Button>,
                ]}
              >
                <List.Item.Meta
                  avatar={<div className="notification-row__icon">{alert.isRead ? <CheckCheck size={19} /> : <Bell size={19} />}</div>}
                  title={<Space wrap><Typography.Text strong={!alert.isRead}>{alert.message}</Typography.Text><Tag>{alertTypeLabels[alert.alertType]}</Tag></Space>}
                  description={<Space wrap><Typography.Text>{formatRequestId(alert.requestId)}</Typography.Text><Typography.Text type="secondary">{formatDateTime(alert.createdAt)}</Typography.Text></Space>}
                />
              </List.Item>
            )}
          />
        ) : null}
      </Card>
    </div>
  );
}
