import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, User, Building2, HeadphonesIcon, ChevronDown, Check } from 'lucide-react';
import { useMutation, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

interface OrgResult { id: string; name: string; slug: string; }

export default function CustomerRegisterPage() {
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
    queryKey: ['org-search', orgSearch],
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
      const res = await api.post('/auth/register/customer', {
        ...form,
        companyName: selectedOrg!.name,
      });
      return res.data.data;
    },
    onSuccess: (data) => {
      setAuth(data.accessToken, data.refreshToken, data.user);
      toast.success('Account created! Welcome to the customer portal.');
      navigate('/customer/portal');
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Registration failed');
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

  return (
    <div style={s.page}>
      <div style={s.card}>
        <div style={s.header}>
          <div style={s.iconBadge}><HeadphonesIcon size={20} color="#fff" /></div>
          <h1 style={s.title}>Create Customer Account</h1>
          <p style={s.subtitle}>Join your company's support portal</p>
        </div>

        <form onSubmit={handleSubmit} style={s.form}>
          {/* Name row */}
          <div style={s.row}>
            <div style={s.field}>
              <label style={s.label}>First name *</label>
              <div style={s.inputWrap}>
                <User size={15} style={s.icon} />
                <input
                  type="text" placeholder="John" style={s.input}
                  value={form.firstName}
                  onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))}
                />
              </div>
            </div>
            <div style={s.field}>
              <label style={s.label}>Last name</label>
              <div style={s.inputWrap}>
                <User size={15} style={s.icon} />
                <input
                  type="text" placeholder="Doe" style={s.input}
                  value={form.lastName}
                  onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))}
                />
              </div>
            </div>
          </div>

          {/* Email */}
          <div style={s.field}>
            <label style={s.label}>Email address *</label>
            <div style={s.inputWrap}>
              <Mail size={15} style={s.icon} />
              <input
                type="email" placeholder="john@company.com" style={s.input}
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
              />
            </div>
          </div>

          {/* Company search with dropdown */}
          <div style={s.field} ref={dropdownRef}>
            <label style={s.label}>Company *</label>
            <div style={s.inputWrap}>
              <Building2 size={15} style={s.icon} />
              <input
                type="text" placeholder="Search your company…" style={s.input}
                value={orgSearch}
                onChange={e => {
                  setOrgSearch(e.target.value);
                  setSelectedOrg(null);
                  setShowDropdown(true);
                }}
                onFocus={() => orgSearch.length >= 2 && setShowDropdown(true)}
              />
              {selectedOrg
                ? <Check size={15} style={{ position: 'absolute', right: 12, color: '#4ade80' }} />
                : <ChevronDown size={15} style={{ position: 'absolute', right: 12, color: '#64748b' }} />
              }
            </div>

            {/* Dropdown */}
            {showDropdown && orgSearch.length >= 2 && (
              <div style={s.dropdown}>
                {searchingOrgs && (
                  <div style={s.dropdownItem}>Searching…</div>
                )}
                {!searchingOrgs && orgResults && orgResults.length === 0 && (
                  <div style={{ ...s.dropdownItem, color: '#f87171' }}>
                    No company found with that name. Contact your admin.
                  </div>
                )}
                {orgResults?.map(org => (
                  <div
                    key={org.id}
                    style={{
                      ...s.dropdownItem,
                      background: selectedOrg?.id === org.id ? 'rgba(99,102,241,0.2)' : undefined,
                    }}
                    onMouseDown={() => handleOrgSelect(org)}
                  >
                    <Building2 size={13} style={{ marginRight: 8, opacity: 0.6 }} />
                    {org.name}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Password */}
          <div style={s.field}>
            <label style={s.label}>Password *</label>
            <div style={s.inputWrap}>
              <Lock size={15} style={s.icon} />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Min. 8 characters" style={s.input}
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
              />
              <button type="button" style={s.eyeBtn} onClick={() => setShowPassword(p => !p)}>
                {showPassword ? <Eye size={14} /> : <EyeOff size={14} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            style={{ ...s.submitBtn, opacity: registerMutation.isPending ? 0.7 : 1 }}
            disabled={registerMutation.isPending}
          >
            {registerMutation.isPending ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <div style={s.footer}>
          <p style={s.footerText}>
            Already have an account?{' '}
            <Link to="/customer/login" style={s.link}>Sign in</Link>
          </p>
          <p style={s.footerText}>
            Support agent?{' '}
            <Link to="/login" style={s.agentLink}>Agent login →</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

const s: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    padding: '24px', fontFamily: "'Inter', 'Outfit', system-ui, sans-serif",
  },
  card: {
    background: 'rgba(255,255,255,0.05)',
    backdropFilter: 'blur(20px)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '20px', padding: '40px',
    width: '100%', maxWidth: '480px',
    boxShadow: '0 25px 50px rgba(0,0,0,0.4)',
  },
  header: { textAlign: 'center', marginBottom: '28px' },
  iconBadge: {
    width: '52px', height: '52px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    borderRadius: '16px', display: 'flex',
    alignItems: 'center', justifyContent: 'center',
    margin: '0 auto 16px', boxShadow: '0 8px 24px rgba(99,102,241,0.4)',
  },
  title: { fontSize: '22px', fontWeight: 700, color: '#f8fafc', margin: '0 0 6px' },
  subtitle: { fontSize: '14px', color: '#94a3b8', margin: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '16px' },
  row: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' },
  field: { display: 'flex', flexDirection: 'column', gap: '6px', position: 'relative' },
  label: { fontSize: '13px', fontWeight: 500, color: '#cbd5e1' },
  inputWrap: { position: 'relative', display: 'flex', alignItems: 'center' },
  icon: { position: 'absolute', left: '12px', color: '#64748b' },
  input: {
    width: '100%', padding: '10px 36px 10px 34px',
    background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '10px', color: '#f1f5f9',
    fontSize: '14px', outline: 'none', boxSizing: 'border-box',
  },
  eyeBtn: {
    position: 'absolute', right: '12px', background: 'none',
    border: 'none', color: '#64748b', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },
  dropdown: {
    position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 100,
    background: '#1e293b', border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '10px', marginTop: '4px',
    boxShadow: '0 12px 32px rgba(0,0,0,0.5)', overflow: 'hidden',
  },
  dropdownItem: {
    padding: '10px 14px', color: '#cbd5e1', fontSize: '14px',
    cursor: 'pointer', display: 'flex', alignItems: 'center',
    transition: 'background 0.15s',
  },
  submitBtn: {
    marginTop: '4px', padding: '12px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    color: '#fff', border: 'none', borderRadius: '10px',
    fontSize: '15px', fontWeight: 600, cursor: 'pointer',
    boxShadow: '0 4px 14px rgba(99,102,241,0.4)',
  },
  footer: { marginTop: '20px', display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' },
  footerText: { fontSize: '13px', color: '#94a3b8', margin: 0 },
  link: { color: '#818cf8', textDecoration: 'none', fontWeight: 500 },
  agentLink: { color: '#64748b', textDecoration: 'none', fontSize: '12px' },
};
