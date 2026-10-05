import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Save, ExternalLink } from 'lucide-react';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from '../SettingsPage.module.css';

interface Branding {
  logoUrl?: string;
  faviconUrl?: string;
  primaryColor?: string;
  accentColor?: string;
  headingFont?: string;
  bodyFont?: string;
  customDomain?: string;
  portalTitle?: string;
  portalSubtitle?: string;
}

const FONTS = ['Inter', 'Roboto', 'Outfit', 'Poppins', 'DM Sans', 'Sora', 'Nunito'];

export default function BrandingSettings() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Branding>({
    primaryColor: '#4F46E5',
    accentColor: '#14B8A6',
  });

  const { data } = useQuery({
    queryKey: ['org-branding'],
    queryFn: async () => {
      const r = await api.get('/org/branding');
      return r.data.data as Branding;
    },
  });

  React.useEffect(() => {
    if (data) setForm(f => ({ ...f, ...data }));
  }, [data]);

  const mutation = useMutation({
    mutationFn: () => api.put('/org/branding', form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org-branding'] });
      toast.success('Branding updated!');
    },
    onError: () => toast.error('Failed to save branding'),
  });

  const f = (field: keyof Branding) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  return (
    <div>
      <h2 className={styles.sectionTitle}>Custom Branding</h2>
      <p className={styles.sectionSubtitle}>Personalize your support portal and email templates.</p>
      <hr className={styles.divider} />

      {/* Colors */}
      <h3 style={{ fontWeight: 700, color: 'var(--neutral-700)', fontSize: 'var(--text-body-sm)', marginBottom: 'var(--space-3)' }}>Brand Colors</h3>
      <div className={styles.formRow}>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Primary Color</label>
          <div className={styles.colorRow}>
            <div className={styles.colorPreview} style={{ background: form.primaryColor }} />
            <input type="text" className={`${styles.formInput} ${styles.colorInput}`}
              value={form.primaryColor} onChange={f('primaryColor')} placeholder="#4F46E5" maxLength={7} />
            <input type="color" value={form.primaryColor} style={{ width: 36, height: 36, border: 'none', cursor: 'pointer', background: 'none' }}
              onChange={f('primaryColor')} />
          </div>
        </div>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Accent Color</label>
          <div className={styles.colorRow}>
            <div className={styles.colorPreview} style={{ background: form.accentColor }} />
            <input type="text" className={`${styles.formInput} ${styles.colorInput}`}
              value={form.accentColor} onChange={f('accentColor')} placeholder="#14B8A6" maxLength={7} />
            <input type="color" value={form.accentColor} style={{ width: 36, height: 36, border: 'none', cursor: 'pointer', background: 'none' }}
              onChange={f('accentColor')} />
          </div>
        </div>
      </div>

      <hr className={styles.divider} />

      {/* Fonts */}
      <h3 style={{ fontWeight: 700, color: 'var(--neutral-700)', fontSize: 'var(--text-body-sm)', marginBottom: 'var(--space-3)' }}>Typography</h3>
      <div className={styles.formRow}>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Heading Font</label>
          <select className={styles.formSelect} value={form.headingFont ?? 'Inter'} onChange={f('headingFont')}>
            {FONTS.map(font => <option key={font} value={font}>{font}</option>)}
          </select>
        </div>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Body Font</label>
          <select className={styles.formSelect} value={form.bodyFont ?? 'Inter'} onChange={f('bodyFont')}>
            {FONTS.map(font => <option key={font} value={font}>{font}</option>)}
          </select>
        </div>
      </div>

      <hr className={styles.divider} />

      {/* Assets */}
      <h3 style={{ fontWeight: 700, color: 'var(--neutral-700)', fontSize: 'var(--text-body-sm)', marginBottom: 'var(--space-3)' }}>Assets & Domain</h3>
      <div className={styles.formRow}>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Logo URL</label>
          <input className={styles.formInput} value={form.logoUrl ?? ''} onChange={f('logoUrl')} placeholder="https://cdn.acme.com/logo.png" />
        </div>
        <div className={styles.formGroup}>
          <label className={styles.formLabel}>Favicon URL</label>
          <input className={styles.formInput} value={form.faviconUrl ?? ''} onChange={f('faviconUrl')} placeholder="https://cdn.acme.com/favicon.ico" />
        </div>
      </div>

      <div className={styles.formGroup} style={{ marginBottom: 'var(--space-4)' }}>
        <label className={styles.formLabel}>Custom Domain</label>
        <input className={styles.formInput} value={form.customDomain ?? ''} onChange={f('customDomain')} placeholder="support.acme.com" />
        <span className={styles.formHint}>Configure a CNAME pointing to <code>portal.helpdeskAi.com</code></span>
      </div>

      <hr className={styles.divider} />

      {/* Portal copy */}
      <h3 style={{ fontWeight: 700, color: 'var(--neutral-700)', fontSize: 'var(--text-body-sm)', marginBottom: 'var(--space-3)' }}>Portal Copy</h3>
      <div className={styles.formGroup} style={{ marginBottom: 'var(--space-4)' }}>
        <label className={styles.formLabel}>Portal Title</label>
        <input className={styles.formInput} value={form.portalTitle ?? ''} onChange={f('portalTitle')} placeholder="Acme Support Center" />
      </div>
      <div className={styles.formGroup} style={{ marginBottom: 'var(--space-4)' }}>
        <label className={styles.formLabel}>Portal Subtitle</label>
        <textarea className={styles.formTextarea} value={form.portalSubtitle ?? ''} onChange={(e) => setForm(p => ({ ...p, portalSubtitle: e.target.value }))}
          placeholder="We're here to help. Submit a ticket or search the knowledge base." rows={2} />
      </div>

      <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }} className={styles.saveBtn}>
        <Button icon={<Save size={14} />} loading={mutation.isPending} onClick={() => mutation.mutate()}>
          Save Branding
        </Button>
        <a href="/portal" target="_blank" rel="noreferrer" style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 'var(--text-body-sm)', color: 'var(--primary-600)', fontWeight: 600 }}>
          <ExternalLink size={13} /> Preview Portal
        </a>
      </div>
    </div>
  );
}
