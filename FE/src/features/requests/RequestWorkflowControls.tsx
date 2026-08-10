import { App, Alert, Button, Form, Input, Modal, Radio, Select, Space, Typography } from 'antd';
import { Bot, UserRoundCog } from 'lucide-react';
import { useState } from 'react';
import { useMembers } from '../members/queries';
import { env } from '../../config/env';
import type { AssignRequest, RequestResponse, RequestStatus, Role } from '../../types/api';
import { statusLabels } from '../../utils/format';
import { useAssignRequest, useRequestSummary, useUpdateRequestStatus } from './queries';
import { allowedNextStatuses } from './workflow';

interface AssignmentValues {
  mode: 'auto' | 'manual';
  developerId?: number;
}

interface StatusValues {
  status: RequestStatus;
  memo?: string;
}

export function RequestWorkflowControls({ request, role }: { request: RequestResponse; role: Role | null }) {
  const { message } = App.useApp();
  const [assignOpen, setAssignOpen] = useState(false);
  const [statusOpen, setStatusOpen] = useState(false);
  const [assignForm] = Form.useForm<AssignmentValues>();
  const [statusForm] = Form.useForm<StatusValues>();
  const assignmentMode = Form.useWatch('mode', assignForm);
  const members = useMembers(role === 'ADMIN' && assignOpen && !env.isRequestDemoMode);
  const assign = useAssignRequest(request.id);
  const updateStatus = useUpdateRequestStatus(request.id);
  const summary = useRequestSummary(request.id);
  const nextStatuses = allowedNextStatuses(request.status);
  const nextStatus = nextStatuses[0];
  const canUpdateStatus = (role === 'ADMIN' || role === 'DEVELOPER') && request.assignedDeveloperId !== null && nextStatuses.length > 0;

  async function submitAssignment(values: AssignmentValues) {
    const developerId = values.developerId;
    if (values.mode === 'manual' && !developerId) {
      message.error('Select a developer.');
      return;
    }
    const payload: AssignRequest = values.mode === 'auto'
      ? { auto: true, expectedVersion: request.version }
      : { auto: false, developerId: developerId as number, expectedVersion: request.version };
    try {
      await assign.mutateAsync(payload);
      message.success(values.mode === 'auto' ? 'Request auto-assigned successfully.' : 'Developer assigned successfully.');
      setAssignOpen(false);
      assignForm.resetFields();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Unable to assign this request.');
    }
  }

  async function submitStatus(values: StatusValues) {
    try {
      await updateStatus.mutateAsync({
        status: values.status,
        memo: values.memo?.trim() || undefined,
        expectedVersion: request.version,
      });
      message.success(`Status updated to ${statusLabels[values.status]}.`);
      setStatusOpen(false);
      statusForm.resetFields();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Unable to update the status.');
    }
  }

  async function generateSummary() {
    try {
      await summary.mutateAsync();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Unable to generate a summary.');
    }
  }

  return (
    <>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {(role === 'ADMIN' || role === 'DEVELOPER') ? (
          <Button
            type="primary"
            block
            disabled={!canUpdateStatus || env.isRequestDemoMode}
            onClick={() => {
              if (nextStatus) statusForm.setFieldsValue({ status: nextStatus });
              setStatusOpen(true);
            }}
          >
            {request.status === 'DONE'
              ? 'Workflow completed'
              : request.assignedDeveloperId === null
                ? 'Assign before updating status'
                : nextStatus
                  ? `Move to ${statusLabels[nextStatus]}`
                  : 'No available transition'}
          </Button>
        ) : (
          <Typography.Text type="secondary">Your role has read-only workflow access.</Typography.Text>
        )}
        {role === 'ADMIN' ? (
          <Button
            icon={<UserRoundCog size={16} />}
            block
            disabled={env.isRequestDemoMode}
            onClick={() => {
              assignForm.setFieldsValue({ mode: 'auto' });
              setAssignOpen(true);
            }}
          >
            {request.assignedDeveloperId ? 'Reassign developer' : 'Assign developer'}
          </Button>
        ) : null}
        <Button
          icon={<Bot size={16} />}
          block
          disabled={env.isRequestDemoMode}
          loading={summary.isPending}
          onClick={() => void generateSummary()}
        >
          {summary.data ? 'Regenerate summary' : 'Generate summary'}
        </Button>
        {summary.data ? <Alert type="info" showIcon message="AI summary" description={summary.data.summary} /> : null}
      </Space>

      <Modal
        title="Assign developer"
        open={assignOpen}
        okText="Assign"
        confirmLoading={assign.isPending}
        onCancel={() => setAssignOpen(false)}
        onOk={() => void assignForm.submit()}
        destroyOnHidden
      >
        <Typography.Paragraph type="secondary">
          Auto-assignment chooses the developer with the lightest active workload. Manual assignment lets you select a specific developer.
        </Typography.Paragraph>
        <Form form={assignForm} layout="vertical" initialValues={{ mode: 'auto' }} onFinish={(values) => void submitAssignment(values)}>
          <Form.Item name="mode" label="Assignment method" rules={[{ required: true }]}>
            <Radio.Group>
              <Radio value="auto">Auto-assign</Radio>
              <Radio value="manual">Choose manually</Radio>
            </Radio.Group>
          </Form.Item>
          {assignmentMode === 'manual' ? (
            <Form.Item name="developerId" label="Developer" rules={[{ required: true, message: 'Select a developer.' }]}>
              <Select
                showSearch
                loading={members.isLoading}
                optionFilterProp="label"
                placeholder="Select a developer"
                options={(members.data ?? []).filter((member) => member.role === 'DEVELOPER').map((member) => ({
                  value: member.id,
                  label: `${member.name} · ${member.email}`,
                }))}
              />
            </Form.Item>
          ) : null}
          {members.error ? <Alert type="error" showIcon message="Unable to load developers" description={members.error.message} /> : null}
        </Form>
      </Modal>

      <Modal
        title="Update request status"
        open={statusOpen}
        okText="Update status"
        confirmLoading={updateStatus.isPending}
        onCancel={() => setStatusOpen(false)}
        onOk={() => void statusForm.submit()}
        destroyOnHidden
      >
        <Form form={statusForm} layout="vertical" onFinish={(values) => void submitStatus(values)}>
          <Form.Item name="status" label="Next status" rules={[{ required: true }]}>
            <Select options={nextStatuses.map((status) => ({ value: status, label: statusLabels[status] }))} />
          </Form.Item>
          <Form.Item name="memo" label="Work note" rules={[{ max: 255, message: 'Work note must be 255 characters or fewer.' }]}>
            <Input.TextArea rows={4} maxLength={255} showCount placeholder="Optional context for the request history" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
