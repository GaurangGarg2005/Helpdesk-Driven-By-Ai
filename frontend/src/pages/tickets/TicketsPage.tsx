import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Plus, Search, Filter, RefreshCw } from 'lucide-react';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import { StatusBadge, PriorityBadge } from '@/components/ui/Badge';
import styles from './TicketsPage.module.css';

const STATUSES = ['ALL', 'OPEN', 'IN_PROGRESS', 'WAITING_ON_CUSTOMER', 'RESOLVED', 'CLOSED'];

export default function TicketsPage() {
  const [status, setStatus] = useState('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['tickets', status, page],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), size: '25' });
      if (status !== 'ALL') params.set('status', status);
      const res = await api.get(`/tickets?${params}`);
      return res.data.data;
    },
    staleTime: 0,           // always consider data stale
    refetchOnMount: true,   // always refetch when component mounts
    retry: 1,
  });

  const tickets: any[] = data?.content ?? [];
  const totalPages: number = data?.totalPages ?? 0;
  const totalElements: number = data?.totalElements ?? 0;

  const filtered = search
    ? tickets.filter(t =>
        t.subject?.toLowerCase().includes(search.toLowerCase()) ||
        String(t.ticketNumber).includes(search)
      )
    : tickets;

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>All Tickets</h1>
          <p className={styles.subtitle}>{totalElements} tickets total</p>
        </div>
        <div className={styles.actions}>
          <Button
            variant="secondary"
            size="sm"
            icon={<RefreshCw size={14} />}
            onClick={() => refetch()}
          >
            Refresh
          </Button>
          <Link to="/tickets/new">
            <Button size="sm" icon={<Plus size={14} />}>New Ticket</Button>
          </Link>
        </div>
      </div>

      {/* Filters row */}
      <div className={styles.filtersRow}>
        {/* Status tabs */}
        <div className={styles.statusTabs}>
          {STATUSES.map(s => (
            <button
              key={s}
              className={`${styles.tab} ${status === s ? styles.tabActive : ''}`}
              onClick={() => { setStatus(s); setPage(0); }}
            >
              {s === 'ALL' ? 'All' : s.replace('_', ' ')}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className={styles.searchWrapper}>
          <Search size={14} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="Search tickets…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Table */}
      <div className={styles.tableWrapper}>
        {isLoading ? (
          <div className={styles.loadingRows}>
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className={`${styles.skeleton} skeleton`} style={{ height: 52 }} />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className={styles.empty}>
            <Ticket size={40} color="var(--neutral-300)" />
            <p>No tickets found</p>
            <Link to="/tickets/new"><Button size="sm">Create your first ticket</Button></Link>
          </div>
        ) : (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>#</th>
                <th>Subject</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Category</th>
                <th>Requester</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((ticket: any) => (
                <tr key={ticket.id} className={styles.row}>
                  <td>
                    <Link to={`/tickets/${ticket.id}`} className={styles.ticketNum}>
                      #{ticket.ticketNumber}
                    </Link>
                  </td>
                  <td>
                    <Link to={`/tickets/${ticket.id}`} className={styles.subject}>
                      {ticket.subject}
                    </Link>
                  </td>
                  <td><StatusBadge status={ticket.status} /></td>
                  <td><PriorityBadge priority={ticket.priority} /></td>
                  <td>
                    <span className={styles.category}>{ticket.category ?? '—'}</span>
                  </td>
                  <td>
                    <span className={styles.requester}>
                      {ticket.requesterEmail ?? ticket.requesterName ?? '—'}
                    </span>
                  </td>
                  <td>
                    <span className={styles.date}>
                      {ticket.createdAt ? new Date(ticket.createdAt).toLocaleDateString() : '—'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <Button
            variant="secondary" size="sm"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >← Prev</Button>
          <span className={styles.pageInfo}>Page {page + 1} of {totalPages}</span>
          <Button
            variant="secondary" size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >Next →</Button>
        </div>
      )}
    </div>
  );
}

function Ticket({ size, color }: { size: number; color: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.5">
      <path d="M20 12V22H4V12" /><path d="M22 7H2v5h20V7z" />
      <path d="M12 22V7" /><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z" />
      <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z" />
    </svg>
  );
}
