import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { Sparkles } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import Button from '@/components/ui/Button';
import styles from './NewTicketPage.module.css';

export default function NewTicketPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const [form, setForm] = useState({
    subject: '',
    description: '',
    tags: '',
  });

  const f = (field: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  const createMutation = useMutation({
    mutationFn: async () => {
      // Channel is always PORTAL; requester info comes from the logged-in user's profile
      const payload: Record<string, any> = {
        subject: form.subject,
        description: form.description,
        channel: 'PORTAL',
        requesterEmail: user?.email,
        requesterName: `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.email,
      };
      if (form.tags.trim()) payload.tags = form.tags;
      const res = await api.post('/tickets', payload);
      return res.data.data;
    },
    onSuccess: (ticket) => {
      toast.success(`Ticket #${ticket.ticketNumber} created — AI has classified it!`);
      navigate(`/tickets/${ticket.id}`);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Something went wrong. Please try again.');
    },
  });

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1 className={styles.title}>New Ticket</h1>
          <p className={styles.subtitle}>
            Describe your issue and our AI will automatically classify and prioritise it.
          </p>
        </div>

        <div className={styles.aiHint}>
          <Sparkles size={16} />
          <span>
            AI will automatically detect <strong>priority</strong>, <strong>category</strong>, and <strong>sentiment</strong> from your description.
          </span>
        </div>

        <form className={styles.form} onSubmit={e => { e.preventDefault(); createMutation.mutate(); }}>
          {/* Subject */}
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Subject <span className={styles.req}>*</span></label>
            <input
              className={styles.input}
              placeholder="Brief description of the issue"
              value={form.subject}
              onChange={f('subject')}
              required
            />
          </div>

          {/* Description */}
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Description <span className={styles.req}>*</span></label>
            <textarea
              className={styles.textarea}
              placeholder="Describe the issue in detail. The more you write, the better our AI can classify it."
              value={form.description}
              onChange={f('description')}
              rows={7}
              required
            />
          </div>

          {/* Tags */}
          <div className={styles.fieldGroup}>
            <label className={styles.label}>
              Tags <span className={styles.optional}>(optional)</span>
            </label>
            <input
              className={styles.input}
              placeholder="login, mobile, urgent (comma separated)"
              value={form.tags}
              onChange={f('tags')}
            />
          </div>

          <div className={styles.formActions}>
            <Button type="button" variant="ghost" onClick={() => navigate(-1)}>Cancel</Button>
            <Button type="submit" loading={createMutation.isPending} icon={<Sparkles size={14} />}>
              Create Ticket with AI
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
