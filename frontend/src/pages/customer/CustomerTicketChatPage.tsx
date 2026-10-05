import React, { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Send, Bot, User, UserCheck, CheckCircle2, AlertTriangle, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

interface Message {
  id: string;
  senderType: string;
  senderName: string;
  body: string;
  createdAt: string;
}

interface TicketDetail {
  id: string;
  ticketNumber: number;
  subject: string;
  status: string;
  aiHandlingActive: boolean;
  humanEscalationCount: number;
  messages: Message[];
}

function TypingIndicator() {
  return (
    <div style={s.typingBubble}>
      <span style={{ ...s.dot, animationDelay: '0ms' }} />
      <span style={{ ...s.dot, animationDelay: '160ms' }} />
      <span style={{ ...s.dot, animationDelay: '320ms' }} />
    </div>
  );
}

function StatusBanner({ status }: { status: string }) {
  if (status === 'NEEDS_AGENT_REVIEW')
    return (
      <div style={{ ...s.banner, borderColor: '#fbbf24', background: 'rgba(251,191,36,0.08)' }}>
        <AlertTriangle size={14} color="#fbbf24" />
        <span style={{ color: '#fbbf24' }}>A support agent has been notified and will join shortly.</span>
      </div>
    );
  if (status === 'CLOSED' || status === 'RESOLVED')
    return (
      <div style={{ ...s.banner, borderColor: '#4ade80', background: 'rgba(74,222,128,0.08)' }}>
        <CheckCircle2 size={14} color="#4ade80" />
        <span style={{ color: '#4ade80' }}>This ticket is closed. Open a new ticket if you need more help.</span>
      </div>
    );
  return null;
}

function formatTime(dateStr: string) {
  return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export default function CustomerTicketChatPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore(s => s.user);
  const [input, setInput] = useState('');
  const [aiTyping, setAiTyping] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const { data: ticket, isLoading } = useQuery({
    queryKey: ['my-ticket', id],
    queryFn: async () => {
      const res = await api.get(`/tickets/${id}`);
      return res.data.data as TicketDetail;
    },
    refetchInterval: 5000, // poll every 5s for new AI messages
  });

  // Scroll to bottom when new messages arrive
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [ticket?.messages?.length]);

  const sendMutation = useMutation({
    mutationFn: async (body: string) => {
      setAiTyping(true);
      const res = await api.post(`/tickets/my/${id}/messages`, { body });
      return res.data.data;
    },
    onSuccess: () => {
      setInput('');
      // Invalidate immediately then again after 3s (AI reply comes async)
      queryClient.invalidateQueries({ queryKey: ['my-ticket', id] });
      setTimeout(() => {
        setAiTyping(false);
        queryClient.invalidateQueries({ queryKey: ['my-ticket', id] });
      }, 3500);
    },
    onError: () => {
      setAiTyping(false);
      toast.error('Failed to send message');
    },
  });

  const closeSelfMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/tickets/my/${id}/messages`, {
        body: 'Thank you, my issue has been resolved.',
      });
    },
    onSuccess: () => {
      toast.success('Ticket closed. Thank you!');
      queryClient.invalidateQueries({ queryKey: ['my-ticket', id] });
      queryClient.invalidateQueries({ queryKey: ['my-tickets'] });
    },
  });

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    if (ticket?.status === 'CLOSED' || ticket?.status === 'RESOLVED') {
      toast.error('This ticket is closed. Please open a new one.');
      return;
    }
    sendMutation.mutate(trimmed);
  };

  const isClosed = ticket?.status === 'CLOSED' || ticket?.status === 'RESOLVED';
  const messages = ticket?.messages ?? [];

  return (
    <div style={s.page}>
      {/* Keyframe injection */}
      <style>{`
        @keyframes bounce {
          0%, 80%, 100% { transform: scale(0); opacity: 0.3; }
          40% { transform: scale(1); opacity: 1; }
        }
      `}</style>

      {/* Header */}
      <header style={s.header}>
        <button style={s.backBtn} onClick={() => navigate('/customer/portal')}>
          <ArrowLeft size={16} />
        </button>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={s.ticketNum}>#{ticket?.ticketNumber}</div>
          <div style={s.ticketSubject}>{ticket?.subject ?? '…'}</div>
        </div>
        <StatusTag status={ticket?.status ?? ''} />
      </header>

      {/* Status banner */}
      {ticket && <StatusBanner status={ticket.status} />}

      {/* Messages */}
      <div style={s.messages}>
        {isLoading && <div style={s.loadingText}>Loading conversation…</div>}

        {messages.map(msg => {
          const isCustomer = msg.senderType === 'CUSTOMER';
          const isAI       = msg.senderType === 'AI';
          const isAgent    = msg.senderType === 'AGENT' || msg.senderType === 'ADMIN';

          return (
            <div key={msg.id} style={{ ...s.msgRow, justifyContent: isCustomer ? 'flex-end' : 'flex-start' }}>
              {/* Avatar (left side only) */}
              {!isCustomer && (
                <div style={{ ...s.avatar, background: isAI ? 'linear-gradient(135deg, #6366f1, #8b5cf6)' : '#0ea5e9' }}>
                  {isAI ? <Bot size={14} color="#fff" /> : <UserCheck size={14} color="#fff" />}
                </div>
              )}

              <div style={{ maxWidth: '72%' }}>
                <div style={{ ...s.senderLabel, textAlign: isCustomer ? 'right' : 'left' }}>
                  {isAI ? 'AI Assistant' : isAgent ? msg.senderName : 'You'}
                  {isAgent && <span style={s.agentTag}>Agent</span>}
                </div>
                <div style={{
                  ...s.bubble,
                  background: isCustomer
                    ? 'linear-gradient(135deg, #4f46e5, #7c3aed)'
                    : isAI
                      ? 'rgba(255,255,255,0.06)'
                      : 'rgba(14,165,233,0.12)',
                  border: isCustomer ? 'none' : '1px solid rgba(255,255,255,0.08)',
                  borderRadius: isCustomer ? '18px 6px 18px 18px' : '6px 18px 18px 18px',
                }}>
                  {msg.body}
                </div>
                <div style={{ ...s.msgTime, textAlign: isCustomer ? 'right' : 'left' }}>
                  {formatTime(msg.createdAt)}
                </div>
              </div>

              {/* Customer avatar (right side) */}
              {isCustomer && (
                <div style={{ ...s.avatar, background: '#4f46e5' }}>
                  <User size={14} color="#fff" />
                </div>
              )}
            </div>
          );
        })}

        {/* AI typing indicator */}
        {aiTyping && (
          <div style={{ ...s.msgRow, justifyContent: 'flex-start' }}>
            <div style={{ ...s.avatar, background: 'linear-gradient(135deg, #6366f1, #8b5cf6)' }}>
              <Bot size={14} color="#fff" />
            </div>
            <TypingIndicator />
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input area */}
      <div style={s.inputArea}>
        {!isClosed && (
          <button
            style={s.resolveBtn}
            onClick={() => closeSelfMutation.mutate()}
            disabled={closeSelfMutation.isPending}
            title="Mark as resolved"
          >
            <CheckCircle2 size={15} />
            Issue resolved
          </button>
        )}

        <div style={s.inputRow}>
          <textarea
            style={s.input}
            placeholder={isClosed ? 'This ticket is closed' : 'Type your message…'}
            value={input}
            disabled={isClosed || sendMutation.isPending}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            rows={1}
          />
          <button
            style={{
              ...s.sendBtn,
              opacity: (isClosed || !input.trim() || sendMutation.isPending) ? 0.4 : 1,
            }}
            disabled={isClosed || !input.trim() || sendMutation.isPending}
            onClick={handleSend}
          >
            <Send size={16} />
          </button>
        </div>
        <div style={s.inputHint}>Press Enter to send · Shift+Enter for new line</div>
      </div>
    </div>
  );
}

function StatusTag({ status }: { status: string }) {
  const cfg: Record<string, { label: string; color: string }> = {
    OPEN:                { label: 'Open',           color: '#60a5fa' },
    IN_PROGRESS:         { label: 'In Progress',    color: '#a78bfa' },
    NEEDS_AGENT_REVIEW:  { label: 'Agent Review',   color: '#fbbf24' },
    WAITING_ON_CUSTOMER: { label: 'Awaiting You',   color: '#fb923c' },
    RESOLVED:            { label: 'Resolved',       color: '#4ade80' },
    CLOSED:              { label: 'Closed',         color: '#64748b' },
  };
  const c = cfg[status] ?? { label: status, color: '#94a3b8' };
  return (
    <span style={{
      fontSize: '11px', fontWeight: 600, color: c.color,
      background: `${c.color}1a`, padding: '4px 10px',
      borderRadius: '999px', whiteSpace: 'nowrap',
    }}>
      {c.label}
    </span>
  );
}

const s: Record<string, React.CSSProperties> = {
  page: {
    height: '100vh', display: 'flex', flexDirection: 'column',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
    fontFamily: "'Inter', system-ui, sans-serif", color: '#f1f5f9',
  },
  header: {
    display: 'flex', alignItems: 'center', gap: '12px',
    padding: '14px 20px',
    background: 'rgba(255,255,255,0.04)',
    borderBottom: '1px solid rgba(255,255,255,0.07)',
    flexShrink: 0,
  },
  backBtn: {
    padding: '7px', background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '8px', color: '#94a3b8', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },
  ticketNum: { fontSize: '11px', color: '#475569', fontFamily: 'monospace' },
  ticketSubject: { fontSize: '15px', fontWeight: 600, color: '#f1f5f9', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' },
  banner: {
    display: 'flex', alignItems: 'center', gap: '8px',
    padding: '10px 20px',
    border: '1px solid transparent',
    borderLeft: 'none', borderRight: 'none',
    fontSize: '13px', flexShrink: 0,
  },
  messages: {
    flex: 1, overflowY: 'auto', padding: '20px',
    display: 'flex', flexDirection: 'column', gap: '16px',
  },
  loadingText: { textAlign: 'center', color: '#64748b', fontSize: '14px', padding: '40px 0' },
  msgRow: { display: 'flex', alignItems: 'flex-end', gap: '8px' },
  avatar: {
    width: '30px', height: '30px', borderRadius: '50%', flexShrink: 0,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  senderLabel: { fontSize: '11px', color: '#64748b', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' },
  agentTag: {
    fontSize: '10px', padding: '1px 6px',
    background: 'rgba(14,165,233,0.15)', color: '#38bdf8',
    borderRadius: '999px', marginLeft: '4px',
  },
  bubble: {
    padding: '10px 14px', fontSize: '14px', lineHeight: '1.6',
    color: '#e2e8f0', whiteSpace: 'pre-wrap', wordBreak: 'break-word',
  },
  msgTime: { fontSize: '10px', color: '#334155', marginTop: '4px' },
  typingBubble: {
    display: 'flex', alignItems: 'center', gap: '4px',
    padding: '12px 16px',
    background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: '6px 18px 18px 18px',
  },
  dot: {
    display: 'inline-block', width: '7px', height: '7px',
    borderRadius: '50%', background: '#6366f1',
    animation: 'bounce 1.2s ease-in-out infinite',
  },
  inputArea: {
    padding: '12px 20px 16px',
    borderTop: '1px solid rgba(255,255,255,0.07)',
    background: 'rgba(255,255,255,0.02)',
    flexShrink: 0,
  },
  resolveBtn: {
    display: 'flex', alignItems: 'center', gap: '6px',
    marginBottom: '10px', padding: '6px 14px',
    background: 'rgba(74,222,128,0.08)',
    border: '1px solid rgba(74,222,128,0.2)',
    borderRadius: '8px', color: '#4ade80',
    fontSize: '12px', cursor: 'pointer',
  },
  inputRow: { display: 'flex', gap: '10px', alignItems: 'flex-end' },
  input: {
    flex: 1, padding: '10px 14px', resize: 'none',
    background: 'rgba(255,255,255,0.07)',
    border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '10px', color: '#f1f5f9',
    fontSize: '14px', fontFamily: 'inherit',
    outline: 'none', lineHeight: '1.5',
  },
  sendBtn: {
    width: '40px', height: '40px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    border: 'none', borderRadius: '10px',
    color: '#fff', cursor: 'pointer',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    flexShrink: 0, transition: 'opacity 0.15s',
  },
  inputHint: { fontSize: '11px', color: '#334155', marginTop: '6px' },
};
