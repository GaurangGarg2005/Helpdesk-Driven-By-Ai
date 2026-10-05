import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Upload, Trash2, RefreshCw, FileText, CheckCircle2,
  Clock, Plus, X, BookOpen, Sparkles, Eye, Edit3,
  Save, AlertCircle, Hash, AlignLeft, ChevronRight,
  Loader2
} from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';

// ── Types ──────────────────────────────────────────────────────────────────

interface CompanyDoc {
  id: string;
  title: string;
  aiHeading: string;
  description: string;
  fileType: string;
  sourceUrl: string;
  indexedInRag: boolean;
  contentPreview: string;
  fullContent?: string;
  wordCount: number;
  createdAt: string;
  updatedAt: string;
}

const FILE_TYPES = ['TXT', 'MARKDOWN', 'PDF', 'URL'];

const TYPE_COLORS: Record<string, string> = {
  TXT: '#6366f1',
  MARKDOWN: '#10b981',
  PDF: '#f59e0b',
  URL: '#3b82f6',
};

// ── Component ─────────────────────────────────────────────────────────────

export default function CompanyKnowledgePage() {
  const queryClient = useQueryClient();

  // Upload form state
  const [showUploadForm, setShowUploadForm] = useState(false);
  const [form, setForm] = useState({ title: '', content: '', fileType: 'TXT', description: '', sourceUrl: '' });

  // Popup viewer/editor state
  const [popupDoc, setPopupDoc] = useState<CompanyDoc | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const popupRef = useRef<HTMLDivElement>(null);

  // ── Queries ──

  const { data: pages, isLoading } = useQuery({
    queryKey: ['company-docs'],
    queryFn: async () => {
      const res = await api.get('/kb/company-docs?size=100');
      return res.data.data.content as CompanyDoc[];
    },
  });
  const docs = pages ?? [];

  // Fetch full content when popup opens
  const { data: fullDocData, isLoading: loadingFull } = useQuery({
    queryKey: ['company-doc', popupDoc?.id],
    queryFn: async () => {
      if (!popupDoc) return null;
      const res = await api.get(`/kb/company-docs/${popupDoc.id}`);
      return res.data.data as CompanyDoc;
    },
    enabled: !!popupDoc,
  });

  // Sync edit fields when full doc loads
  useEffect(() => {
    if (fullDocData) {
      setEditContent(fullDocData.fullContent ?? '');
      setEditTitle(fullDocData.title ?? '');
      setEditDesc(fullDocData.description ?? '');
    }
  }, [fullDocData]);

  // Close popup on Esc
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closePopup();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  // ── Mutations ──

  const uploadMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/kb/company-docs', form);
      return res.data.data;
    },
    onSuccess: () => {
      toast.success('Document uploaded and indexed in AI!');
      setShowUploadForm(false);
      setForm({ title: '', content: '', fileType: 'TXT', description: '', sourceUrl: '' });
      queryClient.invalidateQueries({ queryKey: ['company-docs'] });
    },
    onError: (err: any) => toast.error(err?.response?.data?.message || 'Upload failed'),
  });

  const updateMutation = useMutation({
    mutationFn: async (id: string) => {
      const res = await api.put(`/kb/company-docs/${id}`, {
        title: editTitle,
        content: editContent,
        description: editDesc,
      });
      return res.data.data as CompanyDoc;
    },
    onSuccess: (updated) => {
      toast.success('Document updated and re-indexed!');
      setEditMode(false);
      queryClient.invalidateQueries({ queryKey: ['company-docs'] });
      queryClient.invalidateQueries({ queryKey: ['company-doc', updated.id] });
      setPopupDoc(prev => prev ? { ...prev, ...updated } : null);
    },
    onError: (err: any) => toast.error(err?.response?.data?.message || 'Update failed'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/kb/company-docs/${id}`),
    onSuccess: () => {
      toast.success('Document deleted');
      closePopup();
      queryClient.invalidateQueries({ queryKey: ['company-docs'] });
    },
  });

  const reindexMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/kb/company-docs/${id}/reindex`),
    onSuccess: () => {
      toast.success('Document re-indexed in AI');
      queryClient.invalidateQueries({ queryKey: ['company-docs'] });
      queryClient.invalidateQueries({ queryKey: ['company-doc', popupDoc?.id] });
    },
  });

  // ── Handlers ──

  const closePopup = () => {
    setPopupDoc(null);
    setEditMode(false);
    setEditContent('');
    setEditTitle('');
    setEditDesc('');
  };

  const openPopup = (doc: CompanyDoc) => {
    setPopupDoc(doc);
    setEditMode(false);
  };

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) { toast.error('Title is required'); return; }
    if (!form.content.trim()) { toast.error('Content is required'); return; }
    uploadMutation.mutate();
  };

  // ── Render ──

  return (
    <div style={s.page}>

      {/* ── Header ─────────────────────────────────────────── */}
      <div style={s.pageHeader}>
        <div style={s.headerLeft}>
          <div style={s.headerIcon}><BookOpen size={20} color="#fff" /></div>
          <div>
            <h1 style={s.pageTitle}>Company Knowledge Base</h1>
            <p style={s.pageSubtitle}>
              {docs.length} document{docs.length !== 1 ? 's' : ''} — AI uses these to answer customer tickets
            </p>
          </div>
        </div>
        <button style={s.uploadBtn} onClick={() => setShowUploadForm(true)}>
          <Plus size={14} /> Add Document
        </button>
      </div>

      {/* ── How it works banner ────────────────────────────── */}
      <div style={s.aiCard}>
        <Sparkles size={16} style={{ flexShrink: 0, color: '#a5b4fc' }} />
        <div>
          <div style={s.aiCardTitle}>How RAG works</div>
          <div style={s.aiCardBody}>
            When a customer submits a ticket, the AI searches these documents and uses the
            matching passage to compose a direct answer grounded in your actual company policy —
            not a generic template. The more detailed your docs, the better the answers.
          </div>
        </div>
      </div>

      {/* ── Upload form ────────────────────────────────────── */}
      {showUploadForm && (
        <div style={s.formCard}>
          <div style={s.formHeader}>
            <h2 style={s.formTitle}>New Knowledge Document</h2>
            <button style={s.closeBtn} onClick={() => setShowUploadForm(false)}><X size={16} /></button>
          </div>
          <form onSubmit={handleUpload} style={s.form}>
            <div style={s.formRow}>
              <div style={{ flex: 2 }}>
                <label style={s.label}>Title *</label>
                <input
                  style={s.input}
                  placeholder="e.g. Refund Policy, Shipping FAQ, Getting Started"
                  value={form.title}
                  onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={s.label}>Type</label>
                <select
                  style={s.select}
                  value={form.fileType}
                  onChange={e => setForm(f => ({ ...f, fileType: e.target.value }))}
                >
                  {FILE_TYPES.map(t => <option key={t}>{t}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label style={s.label}>Short description (optional)</label>
              <input
                style={s.input}
                placeholder="What is this document about?"
                value={form.description}
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              />
            </div>
            <div>
              <label style={s.label}>Content * — paste text, markdown, or extracted PDF text</label>
              <textarea
                style={s.textarea}
                rows={10}
                placeholder="Paste your company document content here…"
                value={form.content}
                onChange={e => setForm(f => ({ ...f, content: e.target.value }))}
              />
              <div style={s.charHint}>
                {form.content.split(/\s+/).filter(Boolean).length.toLocaleString()} words
                &nbsp;·&nbsp;
                {form.content.length.toLocaleString()} chars
              </div>
            </div>
            <div style={s.formActions}>
              <button type="button" style={s.cancelBtn} onClick={() => setShowUploadForm(false)}>Cancel</button>
              <button
                type="submit"
                style={{ ...s.submitBtn, opacity: uploadMutation.isPending ? 0.7 : 1 }}
                disabled={uploadMutation.isPending}
              >
                {uploadMutation.isPending
                  ? <><Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} /> Uploading & Indexing…</>
                  : <><Upload size={14} /> Upload & Index</>}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── Doc cards grid ─────────────────────────────────── */}
      {isLoading && (
        <div style={s.emptyText}>
          <Loader2 size={24} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 8px', display: 'block' }} />
          Loading documents…
        </div>
      )}

      {!isLoading && docs.length === 0 && (
        <div style={s.emptyState}>
          <FileText size={44} style={{ opacity: 0.25, marginBottom: 14 }} />
          <p style={{ margin: '0 0 6px', color: '#e2e8f0', fontWeight: 600 }}>No documents yet</p>
          <p style={{ margin: 0, color: '#64748b', fontSize: 13 }}>
            Upload your first knowledge document to start training the AI.
          </p>
        </div>
      )}

      <div style={s.docGrid}>
        {docs.map(doc => (
          <DocCard
            key={doc.id}
            doc={doc}
            onOpen={() => openPopup(doc)}
            onDelete={() => {
              if (window.confirm(`Delete "${doc.title}"? This cannot be undone.`)) {
                deleteMutation.mutate(doc.id);
              }
            }}
            onReindex={() => reindexMutation.mutate(doc.id)}
            isReindexing={reindexMutation.isPending}
          />
        ))}
      </div>

      {/* ── Full content popup ─────────────────────────────── */}
      {popupDoc && (
        <div style={s.overlay} onClick={(e) => { if (e.target === e.currentTarget) closePopup(); }}>
          <div style={s.popup} ref={popupRef}>

            {/* Popup header */}
            <div style={s.popupHeader}>
              <div style={s.popupHeaderLeft}>
                <div style={{ ...s.popupTypeTag, background: TYPE_COLORS[popupDoc.fileType] || '#6366f1' }}>
                  {popupDoc.fileType}
                </div>
                <div>
                  {editMode ? (
                    <input
                      style={{ ...s.input, fontSize: 16, fontWeight: 700, padding: '6px 10px' }}
                      value={editTitle}
                      onChange={e => setEditTitle(e.target.value)}
                      placeholder="Document title"
                    />
                  ) : (
                    <h2 style={s.popupTitle}>{fullDocData?.title ?? popupDoc.title}</h2>
                  )}
                  <div style={s.popupMeta}>
                    <span style={fullDocData?.indexedInRag ? s.indexedTag : s.pendingTag}>
                      {fullDocData?.indexedInRag ? <><CheckCircle2 size={10} /> Indexed in AI</> : <><Clock size={10} /> Pending</>}
                    </span>
                    <span style={s.metaItem}><Hash size={10} /> {fullDocData?.wordCount ?? popupDoc.wordCount} words</span>
                    <span style={s.metaItem}>Updated {new Date(fullDocData?.updatedAt ?? popupDoc.updatedAt).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
              <div style={s.popupActions}>
                {editMode ? (
                  <>
                    <button
                      style={s.popupSaveBtn}
                      onClick={() => updateMutation.mutate(popupDoc.id)}
                      disabled={updateMutation.isPending}
                    >
                      {updateMutation.isPending ? <Loader2 size={13} style={{ animation: 'spin 1s linear infinite' }} /> : <Save size={13} />}
                      Save
                    </button>
                    <button style={s.popupIconBtn} onClick={() => setEditMode(false)} title="Cancel edit">
                      <X size={14} />
                    </button>
                  </>
                ) : (
                  <>
                    <button style={s.popupEditBtn} onClick={() => setEditMode(true)} title="Edit document">
                      <Edit3 size={13} /> Edit
                    </button>
                    <button
                      style={{ ...s.popupIconBtn, color: '#f87171' }}
                      onClick={() => {
                        if (window.confirm(`Delete "${popupDoc.title}"?`)) deleteMutation.mutate(popupDoc.id);
                      }}
                      title="Delete document"
                    >
                      <Trash2 size={14} />
                    </button>
                    <button style={s.popupIconBtn} onClick={() => reindexMutation.mutate(popupDoc.id)} title="Re-index in AI">
                      <RefreshCw size={14} />
                    </button>
                  </>
                )}
                <button style={s.popupCloseBtn} onClick={closePopup} title="Close">
                  <X size={16} />
                </button>
              </div>
            </div>

            {/* Description row in edit mode */}
            {editMode && (
              <div style={{ padding: '0 24px 12px' }}>
                <label style={s.label}>Short description</label>
                <input
                  style={s.input}
                  value={editDesc}
                  onChange={e => setEditDesc(e.target.value)}
                  placeholder="Brief description of this document"
                />
              </div>
            )}

            {/* Content area */}
            <div style={s.popupContent}>
              {loadingFull && !fullDocData ? (
                <div style={{ textAlign: 'center', color: '#64748b', padding: 40 }}>
                  <Loader2 size={24} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 8px', display: 'block' }} />
                  Loading full content…
                </div>
              ) : editMode ? (
                <div>
                  <label style={{ ...s.label, marginBottom: 8 }}>
                    Content — edits will automatically re-index this document in the AI
                  </label>
                  <textarea
                    style={{ ...s.textarea, minHeight: 400, width: '100%', boxSizing: 'border-box' }}
                    value={editContent}
                    onChange={e => setEditContent(e.target.value)}
                    placeholder="Paste or type document content here…"
                  />
                  <div style={s.charHint}>
                    {editContent.split(/\s+/).filter(Boolean).length.toLocaleString()} words
                  </div>
                </div>
              ) : (
                <pre style={s.previewText}>{fullDocData?.fullContent ?? 'Loading…'}</pre>
              )}
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>
    </div>
  );
}

// ── Doc Card subcomponent ─────────────────────────────────────────────────

function DocCard({ doc, onOpen, onDelete, onReindex, isReindexing }: {
  doc: CompanyDoc;
  onOpen: () => void;
  onDelete: () => void;
  onReindex: () => void;
  isReindexing: boolean;
}) {
  const typeColor = TYPE_COLORS[doc.fileType] || '#6366f1';

  return (
    <div style={s.docCard} onClick={onOpen}>
      {/* Card header */}
      <div style={s.cardHeader}>
        <span style={{ ...s.typeTag, color: typeColor, borderColor: typeColor + '40', background: typeColor + '15' }}>
          {doc.fileType}
        </span>
        {doc.indexedInRag
          ? <span style={s.indexedTag}><CheckCircle2 size={10} /> Indexed</span>
          : <span style={s.pendingTag}><Clock size={10} /> Pending</span>
        }
      </div>

      {/* AI heading (primary identifier) */}
      <div style={s.cardHeading}>{doc.aiHeading || doc.title}</div>

      {/* Title (if different from heading) */}
      {doc.aiHeading && doc.aiHeading !== doc.title && (
        <div style={s.cardTitle}>{doc.title}</div>
      )}

      {/* Content preview */}
      {doc.contentPreview && (
        <div style={s.cardPreview}>{doc.contentPreview}</div>
      )}

      {/* Footer */}
      <div style={s.cardFooter}>
        <div style={s.cardMeta}>
          <span style={s.metaItem}><AlignLeft size={10} /> {doc.wordCount.toLocaleString()} words</span>
          <span style={s.metaItem}>{new Date(doc.createdAt).toLocaleDateString()}</span>
        </div>
        <div style={s.cardActions} onClick={e => e.stopPropagation()}>
          <button
            style={s.cardBtn}
            title="Re-index in AI"
            onClick={onReindex}
            disabled={isReindexing}
          >
            <RefreshCw size={12} />
          </button>
          <button
            style={{ ...s.cardBtn, color: '#f87171' }}
            title="Delete"
            onClick={onDelete}
          >
            <Trash2 size={12} />
          </button>
          <button style={{ ...s.cardBtn, color: '#a5b4fc' }} title="View full content">
            <ChevronRight size={12} />
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────

const s: Record<string, React.CSSProperties> = {
  page: { padding: '32px', fontFamily: "'Inter', system-ui, sans-serif", color: '#f1f5f9', maxWidth: 1100 },
  pageHeader: { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 20 },
  headerLeft: { display: 'flex', alignItems: 'center', gap: 14 },
  headerIcon: {
    width: 48, height: 48,
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center',
    flexShrink: 0,
  },
  pageTitle: { fontSize: 22, fontWeight: 700, margin: '0 0 4px', color: '#f8fafc' },
  pageSubtitle: { fontSize: 13, color: '#64748b', margin: 0 },
  uploadBtn: {
    display: 'flex', alignItems: 'center', gap: 6,
    padding: '10px 20px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    color: '#fff', border: 'none', borderRadius: 10,
    fontSize: 13, fontWeight: 600, cursor: 'pointer',
    whiteSpace: 'nowrap',
  },

  aiCard: {
    display: 'flex', gap: 12, alignItems: 'flex-start',
    padding: '14px 18px', marginBottom: 28,
    background: 'rgba(99,102,241,0.07)',
    border: '1px solid rgba(99,102,241,0.2)',
    borderRadius: 12, fontSize: 13,
  },
  aiCardTitle: { fontWeight: 600, marginBottom: 4, color: '#c7d2fe' },
  aiCardBody: { color: '#94a3b8', lineHeight: 1.7 },

  // Upload form
  formCard: {
    background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: 14, padding: 24, marginBottom: 28,
    animation: 'fadeIn 0.2s ease',
  },
  formHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  formTitle: { fontSize: 16, fontWeight: 700, margin: 0, color: '#f1f5f9' },
  closeBtn: { background: 'none', border: 'none', color: '#64748b', cursor: 'pointer', display: 'flex', alignItems: 'center', padding: 4 },
  form: { display: 'flex', flexDirection: 'column', gap: 16 },
  formRow: { display: 'flex', gap: 12 },
  label: { display: 'block', fontSize: 12, fontWeight: 500, color: '#94a3b8', marginBottom: 6 },
  input: {
    width: '100%', padding: '9px 12px', boxSizing: 'border-box' as const,
    background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: 8, color: '#f1f5f9', fontSize: 13, outline: 'none',
  },
  select: {
    width: '100%', padding: '9px 12px', boxSizing: 'border-box' as const,
    background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: 8, color: '#f1f5f9', fontSize: 13, outline: 'none',
  },
  textarea: {
    width: '100%', padding: '10px 12px', resize: 'vertical' as const, boxSizing: 'border-box' as const,
    background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: 8, color: '#f1f5f9', fontSize: 13, outline: 'none',
    fontFamily: 'inherit', lineHeight: 1.6,
  },
  charHint: { fontSize: 11, color: '#475569', marginTop: 4, textAlign: 'right' as const },
  formActions: { display: 'flex', justifyContent: 'flex-end', gap: 10 },
  cancelBtn: {
    padding: '9px 18px', background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: 8, color: '#94a3b8', cursor: 'pointer', fontSize: 13,
  },
  submitBtn: {
    display: 'flex', alignItems: 'center', gap: 6,
    padding: '9px 20px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    color: '#fff', border: 'none', borderRadius: 8,
    fontSize: 13, fontWeight: 600, cursor: 'pointer',
  },

  // Empty / loading states
  emptyText: { color: '#64748b', textAlign: 'center' as const, padding: '40px 0', fontSize: 14 },
  emptyState: {
    textAlign: 'center' as const, padding: '56px 24px',
    background: 'rgba(255,255,255,0.02)',
    border: '1.5px dashed rgba(255,255,255,0.08)',
    borderRadius: 16, display: 'flex', flexDirection: 'column' as const, alignItems: 'center',
  },

  // Doc grid
  docGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))',
    gap: 16,
  },

  // Doc card
  docCard: {
    background: 'rgba(255,255,255,0.04)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: 14, padding: '18px 18px 14px',
    display: 'flex', flexDirection: 'column' as const, gap: 10,
    cursor: 'pointer', transition: 'all 0.18s ease',
    position: 'relative' as const,
    animation: 'fadeIn 0.2s ease',
  },
  cardHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
  typeTag: {
    fontSize: 10, fontWeight: 700, letterSpacing: '0.6px',
    padding: '2px 8px', borderRadius: 5, border: '1px solid',
  },
  cardHeading: { fontSize: 15, fontWeight: 700, color: '#f1f5f9', lineHeight: 1.4 },
  cardTitle: { fontSize: 12, color: '#64748b', marginTop: -4 },
  cardPreview: {
    fontSize: 12, color: '#475569', lineHeight: 1.65,
    display: '-webkit-box',
    WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical' as const,
    overflow: 'hidden',
  },
  cardFooter: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto' as const, paddingTop: 8 },
  cardMeta: { display: 'flex', gap: 10, alignItems: 'center' },
  metaItem: { display: 'flex', alignItems: 'center', gap: 3, fontSize: 11, color: '#475569' },
  cardActions: { display: 'flex', gap: 4 },
  cardBtn: {
    padding: 5, background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: 6, color: '#64748b', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },

  // Status badges
  indexedTag: {
    display: 'inline-flex', alignItems: 'center', gap: 4,
    fontSize: 11, color: '#4ade80', background: 'rgba(74,222,128,0.1)',
    padding: '2px 8px', borderRadius: 999,
  },
  pendingTag: {
    display: 'inline-flex', alignItems: 'center', gap: 4,
    fontSize: 11, color: '#fbbf24', background: 'rgba(251,191,36,0.1)',
    padding: '2px 8px', borderRadius: 999,
  },

  // Popup overlay
  overlay: {
    position: 'fixed' as const, inset: 0,
    background: 'rgba(0,0,0,0.7)',
    backdropFilter: 'blur(4px)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    zIndex: 1000, padding: 24,
    animation: 'fadeIn 0.18s ease',
  },

  // Popup container
  popup: {
    background: '#0f172a',
    border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: 18, width: '100%', maxWidth: 860,
    maxHeight: '90vh', display: 'flex', flexDirection: 'column' as const,
    overflow: 'hidden', boxShadow: '0 40px 80px rgba(0,0,0,0.6)',
  },
  popupHeader: {
    display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
    padding: '20px 24px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)',
    gap: 12,
  },
  popupHeaderLeft: { display: 'flex', alignItems: 'flex-start', gap: 12, flex: 1, minWidth: 0 },
  popupTypeTag: {
    fontSize: 10, fontWeight: 700, color: '#fff',
    padding: '4px 10px', borderRadius: 6, flexShrink: 0, marginTop: 4,
  },
  popupTitle: { fontSize: 18, fontWeight: 700, margin: '0 0 6px', color: '#f8fafc' },
  popupMeta: { display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' as const },
  popupActions: { display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0 },
  popupEditBtn: {
    display: 'flex', alignItems: 'center', gap: 5,
    padding: '6px 14px', background: 'rgba(99,102,241,0.15)',
    border: '1px solid rgba(99,102,241,0.3)',
    borderRadius: 8, color: '#a5b4fc', cursor: 'pointer', fontSize: 13,
  },
  popupSaveBtn: {
    display: 'flex', alignItems: 'center', gap: 5,
    padding: '6px 16px',
    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
    border: 'none', borderRadius: 8, color: '#fff', cursor: 'pointer', fontSize: 13, fontWeight: 600,
  },
  popupIconBtn: {
    padding: 7, background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: 8, color: '#64748b', cursor: 'pointer',
    display: 'flex', alignItems: 'center',
  },
  popupCloseBtn: {
    padding: 7, background: 'none', border: 'none',
    color: '#475569', cursor: 'pointer', display: 'flex', alignItems: 'center',
    marginLeft: 4,
  },
  popupContent: {
    flex: 1, overflow: 'auto', padding: 24,
  },
  previewText: {
    margin: 0, whiteSpace: 'pre-wrap' as const, wordBreak: 'break-word' as const,
    fontSize: 14, color: '#94a3b8', lineHeight: 1.8,
    fontFamily: "'JetBrains Mono', 'Courier New', monospace",
  },
};
