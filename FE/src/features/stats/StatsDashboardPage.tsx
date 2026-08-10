import { Button, Card, Col, Progress, Row, Space, Statistic, Tag, Typography } from 'antd';
import { Bug, CheckCircle2, ClipboardList, Gauge, UsersRound } from 'lucide-react';
import type { ReactNode } from 'react';
import { PageHeader } from '../../components/PageHeader';
import { EmptyState, ErrorAlert, PageLoading } from '../../components/PageStates';
import type { Category, StatsResponse } from '../../types/api';
import { categoryLabels } from '../../utils/format';
import { buildCategoryGradient, categoryColors, safePercentage } from './chart';
import { useRequestStats } from './queries';

const categoryOrder: Category[] = ['BUG', 'FEATURE', 'INQUIRY'];

export function StatsDashboardPage() {
  const stats = useRequestStats();

  if (stats.isLoading) return <PageLoading rows={8} />;

  return (
    <div className="page-stack stats-page">
      <PageHeader title="Operations dashboard" description="A live view of request volume, completion and developer workload." />
      {stats.error ? <ErrorAlert error={stats.error} action={<Button onClick={() => void stats.refetch()}>Retry</Button>} /> : null}
      {stats.data ? <DashboardContent stats={stats.data} /> : null}
    </div>
  );
}

function DashboardContent({ stats }: { stats: StatsResponse }) {
  const categories = categoryOrder.map((category) => ({ category, count: stats.byCategory[category] ?? 0 }));
  const maxAssigned = Math.max(0, ...stats.byDeveloper.map((developer) => developer.assignedCount));
  const completionPercent = Math.round(Math.min(1, Math.max(0, stats.completionRate)) * 100);

  return (
    <>
      <Row gutter={[16, 16]}>
        <Metric title="Total requests" value={stats.total} icon={<ClipboardList />} tone="blue" />
        <Metric title="Completed" value={stats.completed} icon={<CheckCircle2 />} tone="green" />
        <Metric title="Completion rate" value={completionPercent} suffix="%" icon={<Gauge />} tone="purple" />
        <Metric title="Active workload" value={Math.max(0, stats.total - stats.completed)} icon={<UsersRound />} tone="orange" />
      </Row>

      <Row gutter={[20, 20]}>
        <Col xs={24} xl={10}>
          <Card className="detail-card chart-card" title="Requests by category" extra={<Bug size={18} />}>
            {stats.total === 0 ? <EmptyState title="No request data" description="Category distribution will appear after requests are created." /> : (
              <div className="category-chart">
                <div
                  className="category-donut"
                  role="img"
                  aria-label={categories.map((item) => `${categoryLabels[item.category]} ${item.count}`).join(', ')}
                  style={{ background: buildCategoryGradient(categories) }}
                >
                  <div><strong>{stats.total}</strong><span>Total</span></div>
                </div>
                <div className="chart-legend">
                  {categories.map((item) => (
                    <div key={item.category}>
                      <span className="chart-legend__swatch" style={{ background: categoryColors[item.category] }} />
                      <Typography.Text>{categoryLabels[item.category]}</Typography.Text>
                      <Typography.Text strong>{item.count}</Typography.Text>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <Card className="detail-card chart-card" title="Developer workload" extra={<Tag>{stats.byDeveloper.length} developers</Tag>}>
            {!stats.byDeveloper.length ? <EmptyState title="No developer data" description="Assignments will appear here after requests are distributed." /> : (
              <div className="developer-chart" role="img" aria-label="Assigned and completed requests by developer">
                <div className="developer-chart__legend"><span>Assigned</span><span>Done</span></div>
                {stats.byDeveloper.map((developer) => (
                  <div className="developer-row" key={developer.developerId}>
                    <div className="developer-row__label">
                      <Typography.Text strong>{developer.developerName}</Typography.Text>
                      <Typography.Text type="secondary">ID {developer.developerId}</Typography.Text>
                    </div>
                    <div className="developer-row__bars">
                      <div className="metric-bar"><span style={{ width: `${safePercentage(developer.assignedCount, maxAssigned)}%` }} /><b>{developer.assignedCount}</b></div>
                      <div className="metric-bar metric-bar--done"><span style={{ width: `${safePercentage(developer.doneCount, maxAssigned)}%` }} /><b>{developer.doneCount}</b></div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Card className="detail-card completion-card">
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          <Space style={{ justifyContent: 'space-between', width: '100%' }}>
            <Typography.Text strong>Overall completion</Typography.Text>
            <Typography.Text>{stats.completed} of {stats.total}</Typography.Text>
          </Space>
          <Progress percent={completionPercent} strokeColor="#1f8f63" trailColor="#e8ebf0" />
        </Space>
      </Card>
    </>
  );
}

function Metric({ title, value, suffix, icon, tone }: { title: string; value: number; suffix?: string; icon: ReactNode; tone: string }) {
  return (
    <Col xs={24} sm={12} xl={6}>
      <Card className="metric-card">
        <div className={`metric-card__icon metric-card__icon--${tone}`}>{icon}</div>
        <Statistic title={title} value={value} suffix={suffix} />
      </Card>
    </Col>
  );
}
