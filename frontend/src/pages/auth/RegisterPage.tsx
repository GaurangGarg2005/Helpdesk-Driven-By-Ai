import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, Building2, User, Sparkles, Users } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import Button from '@/components/ui/Button';
import styles from './AuthPage.module.css';

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore(s => s.setAuth);
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState({
    firstName: '', lastName: '',
    email: '', password: '',
    organizationName: '',
  });

  const registerMutation = useMutation({
    mutationFn: async (data: typeof form) => {
      const res = await api.post('/auth/register', data);
      return res.data.data;
    },
    onSuccess: (data) => {
      setAuth(data.accessToken, data.refreshToken, data.user);
      toast.success('Company account created! Welcome to HelpDeskAI 🎉');
      navigate('/dashboard');
    },
  });

  const f = (field: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.email || !form.password || !form.organizationName || !form.firstName) {
      toast.error('Please fill in all required fields');
      return;
    }
    if (form.password.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }
    registerMutation.mutate(form);
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.logoBadge}>
          <Sparkles size={14} />
        </div>
        <h1 className={styles.title}>Register your company</h1>
        <p className={styles.subtitle}>Create a company admin account and start managing support</p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.row}>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>First name <span className={styles.required}>*</span></label>
            <div className={styles.inputWrapper}>
              <User size={16} className={styles.inputIcon} />
              <input placeholder="Jane" className={styles.input} value={form.firstName} onChange={f('firstName')} required />
            </div>
          </div>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Last name</label>
            <div className={styles.inputWrapper}>
              <input placeholder="Smith" className={styles.input} style={{ paddingLeft: 14 }} value={form.lastName} onChange={f('lastName')} />
            </div>
          </div>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Company name <span className={styles.required}>*</span></label>
          <div className={styles.inputWrapper}>
            <Building2 size={16} className={styles.inputIcon} />
            <input placeholder="Acme Corp" className={styles.input} value={form.organizationName} onChange={f('organizationName')} required />
          </div>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Work email <span className={styles.required}>*</span></label>
          <div className={styles.inputWrapper}>
            <Mail size={16} className={styles.inputIcon} />
            <input type="email" placeholder="jane@acme.com" className={styles.input} value={form.email} onChange={f('email')} required />
          </div>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Password <span className={styles.required}>*</span></label>
          <div className={styles.inputWrapper}>
            <Lock size={16} className={styles.inputIcon} />
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Min. 8 characters"
              className={styles.input}
              value={form.password}
              onChange={f('password')}
              required
            />
            <button type="button" className={styles.eyeBtn} onClick={() => setShowPassword(p => !p)}>
              {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
            </button>
          </div>
        </div>

        <Button
          type="submit"
          size="lg"
          loading={registerMutation.isPending}
          style={{ width: '100%', marginTop: 4 }}
        >
          Create company account
        </Button>

        <p className={styles.terms}>
          By signing up, you agree to our <a href="#">Terms</a> and <a href="#">Privacy Policy</a>.
        </p>
      </form>

      <p className={styles.switchText}>
        Already have an account?{' '}
        <Link to="/login" className={styles.switchLink}>Sign in</Link>
      </p>
      <p className={styles.switchText} style={{ marginTop: 4 }}>
        <Link to="/register/agent" className={styles.switchLink} style={{ opacity: 0.7, fontSize: '13px' }}>
          <Users size={12} style={{ display: 'inline', marginRight: 4 }} />
          Joining as a support agent? Register here →
        </Link>
      </p>
    </div>
  );
}
