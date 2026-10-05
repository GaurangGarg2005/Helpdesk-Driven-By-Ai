import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Plus, Trash2, Copy, Key } from 'lucide-react';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from '../SettingsPage.module.css';
import { format } from 'date-fns';

const SCOPES = ['tickets.read', 'tickets.write', 'kb.read', 'kb.write', 'analytics.read', 'org.manage'];

interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  scopes: string;
  isActive: boolean;
  createdAt: string;
  lastUsedAt?: string;
}

export default function ApiKeysSettings() {
  const queryClient = useQueryClient();
  const [showNew, setShowNew] = useState(false);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [form, setForm] = useState({ name: '', scopes: [] as string[] });

  const { data: keys = [], isLoading } = useQuery<ApiKey[]>({
    queryKey: ['api-keys'],
    queryFn: async () => {
      const r = await api.get('/org/api-keys');
      return r.data.data ?? [];
    },
  });

  const createMutation = useMutation({
    mutationFn: () => api.post('/org/api-keys', form),
    onSuccess: (res) => {
      setNewKey(res.data.data.plainTextKey);
      setForm({ name: '', scopes: [] });
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
      toast.success('API Key created!');
    },
    onError: () => toast.error('Failed to create API Key'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/org/api-keys/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
      toast.success('API Key deleted');
    },
  });

  const toggleScope = (scope: string) => {
    setForm(prev => ({
      ...prev,
      scopes: prev.scopes.includes(scope) ? prev.scopes.filter(s => s !== scope) : [...prev.scopes, scope],
    }));
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 'var(--space-1)' }}>
        <h2 className={styles.sectionTitle}>API Keys</h2>
        <Button size="sm" icon={<Plus size={14} />} onClick={() => { setShowNew(!showNew); setNewKey(null); }}>
          {showNew ? 'Cancel' : 'New Key'}
        </Button>
      </div>
      <p className={styles.sectionSubtitle}>Use API keys to integrate with HelpDeskAI programmatically.</p>
      <hr className={styles.divider} />

      {/* New key form */}
      {showNew && (
        <div style={{ background: 'var(--neutral-50)', border: '1px solid var(--neutral-200)', borderRadius: 'var(--radius-lg)', padding: 'var(--space-4)', marginBottom: 'var(--space-4)' }}>
          <div className={styles.formGroup} style={{ marginBottom: 'var(--space-3)' }}>
            <label className={styles.formLabel}>Key Name</label>
            <input className={styles.formInput} placeholder="e.g. Production Integration"
              value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
          </div>
          <div className={styles.formGroup} style={{ marginBottom: 'var(--space-4)' }}>
            <label className={styles.formLabel}>Scopes</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-2)', marginTop: 'var(--space-1)' }}>
              {SCOPES.map(scope => (
                <button key={scope}
                  style={{
                    padding: '4px 12px', borderRadius: 'var(--radius-full)', fontSize: 'var(--text-caption)',
                    fontWeight: 600, border: '1px solid',
                    borderColor: form.scopes.includes(scope) ? 'var(--primary-500)' : 'var(--neutral-200)',
                    background: form.scopes.includes(scope) ? 'var(--primary-50)' : 'white',
                    color: form.scopes.includes(scope) ? 'var(--primary-700)' : 'var(--neutral-500)',
                  }}
                  onClick={() => toggleScope(scope)}>{scope}</button>
              ))}
            </div>
          </div>
          <Button size="sm" icon={<Key size={14} />} loading={createMutation.isPending}
            onClick={() => createMutation.mutate()} disabled={!form.name}>
            Generate Key
          </Button>

          {/* Reveal box */}
          {newKey && (
            <div className={styles.keyRevealBox}>
              <p className={styles.keyRevealLabel}>Your new API key — copy it now, it won't be shown again:</p>
              <p className={styles.keyRevealValue}>{newKey}</p>
              <button className={styles.keyRevealWarning}
                style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 8 }}
                onClick={() => { navigator.clipboard.writeText(newKey); toast.success('Copied!'); }}>
                <Copy size={12} /> Copy to clipboard
              </button>
            </div>
          )}
        </div>
      )}

      {/* Keys table */}
      {isLoading ? (
        <p style={{ color: 'var(--neutral-400)', fontSize: 'var(--text-body-sm)' }}>Loading…</p>
      ) : keys.length === 0 ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}><Key size={20} /></div>
          <p>No API keys yet. Create one to get started.</p>
        </div>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Prefix</th>
                <th>Scopes</th>
                <th>Last Used</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {keys.map(k => (
                <tr key={k.id}>
                  <td style={{ fontWeight: 600 }}>{k.name}</td>
                  <td><span className={styles.mono}>{k.keyPrefix}…</span></td>
                  <td style={{ fontSize: 11, color: 'var(--neutral-500)' }}>{k.scopes || '—'}</td>
                  <td style={{ color: 'var(--neutral-400)' }}>{k.lastUsedAt ? format(new Date(k.lastUsedAt), 'MMM d, yyyy') : 'Never'}</td>
                  <td style={{ color: 'var(--neutral-400)' }}>{format(new Date(k.createdAt), 'MMM d, yyyy')}</td>
                  <td>
                    <button className={styles.deleteBtn} onClick={() => deleteMutation.mutate(k.id)}>
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
