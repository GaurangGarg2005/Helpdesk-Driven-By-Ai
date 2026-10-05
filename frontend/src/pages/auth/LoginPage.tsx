import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, Zap } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import Button from '@/components/ui/Button';
import styles from './AuthPage.module.css';

export default function LoginPage() {
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
      setAuth(data.accessToken, data.refreshToken, data.user);
      toast.success(`Welcome back, ${data.user.firstName}!`);
      // Role-aware redirect
      if (data.user.role === 'CUSTOMER') {
        navigate('/customer/portal');
      } else if (data.user.role === 'AGENT') {
        navigate('/tickets');
      } else {
        navigate('/dashboard');
      }
    },
  });


  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.email || !form.password) {
      toast.error('Please fill in all fields');
      return;
    }
    loginMutation.mutate(form);
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.logoBadge}>
          <Zap size={14} />
        </div>
        <h1 className={styles.title}>Welcome back</h1>
        <p className={styles.subtitle}>Sign in to your HelpDeskAI account</p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        {/* Email */}
        <div className={styles.fieldGroup}>
          <label className={styles.label} htmlFor="email">Email address</label>
          <div className={styles.inputWrapper}>
            <Mail size={16} className={styles.inputIcon} />
            <input
              id="email"
              type="email"
              placeholder="you@company.com"
              className={styles.input}
              value={form.email}
              onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
              autoComplete="email"
            />
          </div>
        </div>

        {/* Password */}
        <div className={styles.fieldGroup}>
          <label className={styles.label} htmlFor="password">
            Password
            <Link to="/forgot-password" className={styles.forgotLink}>Forgot password?</Link>
          </label>
          <div className={styles.inputWrapper}>
            <Lock size={16} className={styles.inputIcon} />
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              className={styles.input}
              value={form.password}
              onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
              autoComplete="current-password"
            />
            <button
              type="button"
              className={styles.eyeBtn}
              onClick={() => setShowPassword(p => !p)}
            >
              {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
            </button>
          </div>
        </div>

        <Button
          type="submit"
          size="lg"
          loading={loginMutation.isPending}
          style={{ width: '100%', marginTop: 4 }}
        >
          Sign in
        </Button>
      </form>

      <p className={styles.switchText}>
        Don't have an account?{' '}
        <Link to="/register" className={styles.switchLink}>Create one free</Link>
      </p>
      <p className={styles.switchText} style={{ marginTop: 4 }}>
        <Link to="/customer/login" className={styles.switchLink} style={{ opacity: 0.6, fontSize: '13px' }}>
          Customer? Sign in to the customer portal →
        </Link>
      </p>
    </div>
  );
}
