import { Avatar, Input, Select, Space, Table, Typography } from 'antd';
import { Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ErrorAlert, PageLoading } from '../../components/PageStates';
import { PageHeader } from '../../components/PageHeader';
import { RoleTag } from '../../components/ResourceTags';
import type { MemberResponse, Role } from '../../types/api';
import { formatDateTime } from '../../utils/format';
import { useMembers } from './queries';

export function MemberListPage() {
  const members = useMembers();
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState<Role | undefined>();

  const data = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return (members.data ?? []).filter(
      (member) =>
        (!role || member.role === role) &&
        (!normalized || member.name.toLowerCase().includes(normalized) || member.email.toLowerCase().includes(normalized)),
    );
  }, [keyword, members.data, role]);

  if (members.isLoading) return <PageLoading />;

  return (
    <div className="page-stack">
      <PageHeader title="Members" description="Review the people and roles that can access Bzcom CRM." />
      {members.error ? <ErrorAlert error={members.error} /> : null}
      <section className="surface-card">
        <div className="toolbar">
          <Input
            allowClear
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            prefix={<Search aria-hidden size={16} />}
            placeholder="Search name or email"
            aria-label="Search members by name or email"
            className="toolbar__search"
          />
          <Select<Role>
            allowClear
            value={role}
            onChange={setRole}
            placeholder="All roles"
            aria-label="Filter members by role"
            className="toolbar__select"
            options={[
              { value: 'ADMIN', label: 'Administrator' },
              { value: 'DEVELOPER', label: 'Developer' },
              { value: 'CLIENT', label: 'Client' },
            ]}
          />
        </div>
        <Table<MemberResponse>
          rowKey="id"
          dataSource={data}
          pagination={{ pageSize: 10, showSizeChanger: false, hideOnSinglePage: true }}
          locale={{ emptyText: 'No members match the current filters.' }}
          scroll={{ x: 760 }}
          columns={[
            {
              title: 'Member',
              dataIndex: 'name',
              key: 'name',
              render: (_, member) => (
                <Space>
                  <Avatar>{member.name.charAt(0).toUpperCase()}</Avatar>
                  <div className="table-identity">
                    <Link to={`/members/${member.id}`}>{member.name}</Link>
                    <Typography.Text type="secondary">ID {member.id}</Typography.Text>
                  </div>
                </Space>
              ),
            },
            { title: 'Email', dataIndex: 'email', key: 'email' },
            { title: 'Role', dataIndex: 'role', key: 'role', render: (value: Role) => <RoleTag role={value} /> },
            {
              title: 'Created',
              dataIndex: 'createdAt',
              key: 'createdAt',
              render: (value: string) => formatDateTime(value),
            },
          ]}
        />
      </section>
    </div>
  );
}
