import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Save } from 'lucide-react';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from '../SettingsPage.module.css';

const TIMEZONES = [
  'UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
  'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Asia/Kolkata', 'Asia/Tokyo',
  'Australia/Sydney',
];
const LANGUAGES = ['English', 'Spanish', 'French', 'German', 'Japanese', 'Portuguese'];

export default function GeneralSettings() {
  const queryClient = useQueryClient();

  const { data: org } = useQuery({
    queryKey: ['org-settings'],
    queryFn: async () => {
      const r = await api.get('/org/branding');
      return r.data.data ?? {};
    },
    retry: false,
  });

  const [form, setForm] = useState({
    name: '', supportEmail: '', timezone: 'UTC', language: 'English', website: '',
  });

  React.useEffect(() => {
    if (org) setForm(f => ({ ...f, ...org }));
  }, [org]);

  const mutation = useMutation({
    mutationFn: () => api.put('/org/branding', form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org-settings'] });
      toast.success('Settings saved!');
    },
    onError: () => toast.error('Failed to save settings'),
  });

  const f = (field: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  return (
    <div>
      <h2 className={styles.sectionTitle}>General Settings</h2>
      <p className={styles.sectionSubtitle}>Configure your organization's basic information.</p>
      <hr className={styles.divider} />

      <div className={styles.formRow}>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Organization Name</label>
          <input className={styles.formInput} value={form.name} onChange={f('name')} placeholder="Acme Corp" />
        </div>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Support Email</label>
          <input className={styles.formInput} type="email" value={form.supportEmail} onChange={f('supportEmail')} placeholder="support@acme.com" />
        </div>
      </div>

      <div className={styles.formRow}>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Timezone</label>
          <select className={styles.formSelect} value={form.timezone} onChange={f('timezone')}>
            {TIMEZONES.map(tz => <option key={tz} value={tz}>{tz}</option>)}
          </select>
        </div>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Language</label>
          <select className={styles.formSelect} value={form.language} onChange={f('language')}>
            {LANGUAGES.map(l => <option key={l} value={l}>{l}</option>)}
          </select>
        </div>
      </div>

      <div className={styles.formGroup}>
        <label className={styles.formLabel}>Company Website</label>
        <input className={styles.formInput} value={form.website} onChange={f('website')} placeholder="https://acme.com" />
        <span className={styles.formHint}>Used in email footers and the public portal.</span>
      </div>

      <div className={styles.saveBtn}>
        <Button icon={<Save size={14} />} loading={mutation.isPending} onClick={() => mutation.mutate()}>
          Save Changes
        </Button>
      </div>
    </div>
  );
}
