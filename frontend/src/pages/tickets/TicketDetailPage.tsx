import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bot, Send, Clock, User, ChevronDown } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { StatusBadge, PriorityBadge, Badge } from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import styles from './TicketDetailPage.module.css';

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore(s => s.user);

  const [reply, setReply] = useState('');
  const [isInternal, setIsInternal] = useState(false);

  const { data: ticketData, isLoading } = useQuery({
    queryKey: ['ticket', id],
    queryFn: async () => {
      const res = await api.get(`/tickets/${id}`);
      return res.data.data;
    },
  });

  const { data: aiSummary, refetch: fetchSummary, isFetching: summaryLoading } = useQuery({
    queryKey: ['ticket-ai-summary', id],
    queryFn: async () => {
      const res = await api.get(`/tickets/${id}/ai/summary`);
      return res.data.data;
    },
    enabled: false,
  });

  const { data: aiSuggestion, refetch: fetchSuggestion, isFetching: suggestionLoading } = useQuery({
    queryKey: ['ticket-ai-suggestion', id],
    queryFn: async () => {
      const res = await api.get(`/tickets/${id}/ai/suggestions`);
      return res.data.data;
    },
    enabled: false,
  });

  const replyMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post(`/tickets/${id}/messages`, {
        body: reply,
        internalNote: isInternal,
      });
      return res.data.data;
    },
    onSuccess: () => {
      setReply('');
      queryClient.invalidateQueries({ queryKey: ['ticket', id] });
      toast.success(isInternal ? 'Internal note added' : 'Reply sent');
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: async (status: string) => {
      const res = await api.put(`/tickets/${id}`, { status });
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ticket', id] });
      toast.success('Status updated');
    },
  });

  if (isLoading) {
    return (
      <div className={styles.page}>
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="skeleton" style={{ height: 80, borderRadius: 12 }} />
        ))}
      </div>
    );
  }

  if (!ticketData) return <div className={styles.page}><p>Ticket not found.</p></div>;

  const t = ticketData;
  const messages: any[] = t.messages ?? [];
  const senderLabel = (type: string) =>
    ({ AGENT: 'Agent', CUSTOMER: 'Customer', SYSTEM: 'System', AI: 'AI' })[type] ?? type;

  return (
    <div className={styles.page}>
      {/* ── Header ── */}
      <div className={styles.header}>
        <button className={styles.back} onClick={() => navigate('/tickets')}>← Back</button>
        <div className={styles.headerMain}>
          <h1 className={styles.subject}>
            <span className={styles.ticketNum}>#{t.ticketNumber}</span> {t.subject}
          </h1>
          <div className={styles.meta}>
            <StatusBadge status={t.status} />
            <PriorityBadge priority={t.priority} />
            {t.category && <Badge variant="default">{t.category}</Badge>}
            {t.sentiment && (
              <Badge variant={t.sentiment === 'NEGATIVE' ? 'danger' : t.sentiment === 'POSITIVE' ? 'success' : 'default'}>
                {t.sentiment} sentiment
              </Badge>
            )}
          </div>
        </div>

        {/* Quick status change */}
        <div className={styles.headerActions}>
          {['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map(s => (
            <button
              key={s}
              className={`${styles.statusBtn} ${t.status === s ? styles.statusBtnActive : ''}`}
              onClick={() => updateStatusMutation.mutate(s)}
            >
              {s.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* ── Body: conversation + sidebar ── */}
      <div className={styles.body}>
        {/* Left: conversation */}
        <div className={styles.conversation}>
          {/* AI Summary panel */}
          <div className={styles.aiPanel}>
            <div className={styles.aiPanelHeader}>
              <Bot size={15} />
              <span>AI Assistant</span>
              <div className={styles.aiActions}>
                <Button
                  variant="ai" size="sm"
                  loading={summaryLoading}
                  onClick={() => fetchSummary()}
                >
                  Summarize
                </Button>
                <Button
                  variant="ghost" size="sm"
                  loading={suggestionLoading}
                  onClick={() => fetchSuggestion()}
                >
                  Suggest Reply
                </Button>
              </div>
            </div>
            {aiSummary?.summary && (
              <div className={styles.aiContent}>
                <p className={styles.aiLabel}>Summary</p>
                <p>{aiSummary.summary}</p>
              </div>
            )}
            {aiSuggestion?.suggested_reply && (
              <div className={styles.aiContent}>
                <p className={styles.aiLabel}>Suggested Reply</p>
                <p>{aiSuggestion.suggested_reply}</p>
                <Button
                  size="sm" variant="secondary"
                  style={{ marginTop: 8 }}
                  onClick={() => setReply(aiSuggestion.suggested_reply)}
                >
                  Use this reply
                </Button>
              </div>
            )}
          </div>

          {/* Messages */}
          <div className={styles.messages}>
            {/* Original description */}
            <div className={`${styles.message} ${styles.messageCustomer}`}>
              <div className={styles.msgHeader}>
                <User size={14} />
                <span>{t.requesterName ?? t.requesterEmail ?? 'Customer'}</span>
                <span className={styles.msgTime}>
                  {t.createdAt ? new Date(t.createdAt).toLocaleString() : ''}
                </span>
              </div>
              <div className={styles.msgBody}>{t.description}</div>
            </div>

            {messages.map((msg: any) => (
              <div
                key={msg.id}
                className={`${styles.message} ${
                  msg.senderType === 'AGENT' ? styles.messageAgent :
                  msg.senderType === 'AI'    ? styles.messageAi :
                  msg.isInternalNote         ? styles.messageInternal :
                  styles.messageCustomer
                }`}
              >
                <div className={styles.msgHeader}>
                  <span className={styles.msgSender}>{senderLabel(msg.senderType)}</span>
                  {msg.isInternalNote && <Badge variant="warning" size="sm">Internal</Badge>}
                  <span className={styles.msgTime}>
                    {msg.createdAt ? new Date(msg.createdAt).toLocaleString() : ''}
                  </span>
                </div>
                <div className={styles.msgBody}>{msg.body}</div>
              </div>
            ))}
          </div>

          {/* Reply box */}
          <div className={styles.replyBox}>
            <div className={styles.replyTabs}>
              <button
                className={`${styles.replyTab} ${!isInternal ? styles.replyTabActive : ''}`}
                onClick={() => setIsInternal(false)}
              >Reply to customer</button>
              <button
                className={`${styles.replyTab} ${isInternal ? styles.replyTabActive : ''}`}
                onClick={() => setIsInternal(true)}
              >Internal note</button>
            </div>
            <textarea
              className={`${styles.replyTextarea} ${isInternal ? styles.replyInternal : ''}`}
              placeholder={isInternal ? 'Add a note visible only to agents…' : 'Type your reply…'}
              value={reply}
              onChange={e => setReply(e.target.value)}
              rows={4}
            />
            <div className={styles.replyFooter}>
              <Button
                icon={<Send size={14} />}
                loading={replyMutation.isPending}
                onClick={() => reply.trim() && replyMutation.mutate()}
                disabled={!reply.trim()}
              >
                {isInternal ? 'Add Note' : 'Send Reply'}
              </Button>
            </div>
          </div>
        </div>

        {/* Right: details sidebar */}
        <aside className={styles.sidebar}>
          <div className={styles.sideSection}>
            <h3 className={styles.sideSectionTitle}>Ticket Details</h3>
            <div className={styles.detailRows}>
              <DetailRow label="Status"    value={<StatusBadge status={t.status} />} />
              <DetailRow label="Priority"  value={<PriorityBadge priority={t.priority} />} />
              <DetailRow label="Category"  value={t.category ?? '—'} />
              <DetailRow label="Channel"   value={t.channel} />
              <DetailRow label="Created"   value={t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '—'} />
              {t.firstResponseDueAt && (
                <DetailRow
                  label="First Response Due"
                  value={new Date(t.firstResponseDueAt).toLocaleString()}
                />
              )}
              {t.resolutionDueAt && (
                <DetailRow
                  label="Resolution Due"
                  value={new Date(t.resolutionDueAt).toLocaleString()}
                />
              )}
              {t.slaFirstResponseBreached && (
                <DetailRow label="SLA" value={<Badge variant="danger">Breached</Badge>} />
              )}
            </div>
          </div>

          <div className={styles.sideSection}>
            <h3 className={styles.sideSectionTitle}>Requester</h3>
            <div className={styles.detailRows}>
              <DetailRow label="Name"  value={t.requesterName ?? '—'} />
              <DetailRow label="Email" value={t.requesterEmail ?? '—'} />
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className={styles.detailRow}>
      <span className={styles.detailLabel}>{label}</span>
      <span className={styles.detailValue}>{value}</span>
    </div>
  );
}
