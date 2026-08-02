import { QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import 'antd/dist/reset.css';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './app/App';
import { AppErrorBoundary } from './app/AppErrorBoundary';
import { queryClient } from './app/queryClient';
import { AuthProvider } from './auth/AuthContext';
import './styles/global.css';

const root = document.getElementById('root');
if (!root) throw new Error('Root element was not found.');

createRoot(root).render(
  <StrictMode>
    <AppErrorBoundary>
      <ConfigProvider
        theme={{
          token: {
            colorPrimary: '#003d9b',
            colorInfo: '#0c56d0',
            colorSuccess: '#36b37e',
            colorWarning: '#ffab00',
            colorError: '#ba1a1a',
            colorBgLayout: '#f4f5f7',
            colorText: '#191b23',
            colorBorderSecondary: '#dfe1e6',
            borderRadius: 4,
            borderRadiusLG: 8,
            fontFamily: "Inter, ui-sans-serif, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
          },
          components: {
            Button: { controlHeight: 40, fontWeight: 600 },
            Input: { controlHeight: 40 },
            Select: { controlHeight: 40 },
            Table: { headerBg: '#f7f8fa', headerColor: '#313544' },
            Layout: { bodyBg: '#f4f5f7', headerBg: '#ffffff', siderBg: '#ffffff' },
          },
        }}
      >
        <AntdApp>
          <QueryClientProvider client={queryClient}>
            <BrowserRouter>
              <AuthProvider>
                <App />
              </AuthProvider>
            </BrowserRouter>
          </QueryClientProvider>
        </AntdApp>
      </ConfigProvider>
    </AppErrorBoundary>
  </StrictMode>,
);
