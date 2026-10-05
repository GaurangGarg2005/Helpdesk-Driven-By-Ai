import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Send, Sparkles } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';

export default function CustomerTicketFormPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ subject: '', description: '' });

  const createMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/tickets/my', {
        subject: form.subject,
        description: form.description,
        channel: 'PORTAL',
      });
      return res.data.data;
    },
    onSuccess: (data) => {
      toast.success('Ticket submitted! AI is generating a reply…');
      navigate(`/customer/portal/tickets/${data.id}`);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Failed to submit ticket');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.subject.trim()) { toast.error('Please enter a subject'); return; }
    if (!form.description.trim()) { toast.error('Please describe your issue'); return; }
    createMutation.mutate();
  };

  return (
    <div style={s.page}>
      <div style={s.card}>
        {/* Header */}
        <div style={s.cardHeader}>
          <button style={s.backBtn} onClick={() => navigate('/customer/portal')}>
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 style={s.title}>New Support Request</h1>
            <p style={s.subtitle}>Describe your issue and AI will respond instantly</p>
          </div>
        </div>

        {/* AI banner */}
        <div style={s.aiBanner}>
          <Sparkles size={14} style={{ flexShrink: 0 }} />
          <span>Our AI assistant will analyze and respond to your ticket right away</span>
        </div>

        <form onSubmit={handleSubmit} style={s.form}>
          <div style={s.field}>
            <label style={s.label} htmlFor="ticket-subject">Subject *</label>
            <input
              id="ticket-subject"
              type="text"
              placeholder="Brief summary of your issue"
              style={s.input}
              value={form.subject}
              onChange={e => setForm(f => ({ ...f, subject: e.target.value }))}
              maxLength={200}
            />
            <span style={s.charCount}>{form.subject.length}/200</span>
          </div>

          <div style={s.field}>
            <label style={s.label} htmlFor="ticket-desc">Description *</label>
            <textarea
              id="ticket-desc"
              placeholder="Please describe your issue in detail. Include any error messages, steps you've tried, or relevant context…"
              style={s.textarea}
              value={form.description}
              onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              rows={7}
            />
          </div>

          <div style={s.actions}>
            <button
              type="button"
              style={s.cancelBtn}
              onClick={() => navigate('/customer/portal')}
            >
              Cancel
            </button>
            <button
              type="submit"
              style={{ ...s.submitBtn, opacity: createMutation.isPending ? 0.7 : 1 }}
              disabled={createMutation.isPending}
            >
              <Send size={14} />
              {createMutation.isPending ? 'Submitting…' : 'Submit Ticket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

const s: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    padding: '24px', fontFamily: "'Inter', system-ui, sans-serif",
  },
  card: {
    width: '100%', maxWidth: '620px',
    background: 'rgba(255,255,255,0.05)',
    backdropFilter: 'blur(20px)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '20px', padding: '36px',
    boxShadow: '0 25px 50px rgba(0,0,0,0.4)',
  },
  cardHeader: { display: 'flex', alignItems: 'flex-start', gap: '14px', marginBottom: '20px' },
  backBtn: {
    padding: '8px', background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '8px', color: '#94a3b8', cursor: 'pointer',
    display: 'flex', alignItems: 'center', flexShrink: 0, marginTop: '2px',
  },
  title: { fontSize: '20px', fontWeight: 700, color: '#f8fafc', margin: '0 0 4px' },
  subtitle: { fontSize: '13px', color: '#64748b', margin: 0 },
  aiBanner: {
    display: 'flex', alignItems: 'center', gap: '8px',
    padding: '10px 14px', marginBottom: '24px',
    background: 'rgba(99,102,241,0.12)',
    border: '1px solid rgba(99,102,241,0.2)',
    borderRadius: '10px', color: '#a5b4fc', fontSize: '13px',
  },
  form: { display: 'flex', flexDirection: 'column', gap: '18px' },
  field: { display: 'flex', flexDirection: 'column', gap: '6px', position: 'relative' },
  label: { fontSize: '13px', fontWeight: 500, color: '#cbd5e1' },
  input: {
    padding: '10px 14px',
    background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '10px', color: '#f1f5f9',
    fontSize: '14px', outline: 'none', boxSizing: 'border-box', width: '100%',
  },
  charCount: { fontSize: '11px', color: '#475569', alignSelf: 'flex-end' },
  textarea: {
    padding: '10px 14px', resize: 'vertical',
    background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '10px', color: '#f1f5f9',
    fontSize: '14px', outline: 'none', boxSizing: 'border-box',
    width: '100%', fontFamily: 'inherit', lineHeight: '1.6',
  },
  actions: { display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '4px' },
  cancelBtn: {
    padding: '10px 20px',
    background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '8px', color: '#94a3b8', cursor: 'pointer', fontSize: '14px',
  },
  submitBtn: {
    display: 'flex', alignItems: 'center', gap: '6px',
    padding: '10px 22px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    color: '#fff', border: 'none', borderRadius: '8px',
    fontSize: '14px', fontWeight: 600, cursor: 'pointer',
    boxShadow: '0 4px 14px rgba(99,102,241,0.4)',
  },
};
