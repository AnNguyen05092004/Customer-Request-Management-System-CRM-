import { ArrowLeft, Bot, CalendarDays, Clock3, UserRound, UsersRound } from 'lucide-react';
import { Button, Card, Col, Descriptions, Divider, Grid, Row, Space, Tag, Timeline, Tooltip, Typography } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { PageHeader } from '../../components/PageHeader';
import { ErrorAlert, PageLoading, ResourceError } from '../../components/PageStates';
import { CategoryTag, PriorityLabel, StatusTag } from '../../components/ResourceTags';
import { env } from '../../config/env';
import { formatDateTime, formatRequestId, statusLabels } from '../../utils/format';
import { useRequest, useRequestHistory } from './queries';

export function RequestDetailPage() {
  const { id: rawId } = useParams();
  const navigate = useNavigate();
  const id = Number(rawId);
  const { role } = useAuth();
  const screens = Grid.useBreakpoint();
  const validId = Number.isInteger(id) && id > 0;
  const request = useRequest(validId ? id : 0, role ?? 'CLIENT');
  const history = useRequestHistory(validId ? id : 0, role ?? 'CLIENT');

  if (!validId) return <ResourceError error={new Error('Invalid request ID.')} resource="request" />;
  if (request.isLoading) return <PageLoading rows={8} />;
  if (request.error || !request.data) return <ResourceError error={request.error} resource="request" />;

  const item = request.data;
  const canManageWorkflow = role === 'ADMIN' || role === 'DEVELOPER';

  return (
    <div className="page-stack request-detail-page">
      <PageHeader
        eyebrow={`${item.category} REQUEST · ${formatRequestId(item.id)}`}
        title={item.title}
        description={`Last updated ${formatDateTime(item.updatedAt)}`}
        actions={<Button icon={<ArrowLeft size={16} />} onClick={() => void navigate('/requests')}>Back to requests</Button>}
      />

      {env.isRequestDemoMode ? (
        <div className="demo-banner" role="status">
          <Tag color="gold">Demo data</Tag>
          Read-only request detail based on the planned OpenAPI contract.
        </div>
      ) : null}

      <Row gutter={[20, 20]}>
        <Col xs={24} xl={16}>
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card className="detail-card" title="Request details">
              <Typography.Paragraph className="request-description">
                {item.description || 'No description was provided.'}
              </Typography.Paragraph>
              <Divider />
              <Descriptions column={{ xs: 1, sm: 2 }} size={screens.sm ? 'middle' : 'small'}>
                <Descriptions.Item label="Category"><CategoryTag category={item.category} /></Descriptions.Item>
                <Descriptions.Item label="Priority"><PriorityLabel priority={item.priority} /></Descriptions.Item>
                <Descriptions.Item label={<Space><UserRound size={15} />Client</Space>}>Member #{item.clientId}</Descriptions.Item>
                <Descriptions.Item label={<Space><UsersRound size={15} />Assigned developer</Space>}>
                  {item.assignedDeveloperId ? `Developer #${item.assignedDeveloperId}` : <Typography.Text type="secondary">Unassigned</Typography.Text>}
                </Descriptions.Item>
                <Descriptions.Item label={<Space><CalendarDays size={15} />Created</Space>}>{formatDateTime(item.createdAt)}</Descriptions.Item>
                <Descriptions.Item label="Version">{item.version}</Descriptions.Item>
              </Descriptions>
            </Card>

            <Card className="detail-card" title="Request timeline">
              {history.isLoading ? <PageLoading rows={3} /> : null}
              {history.error ? <ErrorAlert error={history.error} /> : null}
              {!history.isLoading && history.data?.length ? (
                <Timeline
                  items={history.data.map((entry) => ({
                    color: entry.toStatus === 'DONE' ? 'green' : entry.toStatus === 'IN_PROGRESS' ? 'blue' : 'gray',
                    children: (
                      <div className="timeline-entry">
                        <div>
                          <strong>
                            {entry.fromStatus === entry.toStatus
                              ? 'Assignment updated'
                              : entry.toStatus
                                ? `Status changed to ${statusLabels[entry.toStatus]}`
                                : 'Request updated'}
                          </strong>
                          <Typography.Paragraph type="secondary">{entry.memo || 'No memo provided.'}</Typography.Paragraph>
                          <Tag>Member #{entry.changedBy}</Tag>
                        </div>
                        <Typography.Text type="secondary">{formatDateTime(entry.changedAt)}</Typography.Text>
                      </div>
                    ),
                  }))}
                />
              ) : null}
              {!history.isLoading && !history.data?.length ? (
                <Typography.Text type="secondary">
                  No workflow history is available for this request yet.
                </Typography.Text>
              ) : null}
            </Card>
          </Space>
        </Col>

        <Col xs={24} xl={8}>
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card className="detail-card" title="Workflow state">
              <div className="workflow-status">
                <Typography.Text type="secondary">Current status</Typography.Text>
                <StatusTag status={item.status} />
              </div>
              <Divider />
              {canManageWorkflow ? (
                <Tooltip title="Status mutation will be connected by the Workflow feature owner.">
                  <Button block disabled>Update status</Button>
                </Tooltip>
              ) : (
                <Typography.Text type="secondary">Your role has read-only workflow access.</Typography.Text>
              )}
            </Card>

            {role === 'ADMIN' ? (
              <Card className="detail-card" title={<Space>Assignment <Tag>ADMIN ONLY</Tag></Space>}>
                <Typography.Paragraph>
                  {item.assignedDeveloperId ? `Currently assigned to developer #${item.assignedDeveloperId}.` : 'This request is unassigned.'}
                </Typography.Paragraph>
                <Tooltip title="Assignment mutation will be connected by the Workflow feature owner.">
                  <Button block disabled>Assign developer</Button>
                </Tooltip>
              </Card>
            ) : null}

            <Card className="detail-card ai-placeholder" title={<Space><Bot size={18} />Bzcom AI insights</Space>}>
              <Typography.Paragraph type="secondary">
                The backend summary API is available; this action will be enabled when the frontend control is connected.
              </Typography.Paragraph>
              <Button icon={<Bot size={16} />} block disabled>Generate summary</Button>
            </Card>

            <Card size="small">
              <Space><Clock3 size={16} /><Typography.Text type="secondary">Optimistic version {item.version}</Typography.Text></Space>
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  );
}
