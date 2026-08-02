import { Alert, Button, Card, Form, Input, Result, Space, Typography } from 'antd';
import { LockKeyhole, Mail, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toApiError } from '../../lib/apiError';
import type { MemberCreateRequest } from '../../types/api';
import { useRegisterMember } from './queries';

export function RegisterPage() {
  const registration = useRegisterMember();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(values: MemberCreateRequest) {
    setError(null);
    try {
      await registration.mutateAsync(values);
    } catch (submitError) {
      setError(toApiError(submitError).message);
    }
  }

  if (registration.isSuccess) {
    return (
      <main className="auth-page">
        <Card className="auth-card auth-card--result">
          <Result
            status="success"
            title="Client account created"
            subTitle={`Welcome, ${registration.data.name}. You can now sign in with ${registration.data.email}.`}
            extra={<Button type="primary" onClick={() => void navigate('/login')}>Continue to login</Button>}
          />
        </Card>
      </main>
    );
  }

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <div className="auth-brand auth-brand--compact">
          <div className="auth-brand__mark">B</div>
          <Typography.Title level={2}>Create client account</Typography.Title>
          <Typography.Text type="secondary">Public registration always creates the CLIENT role.</Typography.Text>
        </div>

        {error ? <Alert type="error" showIcon message={error} closable onClose={() => setError(null)} /> : null}

        <Form<MemberCreateRequest>
          layout="vertical"
          requiredMark={false}
          size="large"
          onFinish={(values) => void handleSubmit(values)}
        >
          <Form.Item
            label="Full name"
            name="name"
            rules={[
              { required: true, whitespace: true, message: 'Enter your full name.' },
              { max: 100, message: 'Name cannot exceed 100 characters.' },
            ]}
          >
            <Input prefix={<UserRound aria-hidden size={17} />} autoComplete="name" />
          </Form.Item>
          <Form.Item
            label="Email address"
            name="email"
            rules={[
              { required: true, message: 'Enter your email address.' },
              { type: 'email', message: 'Enter a valid email address.' },
              { max: 255, message: 'Email cannot exceed 255 characters.' },
            ]}
          >
            <Input prefix={<Mail aria-hidden size={17} />} autoComplete="email" />
          </Form.Item>
          <Form.Item
            label="Password"
            name="password"
            extra="Use 4–72 characters for this assignment environment."
            rules={[
              { required: true, message: 'Enter a password.' },
              { min: 4, message: 'Password must contain at least 4 characters.' },
              { max: 72, message: 'Password cannot exceed 72 characters.' },
            ]}
          >
            <Input.Password prefix={<LockKeyhole aria-hidden size={17} />} autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={registration.isPending}>
            Create account
          </Button>
        </Form>

        <Space className="auth-card__secondary">
          <Typography.Text type="secondary">Already registered?</Typography.Text>
          <Link to="/login">Back to login</Link>
        </Space>
      </Card>
    </main>
  );
}
