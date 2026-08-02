import { Avatar, Button, Descriptions, Space, Typography } from 'antd';
import { ArrowLeft, Mail, ShieldCheck, UserRound } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '../../components/PageHeader';
import { PageLoading, ResourceError } from '../../components/PageStates';
import { RoleTag } from '../../components/ResourceTags';
import { formatDateTime } from '../../utils/format';
import { useMember } from './queries';

export function MemberDetailPage() {
  const { id: rawId } = useParams();
  const navigate = useNavigate();
  const id = Number(rawId);
  const member = useMember(Number.isInteger(id) && id > 0 ? id : 0);

  if (!Number.isInteger(id) || id <= 0) return <ResourceError error={new Error('Invalid member ID.')} resource="member" />;
  if (member.isLoading) return <PageLoading />;
  if (member.error || !member.data) return <ResourceError error={member.error} resource="member" />;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow={`Member #${member.data.id}`}
        title={member.data.name}
        description="Account identity and authorization role."
        actions={<Button icon={<ArrowLeft size={16} />} onClick={() => void navigate('/members')}>Back to members</Button>}
      />
      <section className="surface-card member-profile">
        <Space align="start" size="large" className="member-profile__hero">
          <Avatar size={72}>{member.data.name.charAt(0).toUpperCase()}</Avatar>
          <div>
            <Typography.Title level={3}>{member.data.name}</Typography.Title>
            <RoleTag role={member.data.role} />
          </div>
        </Space>
        <Descriptions column={{ xs: 1, sm: 2 }} bordered>
          <Descriptions.Item label={<Space><UserRound size={15} />Member ID</Space>}>{member.data.id}</Descriptions.Item>
          <Descriptions.Item label={<Space><Mail size={15} />Email</Space>}>{member.data.email}</Descriptions.Item>
          <Descriptions.Item label={<Space><ShieldCheck size={15} />Role</Space>}><RoleTag role={member.data.role} /></Descriptions.Item>
          <Descriptions.Item label="Created at">{formatDateTime(member.data.createdAt)}</Descriptions.Item>
        </Descriptions>
      </section>
    </div>
  );
}
