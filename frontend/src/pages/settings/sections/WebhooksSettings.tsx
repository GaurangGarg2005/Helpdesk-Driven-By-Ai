import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Plus, Trash2, Globe, CheckCircle2, XCircle } from 'lucide-react';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from '../SettingsPage.module.css';
import { format } from 'date-fns';

const EVENT_TYPES = [
  'ticket.created', 'ticket.updated', 'ticket.assigned',
  'ticket.resolved', 'ticket.sla_breach', 'chat.started', 'chat.ended',
];

interface Webhook {
  id: string;
  url: string;
  events: string[];
  isActive: boolean;
  description?: string;
  createdAt: string;
}

export default function WebhooksSettings() {
  const queryClient = useQueryClient();
  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState({ url: '', description: '', events: [] as string[] });

  const { data: webhooks = [], isLoading } = useQuery<Webhook[]>({
    queryKey: ['webhooks'],
    queryFn: async () => {
      const r = await api.get('/org/webhooks');
      return r.data.data ?? [];
    },
  });

  const createMutation = useMutation({
    mutationFn: () => api.post('/org/webhooks', form),
    onSuccess: () => {
      setForm({ url: '', description: '', events: [] });
      setShowNew(false);
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
      toast.success('Webhook created!');
    },
    onError: () => toast.error('Failed to create webhook'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/org/webhooks/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] });
      toast.success('Webhook deleted');
    },
  });

  const toggleEvent = (event: string) => {
    setForm(prev => ({
      ...prev,
      events: prev.events.includes(event) ? prev.events.filter(e => e !== event) : [...prev.events, event],
    }));
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 'var(--space-1)' }}>
        <h2 className={styles.sectionTitle}>Webhooks</h2>
        <Button size="sm" icon={<Plus size={14} />} onClick={() => setShowNew(!showNew)}>
          {showNew ? 'Cancel' : 'Add Webhook'}
        </Button>
      </div>
      <p className={styles.sectionSubtitle}>Receive real-time HTTP callbacks when events occur.</p>
      <hr className={styles.divider} />

      {/* New webhook form */}
      {showNew && (
        <div style={{ background: 'var(--neutral-50)', border: '1px solid var(--neutral-200)', borderRadius: 'var(--radius-lg)', padding: 'var(--space-4)', marginBottom: 'var(--space-4)' }}>
          <div className={styles.formRow}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Endpoint URL</label>
              <input className={styles.formInput} placeholder="https://acme.com/webhooks/helpdesk"
                value={form.url} onChange={e => setForm(p => ({ ...p, url: e.target.value }))} />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.formLabel}>Description (optional)</label>
              <input className={styles.formInput} placeholder="Production webhook"
                value={form.description} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} />
            </div>
          </div>
          <div className={styles.formGroup} style={{ marginBottom: 'var(--space-4)' }}>
            <label className={styles.formLabel}>Events to subscribe</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-2)', marginTop: 'var(--space-1)' }}>
              {EVENT_TYPES.map(event => (
                <button key={event}
                  style={{
                    padding: '4px 12px', borderRadius: 'var(--radius-full)', fontSize: 'var(--text-caption)',
                    fontWeight: 600, border: '1px solid',
                    borderColor: form.events.includes(event) ? 'var(--primary-500)' : 'var(--neutral-200)',
                    background: form.events.includes(event) ? 'var(--primary-50)' : 'white',
                    color: form.events.includes(event) ? 'var(--primary-700)' : 'var(--neutral-500)',
                  }}
                  onClick={() => toggleEvent(event)}>{event}</button>
              ))}
            </div>
          </div>
          <Button size="sm" icon={<Globe size={14} />} loading={createMutation.isPending}
            onClick={() => createMutation.mutate()} disabled={!form.url || form.events.length === 0}>
            Create Webhook
          </Button>
        </div>
      )}

      {/* Webhooks table */}
      {isLoading ? (
        <p style={{ color: 'var(--neutral-400)', fontSize: 'var(--text-body-sm)' }}>Loading…</p>
      ) : webhooks.length === 0 ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}><Globe size={20} /></div>
          <p>No webhooks configured. Add one to receive event callbacks.</p>
        </div>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Endpoint</th>
                <th>Events</th>
                <th>Status</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {webhooks.map(w => (
                <tr key={w.id}>
                  <td>
                    <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--neutral-700)' }}>{w.url}</div>
                    {w.description && <div style={{ fontSize: 11, color: 'var(--neutral-400)' }}>{w.description}</div>}
                  </td>
                  <td style={{ fontSize: 11 }}>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                      {(Array.isArray(w.events) ? w.events : [w.events]).map(ev => (
                        <span key={ev} className={styles.mono}>{ev}</span>
                      ))}
                    </div>
                  </td>
                  <td>
                    {w.isActive
                      ? <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--success-600)', fontWeight: 600, fontSize: 12 }}><CheckCircle2 size={12} /> Active</span>
                      : <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--neutral-400)', fontSize: 12 }}><XCircle size={12} /> Disabled</span>}
                  </td>
                  <td style={{ color: 'var(--neutral-400)' }}>{format(new Date(w.createdAt), 'MMM d, yyyy')}</td>
                  <td>
                    <button className={styles.deleteBtn} onClick={() => deleteMutation.mutate(w.id)}>
                      <Trash2 size={13} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
