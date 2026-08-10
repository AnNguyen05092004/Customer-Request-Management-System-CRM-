import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd';
import { LockKeyhole, Mail } from 'lucide-react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { toApiError } from '../../lib/apiError';
import type { LoginRequest } from '../../types/api';

interface LoginLocationState {
  from?: string;
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const from = (location.state as LoginLocationState | null)?.from ?? '/requests';

  async function handleSubmit(values: LoginRequest) {
    setError(null);
    setIsSubmitting(true);
    try {
      await login(values);
      void navigate(from, { replace: true });
    } catch (submitError) {
      setError(toApiError(submitError).message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <Card className="auth-card" variant="outlined">
        <div className="auth-brand">
          <div className="auth-brand__mark">B</div>
          <Typography.Title level={1}>Bzcom</Typography.Title>
          <Typography.Text type="secondary">Enterprise CRM</Typography.Text>
        </div>

        <div className="auth-card__intro">
          <Typography.Title level={3}>Welcome back</Typography.Title>
          <Typography.Text type="secondary">Sign in to manage customer requests.</Typography.Text>
        </div>

        {error ? <Alert type="error" showIcon message={error} closable onClose={() => setError(null)} /> : null}

        <Form<LoginRequest>
          layout="vertical"
          requiredMark={false}
          size="large"
          onFinish={(values) => void handleSubmit(values)}
        >
          <Form.Item
            label="Email address"
            name="email"
            rules={[
              { required: true, message: 'Enter your email address.' },
              { type: 'email', message: 'Enter a valid email address.' },
            ]}
          >
            <Input prefix={<Mail aria-hidden size={17} />} autoComplete="email" placeholder="you@bzcom.com" />
          </Form.Item>
          <Form.Item
            label="Password"
            name="password"
            rules={[{ required: true, message: 'Enter your password.' }]}
          >
            <Input.Password
              prefix={<LockKeyhole aria-hidden size={17} />}
              autoComplete="current-password"
              placeholder="Your password"
            />
          </Form.Item>
          <Form.Item className="auth-card__submit">
            <Button type="primary" htmlType="submit" block loading={isSubmitting}>
              Log in
            </Button>
          </Form.Item>
        </Form>

        <Space direction="vertical" className="auth-card__secondary" size="middle">
          <Typography.Text type="secondary">New customer?</Typography.Text>
          <Button block onClick={() => void navigate('/register')}>Register as client</Button>
        </Space>

        <div className="demo-credentials">
          <strong>Demo account</strong>
          <span>admin@bzcom.com · password 1234</span>
        </div>
      </Card>
      <Typography.Text type="secondary" className="auth-footer">Secure access · Bzcom CRM</Typography.Text>
    </main>
  );
}
