import { App, Badge, Button, Divider, Empty, List, Popover, Spin, Typography } from 'antd';
import { Bell, ExternalLink } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { AlertResponse } from '../../types/api';
import { formatDateTime } from '../../utils/format';
import { useAlerts, useMarkAlertRead } from './queries';

export function AlertBell() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const alerts = useAlerts(undefined, true);
  const markRead = useMarkAlertRead();
  const unreadCount = (alerts.data ?? []).filter((alert) => !alert.isRead).length;

  async function openAlert(alert: AlertResponse) {
    try {
      if (!alert.isRead) await markRead.mutateAsync(alert.id);
      setOpen(false);
      void navigate(`/requests/${alert.requestId}`);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Unable to open this alert.');
    }
  }

  const content = (
    <div className="alert-popover">
      <div className="alert-popover__header">
        <div>
          <Typography.Text strong>Notifications</Typography.Text>
          <Typography.Text type="secondary">{unreadCount} unread</Typography.Text>
        </div>
        <Button type="link" size="small" onClick={() => { setOpen(false); void navigate('/alerts'); }}>View all</Button>
      </div>
      <Divider />
      {alerts.isLoading ? <div className="alert-popover__loading"><Spin /></div> : null}
      {alerts.error ? <Typography.Text type="danger">{alerts.error.message}</Typography.Text> : null}
      {!alerts.isLoading && !alerts.error && !alerts.data?.length ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No notifications" /> : null}
      {alerts.data?.length ? (
        <List
          dataSource={alerts.data.slice(0, 5)}
          renderItem={(alert) => (
            <List.Item className={alert.isRead ? 'alert-item' : 'alert-item alert-item--unread'}>
              <button type="button" className="alert-item__button" onClick={() => void openAlert(alert)}>
                <span className="alert-item__dot" aria-hidden />
                <span>
                  <Typography.Text strong={!alert.isRead}>{alert.message}</Typography.Text>
                  <Typography.Text type="secondary">{formatDateTime(alert.createdAt)}</Typography.Text>
                </span>
                <ExternalLink size={14} aria-hidden />
              </button>
            </List.Item>
          )}
        />
      ) : null}
    </div>
  );

  return (
    <Popover trigger="click" placement="bottomRight" open={open} onOpenChange={setOpen} content={content}>
      <Badge count={unreadCount} size="small" overflowCount={99}>
        <Button type="text" aria-label={`Alerts${unreadCount ? `, ${unreadCount} unread` : ''}`} icon={<Bell />} />
      </Badge>
    </Popover>
  );
}
