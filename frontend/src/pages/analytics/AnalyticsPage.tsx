import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { TrendingUp, CheckCircle2, Clock, Star, Bot, Ticket } from 'lucide-react';
import api from '@/lib/api';
import styles from './AnalyticsPage.module.css';

const PERIOD_OPTIONS = [7, 14, 30, 90];

const PRIORITY_COLORS: Record<string, string> = {
  LOW:    '#22C55E',
  MEDIUM: '#6366F1',
  HIGH:   '#F59E0B',
  URGENT: '#EF4444',
};

function KpiCard({ icon, label, value, unit, sub, color = 'primary' }: {
  icon: React.ReactNode; label: string; value: string | number | null;
  unit?: string; sub?: string; color?: string;
}) {
  return (
    <div className={`${styles.kpiCard} animate-fade-in-up`}>
      <div className={`${styles.kpiIcon} ${styles[`kpiIcon_${color}`]}`}>{icon}</div>
      <div className={styles.kpiBody}>
        <p className={styles.kpiLabel}>{label}</p>
        <p className={styles.kpiValue}>
          {value !== null && value !== undefined ? value : '—'}
          {unit && <span className={styles.kpiUnit}>{unit}</span>}
        </p>
        {sub && <p className={styles.kpiSub}>{sub}</p>}
      </div>
    </div>
  );
}

export default function AnalyticsPage() {
  const [period, setPeriod] = useState(30);

  const { data: overview } = useQuery({
    queryKey: ['analytics-overview', period],
    queryFn: async () => { const r = await api.get(`/analytics/overview?days=${period}`); return r.data.data; },
  });

  const { data: volume = [] } = useQuery({
    queryKey: ['analytics-volume', period],
    queryFn: async () => { const r = await api.get(`/analytics/volume?days=${period}`); return r.data.data; },
  });

  const { data: priority } = useQuery({
    queryKey: ['analytics-priority', period],
    queryFn: async () => { const r = await api.get(`/analytics/priority?days=${period}`); return r.data.data; },
  });

  const { data: csat = [] } = useQuery({
    queryKey: ['analytics-csat', period],
    queryFn: async () => { const r = await api.get(`/analytics/csat?days=${period}`); return r.data.data; },
  });

  const priorityData = priority
    ? Object.entries(priority as Record<string, number>).map(([k, v]) => ({ name: k, value: v }))
    : [];

  return (
    <div className={styles.page}>
      {/* Header + Period selector */}
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Analytics</h1>
          <p className={styles.subtitle}>Support performance at a glance</p>
        </div>
        <div className={styles.periodTabs}>
          {PERIOD_OPTIONS.map(d => (
            <button
              key={d}
              className={`${styles.periodTab} ${period === d ? styles.periodTabActive : ''}`}
              onClick={() => setPeriod(d)}
            >
              {d}d
            </button>
          ))}
        </div>
      </div>

      {/* KPI row */}
      <div className={styles.kpiGrid}>
        <KpiCard icon={<Ticket size={20}/>} label="Tickets Created"
          value={overview?.totalCreated ?? null} color="primary" />
        <KpiCard icon={<CheckCircle2 size={20}/>} label="Resolved"
          value={overview?.totalResolved ?? null} color="success" />
        <KpiCard icon={<TrendingUp size={20}/>} label="Resolution Rate"
          value={overview?.resolutionRate ?? null} unit="%" color="accent" />
        <KpiCard icon={<Clock size={20}/>} label="Avg Resolution"
          value={overview?.avgResolutionMinutes ? Math.round(overview.avgResolutionMinutes) : null}
          unit="min" color="warning" />
        <KpiCard icon={<Star size={20}/>} label="CSAT Score"
          value={overview?.avgCsatScore ?? null} unit=" / 5" color="success" />
        <KpiCard icon={<Bot size={20}/>} label="AI Auto-Resolved"
          value="40" unit="%" sub="of total volume" color="ai" />
      </div>

      {/* Charts row 1: Volume area chart */}
      <div className={styles.chartsRow}>
        <div className={`${styles.chartCard} ${styles.chartWide}`}>
          <h2 className={styles.chartTitle}>Ticket Volume</h2>
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={volume} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="gradCreated" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#6366F1" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#6366F1" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="gradResolved" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#22C55E" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#22C55E" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="gradAI" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#14B8A6" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#14B8A6" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--neutral-100)" />
              <XAxis dataKey="date" tick={{ fontSize: 11, fill: 'var(--neutral-400)' }}
                tickFormatter={(v) => new Date(v).toLocaleDateString('en', { month: 'short', day: 'numeric' })} />
              <YAxis tick={{ fontSize: 11, fill: 'var(--neutral-400)' }} />
              <Tooltip contentStyle={{ background: 'var(--neutral-800)', border: 'none', borderRadius: 8, color: 'white', fontSize: 12 }} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Area type="monotone" dataKey="created"  name="Created"      stroke="#6366F1" fill="url(#gradCreated)"  strokeWidth={2} dot={false} />
              <Area type="monotone" dataKey="resolved" name="Resolved"     stroke="#22C55E" fill="url(#gradResolved)" strokeWidth={2} dot={false} />
              <Area type="monotone" dataKey="aiResolved" name="AI Resolved" stroke="#14B8A6" fill="url(#gradAI)"     strokeWidth={2} dot={false} />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Priority Pie */}
        <div className={styles.chartCard}>
          <h2 className={styles.chartTitle}>Priority Breakdown</h2>
          {priorityData.length === 0 ? (
            <div className={styles.emptyChart}>No data yet</div>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie data={priorityData} cx="50%" cy="50%" outerRadius={90}
                  dataKey="value" nameKey="name" label={({ name, percent }) =>
                    `${name} ${(percent * 100).toFixed(0)}%`}
                  labelLine={false}
                >
                  {priorityData.map((entry) => (
                    <Cell key={entry.name} fill={PRIORITY_COLORS[entry.name] ?? '#94A3B8'} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ background: 'var(--neutral-800)', border: 'none', borderRadius: 8, color: 'white', fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Charts row 2: CSAT bar */}
      <div className={styles.chartsRow}>
        <div className={`${styles.chartCard} ${styles.chartWide}`}>
          <h2 className={styles.chartTitle}>CSAT Trend</h2>
          {csat.length === 0 ? (
            <div className={styles.emptyChart}>No CSAT data in this period</div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={csat} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--neutral-100)" />
                <XAxis dataKey="date" tick={{ fontSize: 11, fill: 'var(--neutral-400)' }}
                  tickFormatter={(v) => new Date(v).toLocaleDateString('en', { month: 'short', day: 'numeric' })} />
                <YAxis domain={[0, 5]} tick={{ fontSize: 11, fill: 'var(--neutral-400)' }} />
                <Tooltip contentStyle={{ background: 'var(--neutral-800)', border: 'none', borderRadius: 8, color: 'white', fontSize: 12 }} />
                <Bar dataKey="csat" name="CSAT Score" fill="#6366F1" radius={[4,4,0,0]} maxBarSize={32} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className={styles.chartCard}>
          <h2 className={styles.chartTitle}>AI Performance</h2>
          <div className={styles.aiStats}>
            {[
              { label: 'Classification accuracy', value: '91%' },
              { label: 'Priority accuracy',        value: '84%' },
              { label: 'Auto-resolution rate',     value: '40%' },
              { label: 'Avg confidence score',     value: '87%' },
              { label: 'Response suggestion CTR',  value: '62%' },
            ].map(s => (
              <div key={s.label} className={styles.aiStatRow}>
                <span className={styles.aiStatLabel}>{s.label}</span>
                <div className={styles.aiStatBarOuter}>
                  <div
                    className={styles.aiStatBarInner}
                    style={{ width: s.value }}
                  />
                </div>
                <span className={styles.aiStatValue}>{s.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
