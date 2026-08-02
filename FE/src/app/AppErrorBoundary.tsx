import { Button, Result } from 'antd';
import { Component, type ErrorInfo, type ReactNode } from 'react';

interface State {
  hasError: boolean;
}

export class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    if (import.meta.env.DEV) console.error('Uncaught application error', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="500"
          title="The application could not continue"
          subTitle="Reload the page. If the problem continues, share the browser console with the team."
          extra={<Button type="primary" onClick={() => window.location.reload()}>Reload application</Button>}
        />
      );
    }
    return this.props.children;
  }
}
