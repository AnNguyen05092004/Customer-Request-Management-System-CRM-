import {
  Bot,
  ChevronDown,
  LogOut,
  Menu as MenuIcon,
  Search,
  SquarePlus,
  TicketCheck,
  Users,
} from 'lucide-react';
import { Avatar, Button, Drawer, Grid, Input, Layout, Menu, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { roleLabels } from '../utils/format';
import { AlertBell } from '../features/alerts/AlertBell';

const { Header, Sider, Content } = Layout;

function Brand() {
  return (
    <div className="brand" aria-label="Bzcom CRM home">
      <div className="brand__mark">B</div>
      <div>
        <strong>Bzcom</strong>
        <span>Enterprise CRM</span>
      </div>
    </div>
  );
}

export function AppLayout() {
  const { role, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const screens = Grid.useBreakpoint();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!userMenuOpen) return;

    function closeOnOutsideClick(event: PointerEvent) {
      if (!userMenuRef.current?.contains(event.target as Node)) setUserMenuOpen(false);
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') setUserMenuOpen(false);
    }

    document.addEventListener('pointerdown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [userMenuOpen]);

  const menuItems = useMemo(
    () => [
      { key: '/requests', icon: <TicketCheck size={18} />, label: 'Requests' },
      ...(role === 'CLIENT' ? [{ key: '/requests/new', icon: <SquarePlus size={18} />, label: 'Create request' }] : []),
      ...(role === 'ADMIN' ? [{ key: '/members', icon: <Users size={18} />, label: 'Members' }] : []),
    ],
    [role],
  );

  const selectedKey = location.pathname === '/requests/new'
    ? '/requests/new'
    : location.pathname.startsWith('/members') ? '/members' : '/requests';
  const navigation = (
    <Menu
      mode="inline"
      selectedKeys={[selectedKey]}
      items={menuItems}
      onClick={({ key }) => {
        void navigate(key);
        setDrawerOpen(false);
      }}
    />
  );

  async function handleLogout() {
    setUserMenuOpen(false);
    await logout();
    void navigate('/login', { replace: true });
  }

  return (
    <Layout className="app-shell">
      {screens.lg ? (
        <Sider width={252} theme="light" className="app-sider">
          <Brand />
          <nav aria-label="Primary navigation">{navigation}</nav>
          <div className="app-sider__footer">
            <Avatar size="small">{role?.charAt(0)}</Avatar>
            <div>
              <strong>{role ? roleLabels[role] : 'User'}</strong>
              <span>Signed in</span>
            </div>
          </div>
        </Sider>
      ) : (
        <Drawer
          placement="left"
          width={276}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          styles={{ body: { padding: 0 } }}
        >
          <Brand />
          <nav aria-label="Primary navigation">{navigation}</nav>
        </Drawer>
      )}

      <Layout>
        <Header className="app-header">
          <Space size="middle" className="app-header__start">
            {!screens.lg ? (
              <Button type="text" aria-label="Open navigation" icon={<MenuIcon />} onClick={() => setDrawerOpen(true)} />
            ) : null}
            <div className="app-header__mobile-brand">Bzcom CRM</div>
            <Input
              className="global-search"
              prefix={<Search aria-hidden size={17} />}
              placeholder="Search requests..."
              aria-label="Global search (coming soon)"
              disabled
            />
          </Space>
          <Space size="small">
            <Button type="text" aria-label="AI assistant (coming soon)" icon={<Bot />} disabled />
            <AlertBell />
            <div className="user-menu-container" ref={userMenuRef}>
              <Button
                type="text"
                className="user-menu"
                aria-label="Mở menu tài khoản"
                aria-haspopup="menu"
                aria-expanded={userMenuOpen}
                onClick={() => setUserMenuOpen((open) => !open)}
              >
                <Avatar size="small">{role?.charAt(0)}</Avatar>
                {screens.sm && role ? <Tag bordered={false}>{roleLabels[role]}</Tag> : null}
                <ChevronDown size={14} />
              </Button>
              {userMenuOpen ? (
                <div className="user-dropdown" role="menu" aria-label="Menu tài khoản">
                  <Button
                    type="text"
                    danger
                    block
                    role="menuitem"
                    icon={<LogOut size={16} />}
                    onClick={() => void handleLogout()}
                  >
                    Đăng xuất
                  </Button>
                </div>
              ) : null}
            </div>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
          <footer className="app-footer">
            <Typography.Text type="secondary">Bzcom CRM · Customer request management</Typography.Text>
          </footer>
        </Content>
      </Layout>
    </Layout>
  );
}
