import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Ticket, BookOpen,
  BarChart2, Users, Settings, LogOut, Bot, ChevronRight, BrainCircuit
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import styles from './Sidebar.module.css';

interface NavItem {
  icon: React.ReactNode;
  label: string;
  to: string;
  badge?: number;
}



export default function Sidebar() {
  const { user, logout, isAdmin, isOwner, isAgent } = useAuthStore();
  const navigate = useNavigate();

  const navItems: NavItem[] = [
    // OWNER / ADMIN see Dashboard
    ...(!isAgent() ? [{ icon: <LayoutDashboard size={18} />, label: 'Dashboard', to: '/dashboard' }] : []),
    // Everyone sees Tickets
    { icon: <Ticket size={18} />, label: 'Tickets', to: '/tickets' },
    // OWNER / ADMIN see Knowledge Base
    ...(!isAgent() ? [{ icon: <BookOpen size={18} />, label: 'Knowledge Base', to: '/kb' }] : []),
    // ADMIN / OWNER see Company AI Docs
    ...(isAdmin() ? [{ icon: <BrainCircuit size={18} />, label: 'Company AI Docs', to: '/kb/company' }] : []),
    // Everyone sees Analytics
    { icon: <BarChart2 size={18} />, label: 'Analytics', to: '/analytics' },
    // OWNER / ADMIN see Team management
    ...(isAdmin() ? [{ icon: <Users size={18} />, label: 'Team', to: '/team' }] : []),
    // OWNER / ADMIN see Settings
    ...(isAdmin() ? [{ icon: <Settings size={18} />, label: 'Settings', to: '/settings' }] : []),
  ];

  const bottomItems: NavItem[] = [
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className={styles.sidebar}>
      {/* Logo */}
      <div className={styles.logo}>
        <div className={styles.logoIcon}>
          <svg width="24" height="24" viewBox="0 0 36 36" fill="none">
            <rect width="36" height="36" rx="10" fill="#4F46E5"/>
            <path d="M9 12h18M9 18h12M9 24h15" stroke="white" strokeWidth="2.5" strokeLinecap="round"/>
            <circle cx="27" cy="24" r="4.5" fill="#14B8A6"/>
          </svg>
        </div>
        <span className={styles.logoText}>HelpDeskAI</span>
      </div>

      {/* AI badge */}
      <div className={styles.aiBadge}>
        <Bot size={14} />
        <span>AI-Powered</span>
        <div className={styles.aiDot} />
      </div>

      {/* Nav */}
      <nav className={styles.nav}>
        <ul className={styles.navList}>
          {navItems.map(item => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  `${styles.navItem} ${isActive ? styles.active : ''}`
                }
              >
                <span className={styles.navIcon}>{item.icon}</span>
                <span className={styles.navLabel}>{item.label}</span>
                {item.badge ? (
                  <span className={styles.badge}>{item.badge}</span>
                ) : (
                  <ChevronRight size={14} className={styles.chevron} />
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* Bottom items */}
      <div className={styles.bottom}>
        {bottomItems.length > 0 && (
          <ul className={styles.navList}>
            {bottomItems.map(item => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    `${styles.navItem} ${isActive ? styles.active : ''}`
                  }
                >
                  <span className={styles.navIcon}>{item.icon}</span>
                  <span className={styles.navLabel}>{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        )}

        {/* User profile */}
        <div className={styles.userSection}>
          <div className={styles.avatar}>
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </div>
          <div className={styles.userInfo}>
            <span className={styles.userName}>{user?.firstName} {user?.lastName}</span>
            <span className={styles.userRole}>{user?.role}</span>
          </div>
          <button className={styles.logoutBtn} onClick={handleLogout} title="Logout">
            <LogOut size={16} />
          </button>
        </div>
      </div>
    </aside>
  );
}
