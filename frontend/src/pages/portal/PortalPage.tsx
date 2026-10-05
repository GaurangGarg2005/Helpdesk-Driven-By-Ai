import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { MessageSquare, CheckCircle, Sparkles } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from './PortalPage.module.css';

export default function PortalPage() {
  const [submitted, setSubmitted] = useState(false);
  const [ticketNum, setTicketNum] = useState<number | null>(null);
  const [form, setForm] = useState({
    subject: '', description: '',
    requesterName: '', requesterEmail: '',
  });

  const f = (field: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  // Public portal uses orgId from URL param or a default
  const orgId = new URLSearchParams(window.location.search).get('org') ?? '';

  const submitMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post(`/tickets/public?orgId=${orgId}`, form);
      return res.data.data;
    },
    onSuccess: (data) => {
      setSubmitted(true);
      setTicketNum(data.ticketNumber);
    },
    onError: () => toast.error('Failed to submit ticket. Please try again.'),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.subject || !form.description || !form.requesterEmail) {
      toast.error('Please fill in all required fields.');
      return;
    }
    submitMutation.mutate();
  };

  return (
    <div className={styles.root}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.logo}>
          <svg width="28" height="28" viewBox="0 0 36 36" fill="none">
            <rect width="36" height="36" rx="10" fill="#4F46E5" />
            <path d="M9 12h18M9 18h12M9 24h15" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
            <circle cx="27" cy="24" r="4.5" fill="#14B8A6" />
          </svg>
          <span>HelpDeskAI Support</span>
        </div>
      </header>

      <main className={styles.main}>
        {submitted ? (
          // Success state
          <div className={styles.success}>
            <CheckCircle size={56} color="var(--success-500)" />
            <h1 className={styles.successTitle}>Ticket Submitted!</h1>
            <p className={styles.successSub}>
              Your ticket <strong>#{ticketNum}</strong> has been received and our AI
              has already classified it for faster routing.
            </p>
            <p className={styles.successEmail}>
              We'll send updates to <strong>{form.requesterEmail}</strong>.
            </p>
            <Button
              variant="secondary"
              onClick={() => { setSubmitted(false); setForm({ subject: '', description: '', requesterName: '', requesterEmail: '' }); }}
            >
              Submit another ticket
            </Button>
          </div>
        ) : (
          <div className={styles.formCard}>
            <div className={styles.formHeader}>
              <MessageSquare size={32} color="var(--primary-600)" />
              <h1 className={styles.formTitle}>Submit a Support Request</h1>
              <p className={styles.formSubtitle}>
                Describe your issue and our AI-powered support team will get back to you quickly.
              </p>
            </div>

            <div className={styles.aiBanner}>
              <Sparkles size={14} />
              <span>Your ticket will be automatically prioritized and routed by our AI.</span>
            </div>

            <form className={styles.form} onSubmit={handleSubmit}>
              <div className={styles.row}>
                <div className={styles.fieldGroup}>
                  <label className={styles.label}>Your Name <span className={styles.req}>*</span></label>
                  <input className={styles.input} placeholder="Jane Smith" value={form.requesterName} onChange={f('requesterName')} required />
                </div>
                <div className={styles.fieldGroup}>
                  <label className={styles.label}>Email Address <span className={styles.req}>*</span></label>
                  <input type="email" className={styles.input} placeholder="jane@company.com" value={form.requesterEmail} onChange={f('requesterEmail')} required />
                </div>
              </div>

              <div className={styles.fieldGroup}>
                <label className={styles.label}>Subject <span className={styles.req}>*</span></label>
                <input className={styles.input} placeholder="Brief description of your issue" value={form.subject} onChange={f('subject')} required />
              </div>

              <div className={styles.fieldGroup}>
                <label className={styles.label}>Description <span className={styles.req}>*</span></label>
                <textarea
                  className={styles.textarea}
                  placeholder="Please provide as much detail as possible…"
                  value={form.description}
                  onChange={f('description')}
                  rows={6}
                  required
                />
              </div>

              <Button type="submit" size="lg" loading={submitMutation.isPending} style={{ width: '100%' }}>
                Submit Request
              </Button>
            </form>
          </div>
        )}
      </main>
    </div>
  );
}
