import { App, Alert, Button, Card, Col, Form, Input, Progress, Row, Select, Space, Typography } from 'antd';
import { ArrowLeft, Bot, CheckCircle2, Sparkles } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../components/PageHeader';
import { env } from '../../config/env';
import type { RequestCreateRequest } from '../../types/api';
import { categoryLabels, priorityLabels } from '../../utils/format';
import { useCreateRequest, useRequestSuggestions } from './queries';

export function RequestCreatePage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [form] = Form.useForm<RequestCreateRequest>();
  const watchedDescription = Form.useWatch((values: RequestCreateRequest) => values.description, form);
  const create = useCreateRequest();
  const suggestions = useRequestSuggestions();

  async function requestAiSuggestions() {
    try {
      const description = watchedDescription?.trim();
      if (!description) {
        message.warning('Add a description before asking AI for suggestions.');
        return;
      }
      const result = await suggestions.mutateAsync({ description });
      form.setFieldsValue({
        category: result.classification.category,
        priority: result.priority.priority,
      });
      message.success('AI suggestions applied. Review them before creating the request.');
    } catch (error) {
      if (error instanceof Error) message.error(error.message);
    }
  }

  async function submit(values: RequestCreateRequest) {
    try {
      const created = await create.mutateAsync({
        ...values,
        title: values.title.trim(),
        description: values.description?.trim() || undefined,
      });
      message.success(`Request REQ-${String(created.id).padStart(4, '0')} created successfully.`);
      void navigate('/requests');
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Unable to create this request.');
    }
  }

  return (
    <div className="page-stack request-create-page">
      <PageHeader
        eyebrow="CLIENT REQUEST"
        title="Create a request"
        description="Describe what you need. Bzcom AI can suggest a category and priority, while you keep final control."
        actions={<Button icon={<ArrowLeft size={16} />} onClick={() => void navigate('/requests')}>Back to requests</Button>}
      />

      {env.isRequestDemoMode ? (
        <Alert
          type="warning"
          showIcon
          message="Request demo mode is read-only"
          description="Switch VITE_REQUEST_DATA_MODE to api before creating a request, otherwise the new backend record would not appear in the demo adapter."
        />
      ) : null}

      <Row gutter={[20, 20]}>
        <Col xs={24} lg={16}>
          <Card className="detail-card" title="Request information">
            <Form<RequestCreateRequest>
              form={form}
              layout="vertical"
              requiredMark="optional"
              initialValues={{ category: 'INQUIRY', priority: 'MEDIUM' }}
              onFinish={(values) => void submit(values)}
            >
              <Form.Item
                name="title"
                label="Title"
                rules={[
                  { required: true, whitespace: true, message: 'Enter a request title.' },
                  { max: 200, message: 'Title must be 200 characters or fewer.' },
                ]}
              >
                <Input maxLength={200} showCount placeholder="A concise summary of the request" autoFocus />
              </Form.Item>

              <Form.Item
                name="description"
                label="Description"
                rules={[{ max: 4000, message: 'Description must be 4000 characters or fewer.' }]}
                extra="Add enough context for the team and for accurate AI suggestions."
              >
                <Input.TextArea rows={9} maxLength={4000} showCount placeholder="What happened, what did you expect, and what is the impact?" />
              </Form.Item>

              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item name="category" label="Category" rules={[{ required: true }]}>
                    <Select options={Object.entries(categoryLabels).map(([value, label]) => ({ value, label }))} />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="priority" label="Priority" rules={[{ required: true }]}>
                    <Select options={Object.entries(priorityLabels).map(([value, label]) => ({ value, label }))} />
                  </Form.Item>
                </Col>
              </Row>

              <div className="form-actions">
                <Button onClick={() => void navigate('/requests')}>Cancel</Button>
                <Button
                  icon={<Sparkles size={16} />}
                  loading={suggestions.isPending}
                  disabled={env.isRequestDemoMode}
                  onClick={() => void requestAiSuggestions()}
                >
                  Suggest with AI
                </Button>
                <Button type="primary" htmlType="submit" loading={create.isPending} disabled={env.isRequestDemoMode}>
                  Create request
                </Button>
              </div>
            </Form>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card className="detail-card ai-guidance-card" title={<Space><Bot size={18} />Bzcom AI assistant</Space>}>
              <Typography.Paragraph type="secondary">
                AI reads only the description and returns suggestions. It never creates or updates a request without your confirmation.
              </Typography.Paragraph>
              {suggestions.data ? (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Suggestion
                    label="Category"
                    value={categoryLabels[suggestions.data.classification.category]}
                    confidence={suggestions.data.classification.confidence}
                    reason={suggestions.data.classification.reason}
                  />
                  <Suggestion
                    label="Priority"
                    value={priorityLabels[suggestions.data.priority.priority]}
                    confidence={suggestions.data.priority.confidence}
                    reason={suggestions.data.priority.reason}
                  />
                  <Alert type="success" showIcon icon={<CheckCircle2 size={16} />} message="Suggestions applied" description="Review or change both fields before submitting." />
                </Space>
              ) : (
                <Alert type="info" showIcon message="You remain in control" description="Write a description, then choose Suggest with AI." />
              )}
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  );
}

function Suggestion({ label, value, confidence, reason }: { label: string; value: string; confidence: number; reason: string }) {
  const percent = Math.round(Math.min(1, Math.max(0, confidence)) * 100);
  return (
    <div className="ai-suggestion">
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Typography.Text type="secondary">{label}</Typography.Text>
        <Typography.Text strong>{value}</Typography.Text>
      </Space>
      <Progress percent={percent} size="small" strokeColor="#0052cc" />
      <Typography.Paragraph type="secondary">{reason}</Typography.Paragraph>
    </div>
  );
}
