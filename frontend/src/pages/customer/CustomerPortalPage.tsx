import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Plus, Ticket, CheckCircle2, Clock, AlertTriangle,
  LogOut, ChevronRight, MessageSquare, Building2, RefreshCw
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';

interface TicketSummary {
  id: string;
  ticketNumber: number;
  subject: string;
  status: string;
  priority: string;
  createdAt: string;
  updatedAt: string;
}

const STATUS_CONFIG: Record<string, { label: string; color: string; bg: string; icon: React.ReactNode }> = {
  OPEN:               { label: 'Open',           color: '#60a5fa', bg: 'rgba(96,165,250,0.12)',   icon: <Clock size={12} /> },
  IN_PROGRESS:        { label: 'In Progress',    color: '#a78bfa', bg: 'rgba(167,139,250,0.12)',  icon: <RefreshCw size={12} /> },
  NEEDS_AGENT_REVIEW: { label: 'Agent Review',   color: '#fbbf24', bg: 'rgba(251,191,36,0.12)',   icon: <AlertTriangle size={12} /> },
  WAITING_ON_CUSTOMER:{ label: 'Awaiting You',   color: '#fb923c', bg: 'rgba(251,146,60,0.12)',   icon: <MessageSquare size={12} /> },
  RESOLVED:           { label: 'Resolved',       color: '#4ade80', bg: 'rgba(74,222,128,0.12)',   icon: <CheckCircle2 size={12} /> },
  CLOSED:             { label: 'Closed',         color: '#64748b', bg: 'rgba(100,116,139,0.12)',  icon: <CheckCircle2 size={12} /> },
};

function StatusBadge({ status }: { status: string }) {
  const cfg = STATUS_CONFIG[status] ?? { label: status, color: '#94a3b8', bg: 'rgba(148,163,184,0.1)', icon: null };
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '4px',
      padding: '3px 10px', borderRadius: '999px', fontSize: '11px', fontWeight: 600,
      color: cfg.color, background: cfg.bg,
    }}>
      {cfg.icon} {cfg.label}
    </span>
  );
}

function formatRelative(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function CustomerPortalPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore(s => ({ user: s.user, logout: s.logout }));

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['my-tickets'],
    queryFn: async () => {
      const res = await api.get('/tickets/my?size=50');
      return res.data.data.content as TicketSummary[];
    },
    refetchInterval: 30000,
  });

  const handleLogout = () => { logout(); navigate('/customer/login'); };

  const openCount   = data?.filter(t => t.status === 'OPEN').length ?? 0;
  const activeCount = data?.filter(t => ['IN_PROGRESS', 'WAITING_ON_CUSTOMER', 'NEEDS_AGENT_REVIEW'].includes(t.status)).length ?? 0;
  const closedCount = data?.filter(t => ['RESOLVED', 'CLOSED'].includes(t.status)).length ?? 0;

  return (
    <div style={s.page}>
      {/* Top nav */}
      <header style={s.nav}>
        <div style={s.navLeft}>
          <div style={s.navIcon}>
            <Building2 size={16} color="#fff" />
          </div>
          <div>
            <div style={s.orgName}>{user?.organizationName ?? 'Customer Portal'}</div>
            <div style={s.userEmail}>{user?.email}</div>
          </div>
        </div>
        <div style={s.navRight}>
          <button style={s.newBtn} onClick={() => navigate('/customer/portal/new')}>
            <Plus size={14} /> New Ticket
          </button>
          <button style={s.logoutBtn} onClick={handleLogout}>
            <LogOut size={14} />
          </button>
        </div>
      </header>

      <main style={s.main}>
        {/* Welcome */}
        <div style={s.welcomeBar}>
          <div>
            <h1 style={s.welcomeTitle}>Hi, {user?.firstName}! 👋</h1>
            <p style={s.welcomeSub}>Manage your support requests below</p>
          </div>
          <button style={s.refreshBtn} onClick={() => refetch()}>
            <RefreshCw size={14} />
          </button>
        </div>

        {/* Stats */}
        <div style={s.statsGrid}>
          {[
            { label: 'Open', count: openCount, color: '#60a5fa' },
            { label: 'Active', count: activeCount, color: '#a78bfa' },
            { label: 'Resolved', count: closedCount, color: '#4ade80' },
          ].map(stat => (
            <div key={stat.label} style={s.statCard}>
              <div style={{ ...s.statNum, color: stat.color }}>{stat.count}</div>
              <div style={s.statLabel}>{stat.label}</div>
            </div>
          ))}
        </div>

        {/* Ticket list */}
        <div style={s.section}>
          <div style={s.sectionHeader}>
            <h2 style={s.sectionTitle}>Your Tickets</h2>
            <span style={s.sectionCount}>{data?.length ?? 0} total</span>
          </div>

          {isLoading && (
            <div style={s.emptyState}>Loading your tickets…</div>
          )}

          {!isLoading && (!data || data.length === 0) && (
            <div style={s.emptyState}>
              <Ticket size={40} style={{ opacity: 0.3, marginBottom: 12 }} />
              <p>No tickets yet. Create one and our AI will respond instantly.</p>
              <button style={s.newBtn} onClick={() => navigate('/customer/portal/new')}>
                <Plus size={14} /> Submit First Ticket
              </button>
            </div>
          )}

          <div style={s.ticketList}>
            {data?.map(ticket => (
              <div
                key={ticket.id}
                style={s.ticketCard}
                onClick={() => navigate(`/customer/portal/tickets/${ticket.id}`)}
              >
                <div style={s.ticketLeft}>
                  <div style={s.ticketNumRow}>
                    <span style={s.ticketNum}>#{ticket.ticketNumber}</span>
                    <StatusBadge status={ticket.status} />
                    {ticket.status === 'NEEDS_AGENT_REVIEW' && (
                      <span style={s.agentBadge}>🔔 Agent notified</span>
                    )}
                  </div>
                  <div style={s.ticketSubject}>{ticket.subject}</div>
                  <div style={s.ticketMeta}>Updated {formatRelative(ticket.updatedAt)}</div>
                </div>
                <ChevronRight size={16} style={{ color: '#475569', flexShrink: 0 }} />
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}

const s: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
    fontFamily: "'Inter', system-ui, sans-serif",
    color: '#f1f5f9',
  },
  nav: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '16px 24px',
    background: 'rgba(255,255,255,0.04)',
    borderBottom: '1px solid rgba(255,255,255,0.07)',
    backdropFilter: 'blur(12px)',
    position: 'sticky', top: 0, zIndex: 50,
  },
  navLeft: { display: 'flex', alignItems: 'center', gap: '12px' },
  navIcon: {
    width: '36px', height: '36px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    borderRadius: '10px', display: 'flex',
    alignItems: 'center', justifyContent: 'center',
  },
  orgName: { fontSize: '14px', fontWeight: 600, color: '#f1f5f9' },
  userEmail: { fontSize: '12px', color: '#64748b' },
  navRight: { display: 'flex', alignItems: 'center', gap: '10px' },
  newBtn: {
    display: 'flex', alignItems: 'center', gap: '6px',
    padding: '8px 16px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    color: '#fff', border: 'none', borderRadius: '8px',
    fontSize: '13px', fontWeight: 600, cursor: 'pointer',
  },
  logoutBtn: {
    padding: '8px', background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '8px', color: '#94a3b8', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },
  main: { maxWidth: '800px', margin: '0 auto', padding: '32px 24px' },
  welcomeBar: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' },
  welcomeTitle: { fontSize: '24px', fontWeight: 700, margin: '0 0 4px' },
  welcomeSub: { fontSize: '14px', color: '#64748b', margin: 0 },
  refreshBtn: {
    padding: '8px', background: 'rgba(255,255,255,0.05)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '8px', color: '#64748b', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },
  statsGrid: { display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', marginBottom: '28px' },
  statCard: {
    background: 'rgba(255,255,255,0.04)',
    border: '1px solid rgba(255,255,255,0.07)',
    borderRadius: '12px', padding: '16px 20px',
    textAlign: 'center',
  },
  statNum: { fontSize: '28px', fontWeight: 700, lineHeight: 1 },
  statLabel: { fontSize: '12px', color: '#64748b', marginTop: '4px' },
  section: {},
  sectionHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' },
  sectionTitle: { fontSize: '16px', fontWeight: 600, margin: 0 },
  sectionCount: { fontSize: '12px', color: '#64748b' },
  emptyState: {
    textAlign: 'center', padding: '48px 24px',
    color: '#64748b', display: 'flex', flexDirection: 'column',
    alignItems: 'center', gap: '12px',
    background: 'rgba(255,255,255,0.02)',
    borderRadius: '12px', border: '1px dashed rgba(255,255,255,0.08)',
  },
  ticketList: { display: 'flex', flexDirection: 'column', gap: '8px' },
  ticketCard: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '16px 18px',
    background: 'rgba(255,255,255,0.04)',
    border: '1px solid rgba(255,255,255,0.07)',
    borderRadius: '12px', cursor: 'pointer',
    transition: 'background 0.15s, border-color 0.15s',
  },
  ticketLeft: { flex: 1, minWidth: 0 },
  ticketNumRow: { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' },
  ticketNum: { fontSize: '12px', color: '#64748b', fontFamily: 'monospace' },
  ticketSubject: { fontSize: '14px', fontWeight: 500, color: '#e2e8f0', marginBottom: '4px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' },
  ticketMeta: { fontSize: '12px', color: '#475569' },
  agentBadge: { fontSize: '11px', color: '#fbbf24', background: 'rgba(251,191,36,0.08)', padding: '2px 8px', borderRadius: '999px' },
};
