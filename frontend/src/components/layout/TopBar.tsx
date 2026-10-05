import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Bell, Search, Plus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';
import styles from './TopBar.module.css';

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/tickets':   'Tickets',
  '/chat':      'Live Chat',
  '/kb':        'Knowledge Base',
  '/analytics': 'Analytics',
  '/team':      'Team',
  '/settings':  'Settings',
};

export default function TopBar() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore(s => s.user);

  const title = Object.entries(PAGE_TITLES).find(([path]) =>
    pathname.startsWith(path)
  )?.[1] ?? 'HelpDeskAI';

  const { data: unread = 0 } = useQuery({
    queryKey: ['notif-unread'],
    queryFn: async () => {
      const r = await api.get('/notifications/unread-count', { silent: true } as any);
      return r.data.data?.count ?? 0;
    },
    refetchInterval: 30000,
    retry: false,
  });


  const markAll = useMutation({
    mutationFn: () => api.post('/notifications/read-all'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notif-unread'] }),
  });

  return (
    <header className={styles.topbar}>
      <div className={styles.left}>
        <h1 className={styles.title}>{title}</h1>
      </div>

      <div className={styles.center}>
        <div className={styles.searchWrapper}>
          <Search size={15} className={styles.searchIcon} />
          <input
            type="text"
            placeholder="Search tickets, contacts, articles…"
            className={styles.searchInput}
          />
          <kbd className={styles.searchKbd}>⌘K</kbd>
        </div>
      </div>

      <div className={styles.right}>
        <button
          className={styles.newTicketBtn}
          onClick={() => navigate('/tickets/new')}
        >
          <Plus size={15} />
          <span>New Ticket</span>
        </button>

        <button
          className={styles.iconBtn}
          aria-label="Notifications"
          onClick={() => markAll.mutate()}
          title={`${unread} unread notification${unread !== 1 ? 's' : ''}`}
        >
          <Bell size={18} />
          {unread > 0 && (
            <span className={styles.notifBadge}>{unread > 99 ? '99+' : unread}</span>
          )}
        </button>

        <div className={styles.avatar} title={`${user?.firstName} ${user?.lastName}`}>
          {user?.firstName?.[0]}{user?.lastName?.[0]}
        </div>
      </div>
    </header>
  );
}
