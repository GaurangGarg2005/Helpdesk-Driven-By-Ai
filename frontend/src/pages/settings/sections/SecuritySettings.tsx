import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Shield, User, Clock, Trash2 } from 'lucide-react';
import api from '@/lib/api';
import styles from '../SettingsPage.module.css';
import { formatDistanceToNow } from 'date-fns';

interface AuditLogEntry {
  id: string;
  userId: string;
  action: string;
  entityType?: string;
  entityId?: string;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

const ACTION_COLOR: Record<string, string> = {
  CREATE: 'var(--success-500)',
  UPDATE: 'var(--primary-500)',
  DELETE: 'var(--danger-500)',
  LOGIN:  'var(--accent-500)',
  LOGOUT: 'var(--neutral-400)',
};

export default function SecuritySettings() {
  const { data: logs = [], isLoading } = useQuery<AuditLogEntry[]>({
    queryKey: ['audit-logs'],
    queryFn: async () => {
      const r = await api.get('/org/audit-logs?page=0&size=30');
      return r.data.data?.content ?? [];
    },
  });

  return (
    <div>
      <h2 className={styles.sectionTitle}>Security &amp; Audit Log</h2>
      <p className={styles.sectionSubtitle}>Review all activity across your organization in real-time.</p>
      <hr className={styles.divider} />

      {/* Session card */}
      <div style={{ background: 'var(--neutral-50)', border: '1px solid var(--neutral-100)', borderRadius: 'var(--radius-lg)', padding: 'var(--space-4)', marginBottom: 'var(--space-5)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <p style={{ fontWeight: 700, fontSize: 'var(--text-body-sm)', color: 'var(--neutral-700)' }}>Current Session</p>
            <p style={{ fontSize: 'var(--text-caption)', color: 'var(--neutral-400)', marginTop: 2 }}>Secured via JWT · Auto-expires in 15 minutes (refresh token valid 7 days)</p>
          </div>
          <div style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--success-500)', boxShadow: '0 0 6px var(--success-400)' }} />
        </div>
      </div>

      {/* MFA placeholder */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: 'var(--space-4)', border: '1px solid var(--neutral-100)', borderRadius: 'var(--radius-lg)', marginBottom: 'var(--space-5)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <div style={{ width: 36, height: 36, background: 'var(--warning-50)', borderRadius: 'var(--radius-md)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--warning-500)' }}>
            <Shield size={16} />
          </div>
          <div>
            <p style={{ fontWeight: 700, fontSize: 'var(--text-body-sm)', color: 'var(--neutral-700)' }}>Two-Factor Authentication</p>
            <p style={{ fontSize: 'var(--text-caption)', color: 'var(--neutral-400)', marginTop: 2 }}>Coming soon — TOTP-based 2FA for all org members</p>
          </div>
        </div>
        <span style={{ fontSize: 'var(--text-caption)', background: 'var(--neutral-100)', padding: '4px 10px', borderRadius: 'var(--radius-full)', color: 'var(--neutral-500)', fontWeight: 600 }}>Soon</span>
      </div>

      <h3 style={{ fontWeight: 700, fontSize: 'var(--text-body-sm)', color: 'var(--neutral-700)', marginBottom: 'var(--space-3)' }}>
        Recent Activity
      </h3>

      {isLoading ? (
        <p style={{ color: 'var(--neutral-400)', fontSize: 'var(--text-body-sm)' }}>Loading audit log…</p>
      ) : logs.length === 0 ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}><Shield size={20} /></div>
          <p>No activity recorded yet.</p>
        </div>
      ) : (
        <div>
          {logs.map(log => (
            <div key={log.id} className={styles.auditRow}>
              <div className={styles.auditDot} style={{ background: ACTION_COLOR[log.action.split('.')[0]] ?? 'var(--neutral-300)' }} />
              <div>
                <p className={styles.auditAction}>
                  <span style={{ color: ACTION_COLOR[log.action.split('.')[0]] ?? 'var(--neutral-700)' }}>{log.action}</span>
                  {log.entityType && <span style={{ color: 'var(--neutral-500)', fontWeight: 400 }}> on {log.entityType}</span>}
                  {log.details && <span style={{ color: 'var(--neutral-500)', fontWeight: 400 }}> — {log.details}</span>}
                </p>
                <p className={styles.auditMeta}>
                  <User size={10} style={{ display: 'inline', marginRight: 3 }} />
                  {log.userId ?? 'System'}
                  <Clock size={10} style={{ display: 'inline', margin: '0 3px 0 8px' }} />
                  {formatDistanceToNow(new Date(log.createdAt), { addSuffix: true })}
                  {log.ipAddress && <span style={{ marginLeft: 8 }}>from {log.ipAddress}</span>}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
