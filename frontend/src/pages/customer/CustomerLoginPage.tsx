import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, HeadphonesIcon } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

export default function CustomerLoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore(s => s.setAuth);
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState({ email: '', password: '' });

  const loginMutation = useMutation({
    mutationFn: async (data: typeof form) => {
      const res = await api.post('/auth/login', data);
      return res.data.data;
    },
    onSuccess: (data) => {
      // OWNER can log in from any portal — always redirect to main dashboard
      if (data.user.role === 'OWNER') {
        setAuth(data.accessToken, data.refreshToken, data.user);
        toast.success(`Welcome back, ${data.user.firstName}!`);
        navigate('/dashboard');
        return;
      }
      if (data.user.role !== 'CUSTOMER') {
        toast.error('This portal is for customers only. Please use the agent login.');
        return;
      }
      setAuth(data.accessToken, data.refreshToken, data.user);
      toast.success(`Welcome, ${data.user.firstName}!`);
      navigate('/customer/portal');
    },
    onError: () => {
      toast.error('Invalid email or password.');
    },
  });


  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.email || !form.password) { toast.error('Please fill in all fields'); return; }
    loginMutation.mutate(form);
  };

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        {/* Header */}
        <div style={styles.header}>
          <div style={styles.iconBadge}>
            <HeadphonesIcon size={20} color="#fff" />
          </div>
          <h1 style={styles.title}>Customer Portal</h1>
          <p style={styles.subtitle}>Sign in to track and manage your support requests</p>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.field}>
            <label style={styles.label} htmlFor="c-email">Email address</label>
            <div style={styles.inputWrap}>
              <Mail size={16} style={styles.icon} />
              <input
                id="c-email"
                type="email"
                placeholder="you@example.com"
                style={styles.input}
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                autoComplete="email"
              />
            </div>
          </div>

          <div style={styles.field}>
            <label style={styles.label} htmlFor="c-password">Password</label>
            <div style={styles.inputWrap}>
              <Lock size={16} style={styles.icon} />
              <input
                id="c-password"
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                style={styles.input}
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                autoComplete="current-password"
              />
              <button type="button" style={styles.eyeBtn} onClick={() => setShowPassword(p => !p)}>
                {showPassword ? <Eye size={15} /> : <EyeOff size={15} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            style={{
              ...styles.submitBtn,
              opacity: loginMutation.isPending ? 0.7 : 1,
              cursor: loginMutation.isPending ? 'not-allowed' : 'pointer',
            }}
            disabled={loginMutation.isPending}
          >
            {loginMutation.isPending ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div style={styles.footer}>
          <p style={styles.footerText}>
            New customer?{' '}
            <Link to="/customer/register" style={styles.link}>Create an account</Link>
          </p>
          <p style={styles.footerText}>
            Support agent?{' '}
            <Link to="/login" style={styles.agentLink}>Agent login →</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '24px',
    fontFamily: "'Inter', 'Outfit', system-ui, sans-serif",
  },
  card: {
    background: 'rgba(255,255,255,0.05)',
    backdropFilter: 'blur(20px)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '20px',
    padding: '40px',
    width: '100%',
    maxWidth: '420px',
    boxShadow: '0 25px 50px rgba(0,0,0,0.4)',
  },
  header: { textAlign: 'center', marginBottom: '32px' },
  iconBadge: {
    width: '52px', height: '52px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    borderRadius: '16px', display: 'flex',
    alignItems: 'center', justifyContent: 'center',
    margin: '0 auto 16px',
    boxShadow: '0 8px 24px rgba(99,102,241,0.4)',
  },
  title: { fontSize: '24px', fontWeight: 700, color: '#f8fafc', margin: '0 0 6px' },
  subtitle: { fontSize: '14px', color: '#94a3b8', margin: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '20px' },
  field: { display: 'flex', flexDirection: 'column', gap: '6px' },
  label: { fontSize: '13px', fontWeight: 500, color: '#cbd5e1' },
  inputWrap: { position: 'relative', display: 'flex', alignItems: 'center' },
  icon: { position: 'absolute', left: '12px', color: '#64748b' },
  input: {
    width: '100%', padding: '10px 12px 10px 36px',
    background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '10px', color: '#f1f5f9',
    fontSize: '14px', outline: 'none',
    transition: 'border-color 0.2s',
    boxSizing: 'border-box',
  },
  eyeBtn: {
    position: 'absolute', right: '12px',
    background: 'none', border: 'none',
    color: '#64748b', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },
  submitBtn: {
    marginTop: '4px',
    padding: '12px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    color: '#fff', border: 'none',
    borderRadius: '10px', fontSize: '15px',
    fontWeight: 600, cursor: 'pointer',
    transition: 'transform 0.15s, box-shadow 0.15s',
    boxShadow: '0 4px 14px rgba(99,102,241,0.4)',
  },
  footer: { marginTop: '24px', display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' },
  footerText: { fontSize: '13px', color: '#94a3b8', margin: 0 },
  link: { color: '#818cf8', textDecoration: 'none', fontWeight: 500 },
  agentLink: { color: '#64748b', textDecoration: 'none', fontSize: '12px' },
};
