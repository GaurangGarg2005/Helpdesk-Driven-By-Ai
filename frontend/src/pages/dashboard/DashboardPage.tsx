import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Ticket, CheckCircle2, Clock, AlertTriangle,
  TrendingUp, Bot, Users, ArrowUpRight
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import api, { SilentConfig } from '@/lib/api';
import styles from './DashboardPage.module.css';

// ── Stat card ──────────────────────────────────────────────
function StatCard({
  icon, label, value, delta, color = 'primary'
}: {
  icon: React.ReactNode; label: string; value: string | number;
  delta?: string; color?: string;
}) {
  return (
    <div className={`${styles.statCard} animate-fade-in-up`}>
      <div className={`${styles.statIcon} ${styles[`icon_${color}`]}`}>{icon}</div>
      <div className={styles.statBody}>
        <p className={styles.statLabel}>{label}</p>
        <p className={styles.statValue}>{value}</p>
        {delta && (
          <p className={styles.statDelta}>
            <TrendingUp size={12} /> {delta}
          </p>
        )}
      </div>
    </div>
  );
}

// ── Activity item ──────────────────────────────────────────
function ActivityItem({ ticket }: { ticket: any }) {
  const statusColor: Record<string, string> = {
    OPEN: 'var(--danger-500)',
    IN_PROGRESS: 'var(--primary-500)',
    RESOLVED: 'var(--success-500)',
    CLOSED: 'var(--neutral-400)',
  };
  return (
    <div className={styles.actItem}>
      <div
        className={styles.actDot}
        style={{ background: statusColor[ticket.status] ?? 'var(--neutral-400)' }}
      />
      <div className={styles.actContent}>
        <p className={styles.actTitle}>#{ticket.ticketNumber} — {ticket.subject}</p>
        <p className={styles.actMeta}>{ticket.status} · {ticket.priority} priority</p>
      </div>
      <ArrowUpRight size={14} className={styles.actArrow} />
    </div>
  );
}

export default function DashboardPage() {
  const user = useAuthStore(s => s.user);

  const { data: stats } = useQuery({
    queryKey: ['ticket-stats'],
    queryFn: async () => {
      try {
        const res = await api.get('/tickets/stats', { silent: true } as SilentConfig);
        return res.data.data;
      } catch { return null; }
    },
    retry: false,
  });

  const { data: recentTickets } = useQuery({
    queryKey: ['recent-tickets'],
    queryFn: async () => {
      try {
        const res = await api.get('/tickets?size=5', { silent: true } as SilentConfig);
        return res.data.data?.content ?? [];
      } catch { return []; }
    },
    retry: false,
  });

  const total = stats
    ? Object.values(stats as Record<string, number>).reduce((a, b) => a + b, 0)
    : 0;

  return (
    <div className={styles.page}>
      {/* ── Welcome header ── */}
      <div className={styles.welcomeHeader}>
        <div>
          <h1 className={styles.welcome}>
            Good {getGreeting()}, {user?.firstName} 👋
          </h1>
          <p className={styles.welcomeSub}>
            Here's what's happening with your support queue today.
          </p>
        </div>
        <div className={styles.aiBanner}>
          <Bot size={18} />
          <span>AI has auto-resolved <strong>12 tickets</strong> today</span>
        </div>
      </div>

      {/* ── Stats grid ── */}
      <div className={styles.statsGrid}>
        <StatCard
          icon={<Ticket size={20} />}
          label="Open Tickets"
          value={stats?.open ?? '—'}
          delta="+3 from yesterday"
          color="danger"
        />
        <StatCard
          icon={<Clock size={20} />}
          label="In Progress"
          value={stats?.inProgress ?? '—'}
          color="primary"
        />
        <StatCard
          icon={<AlertTriangle size={20} />}
          label="Waiting on Customer"
          value={stats?.waitingOnCustomer ?? '—'}
          color="warning"
        />
        <StatCard
          icon={<CheckCircle2 size={20} />}
          label="Resolved Today"
          value={stats?.resolved ?? '—'}
          delta="+8% vs last week"
          color="success"
        />
        <StatCard
          icon={<Users size={20} />}
          label="Total Tickets"
          value={total}
          color="neutral"
        />
        <StatCard
          icon={<Bot size={20} />}
          label="AI Auto-Resolved"
          value="12"
          delta="40% of today's volume"
          color="accent"
        />
      </div>

      {/* ── Bottom panels ── */}
      <div className={styles.panels}>
        {/* Recent tickets */}
        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>Recent Tickets</h2>
            <a href="/tickets" className={styles.panelLink}>View all →</a>
          </div>
          <div className={styles.actList}>
            {(recentTickets ?? []).length === 0 && (
              <p className={styles.emptyMsg}>No tickets yet. <a href="/tickets/new">Create one →</a></p>
            )}
            {(recentTickets ?? []).map((t: any) => (
              <ActivityItem key={t.id} ticket={t} />
            ))}
          </div>
        </div>

        {/* AI insights */}
        <div className={`${styles.panel} ${styles.aiPanel}`}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>
              <Bot size={16} style={{ color: 'var(--accent-500)' }} />
              AI Insights
            </h2>
          </div>
          <div className={styles.insights}>
            {[
              { label: 'Avg. AI confidence',    value: '87%',     trend: '↑ 2%' },
              { label: 'Category accuracy',      value: '91%',     trend: '↑ 5%' },
              { label: 'Priority accuracy',      value: '84%',     trend: '↑ 1%' },
              { label: 'Avg. first response',    value: '4m 12s',  trend: '↓ 18s' },
              { label: 'CSAT score',             value: '4.7 / 5', trend: '↑ 0.2' },
            ].map(item => (
              <div key={item.label} className={styles.insightRow}>
                <span className={styles.insightLabel}>{item.label}</span>
                <span className={styles.insightValue}>{item.value}</span>
                <span className={styles.insightTrend}>{item.trend}</span>
              </div>
            ))}
          </div>

          <div className={styles.aiSuggestion}>
            <p className={styles.aiSuggestionTitle}>💡 Suggestion</p>
            <p className={styles.aiSuggestionText}>
              6 tickets tagged <em>billing</em> are unassigned. Consider routing them to the Finance team.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return 'morning';
  if (h < 18) return 'afternoon';
  return 'evening';
}
