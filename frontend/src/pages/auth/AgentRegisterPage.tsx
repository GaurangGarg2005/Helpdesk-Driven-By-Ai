import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, User, Building2, Shield, ChevronDown, Check } from 'lucide-react';
import { useMutation, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import Button from '@/components/ui/Button';
import styles from './AuthPage.module.css';

interface OrgResult { id: string; name: string; slug: string; }

export default function AgentRegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore(s => s.setAuth);
  const [showPassword, setShowPassword] = useState(false);
  const [orgSearch, setOrgSearch] = useState('');
  const [selectedOrg, setSelectedOrg] = useState<OrgResult | null>(null);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', password: '',
  });

  // Live org search
  const { data: orgResults, isFetching: searchingOrgs } = useQuery({
    queryKey: ['org-search-agent', orgSearch],
    queryFn: async () => {
      if (orgSearch.length < 2) return [];
      const res = await api.get(`/auth/organizations/search?name=${encodeURIComponent(orgSearch)}`);
      return res.data.data as OrgResult[];
    },
    enabled: orgSearch.length >= 2,
  });

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const registerMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/auth/register/agent', {
        ...form,
        companyName: selectedOrg!.name,
      });
      return res.data.data;
    },
    onSuccess: (data) => {
      setAuth(data.accessToken, data.refreshToken, data.user);
      toast.success(`Welcome, ${data.user.firstName}! Your agent account is ready.`);
      navigate('/dashboard');
    },
    onError: (error: any) => {
      // Error toast already shown by api.ts interceptor
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.firstName || !form.email || !form.password) {
      toast.error('Please fill in all required fields'); return;
    }
    if (!selectedOrg) {
      toast.error('Please select your company from the dropdown'); return;
    }
    if (form.password.length < 8) {
      toast.error('Password must be at least 8 characters'); return;
    }
    registerMutation.mutate();
  };

  const handleOrgSelect = (org: OrgResult) => {
    setSelectedOrg(org);
    setOrgSearch(org.name);
    setShowDropdown(false);
  };

  const f = (field: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.logoBadge}>
          <Shield size={14} />
        </div>
        <h1 className={styles.title}>Join as a support agent</h1>
        <p className={styles.subtitle}>Select your company and create your agent account</p>
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

        {/* Company dropdown */}
        <div className={styles.fieldGroup}>
          <label className={styles.label}>Your company <span className={styles.required}>*</span></label>
          <div ref={dropdownRef} style={{ position: 'relative' }}>
            <div className={styles.inputWrapper}>
              <Building2 size={16} className={styles.inputIcon} />
              <input
                placeholder="Search for your company..."
                className={styles.input}
                value={orgSearch}
                onChange={e => {
                  setOrgSearch(e.target.value);
                  setSelectedOrg(null);
                  setShowDropdown(true);
                }}
                onFocus={() => orgSearch.length >= 2 && setShowDropdown(true)}
                autoComplete="off"
              />
              <ChevronDown size={14} style={{ position: 'absolute', right: 12, color: 'var(--neutral-500)', pointerEvents: 'none' }} />
            </div>
            {showDropdown && orgSearch.length >= 2 && (
              <div style={{
                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 50,
                background: 'var(--surface-overlay)', border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-lg)', marginTop: 4,
                boxShadow: 'var(--shadow-lg)', overflow: 'hidden',
              }}>
                {searchingOrgs ? (
                  <div style={{ padding: '12px 16px', color: 'var(--neutral-500)', fontSize: '13px' }}>Searching...</div>
                ) : orgResults && orgResults.length > 0 ? (
                  orgResults.map(org => (
                    <button
                      key={org.id}
                      type="button"
                      onClick={() => handleOrgSelect(org)}
                      style={{
                        width: '100%', padding: '10px 16px', textAlign: 'left',
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        background: 'transparent', color: 'var(--neutral-200)',
                        fontSize: '13px', cursor: 'pointer', transition: 'background 0.15s',
                      }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-hover)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <span>{org.name}</span>
                      {selectedOrg?.id === org.id && <Check size={14} color="var(--primary-400)" />}
                    </button>
                  ))
                ) : (
                  <div style={{ padding: '12px 16px', color: 'var(--neutral-500)', fontSize: '13px' }}>
                    No companies found. Ask your admin to register first.
                  </div>
                )}
              </div>
            )}
          </div>
          {selectedOrg && (
            <p style={{ fontSize: '12px', color: 'var(--success-400)', marginTop: 4 }}>
              ✓ Selected: <strong>{selectedOrg.name}</strong>
            </p>
          )}
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
          Create agent account
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
        Registering a new company?{' '}
        <Link to="/register" className={styles.switchLink} style={{ fontSize: '13px' }}>Create company account →</Link>
      </p>
    </div>
  );
}
