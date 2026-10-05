import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Send, MessageSquare, CheckCircle } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import Button from '@/components/ui/Button';
import styles from './ChatPage.module.css';

interface WsMessage {
  type: string;
  body?: string;
  senderName?: string;
  senderType?: string;
  sentAt?: string;
  agentName?: string;
}

export default function ChatPage() {
  const user = useAuthStore(s => s.user);
  const queryClient = useQueryClient();

  const [selectedSession, setSelectedSession] = useState<any>(null);
  const [messages, setMessages] = useState<WsMessage[]>([]);
  const [reply, setReply] = useState('');
  const [stompClient, setStompClient] = useState<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const { data: sessions = [] } = useQuery({
    queryKey: ['chat-sessions'],
    queryFn: async () => {
      const r = await api.get('/chat/sessions?size=50');
      return r.data.data?.content ?? [];
    },
    refetchInterval: 10000,
  });

  const { data: queue = [] } = useQuery({
    queryKey: ['chat-queue'],
    queryFn: async () => {
      const r = await api.get('/chat/queue');
      return r.data.data ?? [];
    },
    refetchInterval: 5000,
  });

  const assignMutation = useMutation({
    mutationFn: (sessionId: string) => api.post(`/chat/sessions/${sessionId}/assign`),
    onSuccess: (_, sessionId) => {
      queryClient.invalidateQueries({ queryKey: ['chat-sessions'] });
      queryClient.invalidateQueries({ queryKey: ['chat-queue'] });
      const session = queue.find((s: any) => s.id === sessionId);
      if (session) openSession(session);
      toast.success('You are now handling this chat');
    },
  });

  const closeMutation = useMutation({
    mutationFn: (sessionId: string) => api.post(`/chat/sessions/${sessionId}/close`),
    onSuccess: () => {
      setSelectedSession(null);
      disconnectWs();
      queryClient.invalidateQueries({ queryKey: ['chat-sessions'] });
      toast.success('Session closed');
    },
  });

  const openSession = async (session: any) => {
    setSelectedSession(session);
    // Load history
    const r = await api.get(`/chat/sessions/${session.id}/messages`);
    const history: WsMessage[] = (r.data.data ?? []).map((m: any) => ({
      type: 'MESSAGE', body: m.body,
      senderName: m.senderName, senderType: m.senderType, sentAt: m.createdAt,
    }));
    setMessages(history);
    connectWs(session.id);
  };

  const connectWs = (sessionId: string) => {
    disconnectWs();
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws/chat'),
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/chat/${sessionId}`, (frame) => {
          const msg: WsMessage = JSON.parse(frame.body);
          setMessages(prev => [...prev, msg]);
        });
      },
      onDisconnect: () => setConnected(false),
    });
    client.activate();
    setStompClient(client);
  };

  const disconnectWs = () => {
    stompClient?.deactivate();
    setStompClient(null);
    setConnected(false);
  };

  const sendMessage = () => {
    if (!reply.trim() || !stompClient || !selectedSession) return;
    stompClient.publish({
      destination: `/app/chat/${selectedSession.id}/send`,
      body: JSON.stringify({
        body: reply,
        senderName: `${user?.firstName} ${user?.lastName}`,
        senderType: 'AGENT',
        senderId: user?.id,
      }),
    });
    setReply('');
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => () => disconnectWs(), []);

  const statusColor: Record<string, string> = {
    QUEUED: 'var(--warning-500)',
    ACTIVE: 'var(--success-500)',
    CLOSED: 'var(--neutral-400)',
    OPEN:   'var(--primary-500)',
  };

  return (
    <div className={styles.page}>
      {/* Left: session list */}
      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>
          <h2 className={styles.sidebarTitle}>Live Chat</h2>
          {queue.length > 0 && (
            <span className={styles.queueBadge}>{queue.length} in queue</span>
          )}
        </div>

        {/* Queue section */}
        {queue.length > 0 && (
          <div className={styles.queueSection}>
            <p className={styles.queueLabel}>⏳ Queue</p>
            {queue.map((s: any) => (
              <div key={s.id} className={styles.sessionItem}>
                <div className={styles.sessionAvatar}>{(s.visitorName ?? 'V')[0].toUpperCase()}</div>
                <div className={styles.sessionInfo}>
                  <p className={styles.sessionName}>{s.visitorName ?? 'Anonymous'}</p>
                  <p className={styles.sessionMeta}>{s.visitorEmail}</p>
                </div>
                <Button size="sm" variant="primary" onClick={() => assignMutation.mutate(s.id)}>
                  Accept
                </Button>
              </div>
            ))}
          </div>
        )}

        {/* All sessions */}
        <div className={styles.sessionsList}>
          {sessions.map((s: any) => (
            <button
              key={s.id}
              className={`${styles.sessionItem} ${selectedSession?.id === s.id ? styles.sessionActive : ''}`}
              onClick={() => openSession(s)}
            >
              <div className={styles.sessionAvatarWrap}>
                <div className={styles.sessionAvatar}>{(s.visitorName ?? 'V')[0].toUpperCase()}</div>
                <div className={styles.sessionStatusDot} style={{ background: statusColor[s.status] ?? 'var(--neutral-400)' }} />
              </div>
              <div className={styles.sessionInfo}>
                <p className={styles.sessionName}>{s.visitorName ?? 'Anonymous'}</p>
                <p className={styles.sessionMeta}>{s.status} · {s.channel}</p>
              </div>
            </button>
          ))}
          {sessions.length === 0 && (
            <p className={styles.emptyMsg}>No chat sessions yet</p>
          )}
        </div>
      </aside>

      {/* Right: chat window */}
      <main className={styles.chatArea}>
        {!selectedSession ? (
          <div className={styles.noChatSelected}>
            <MessageSquare size={48} color="var(--neutral-300)" />
            <p>Select a chat session to start messaging</p>
          </div>
        ) : (
          <>
            {/* Chat header */}
            <div className={styles.chatHeader}>
              <div className={styles.chatHeaderInfo}>
                <div className={styles.chatAvatar}>{(selectedSession.visitorName ?? 'V')[0].toUpperCase()}</div>
                <div>
                  <p className={styles.chatName}>{selectedSession.visitorName ?? 'Anonymous'}</p>
                  <p className={styles.chatMeta}>
                    {selectedSession.visitorEmail}
                    {connected && <span className={styles.liveIndicator}>● Live</span>}
                  </p>
                </div>
              </div>
              <Button
                size="sm" variant="danger"
                icon={<CheckCircle size={14} />}
                onClick={() => closeMutation.mutate(selectedSession.id)}
                loading={closeMutation.isPending}
              >
                Close Session
              </Button>
            </div>

            {/* Messages */}
            <div className={styles.messages}>
              {messages.filter(m => m.type === 'MESSAGE').map((m, i) => (
                <div
                  key={i}
                  className={`${styles.bubble} ${m.senderType === 'AGENT' ? styles.bubbleAgent : styles.bubbleCustomer}`}
                >
                  <div className={styles.bubbleHeader}>
                    <span className={styles.bubbleSender}>{m.senderName}</span>
                    {m.sentAt && (
                      <span className={styles.bubbleTime}>
                        {new Date(m.sentAt).toLocaleTimeString()}
                      </span>
                    )}
                  </div>
                  <p className={styles.bubbleBody}>{m.body}</p>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className={styles.inputRow}>
              <textarea
                className={styles.replyInput}
                placeholder="Type a message… (Enter to send)"
                value={reply}
                rows={2}
                onChange={e => setReply(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
                }}
              />
              <Button
                icon={<Send size={16} />}
                onClick={sendMessage}
                disabled={!reply.trim() || !connected}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
