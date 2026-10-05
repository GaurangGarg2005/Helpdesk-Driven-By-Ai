import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Save, Send, ArrowLeft, Star, Eye } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from './KbEditorPage.module.css';

export default function KbEditorPage() {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditing = Boolean(id);

  const [form, setForm] = useState({
    title: '',
    excerpt: '',
    content: '',
    tags: '',
    categoryId: '',
    isFeatured: false,
    status: 'DRAFT',
    seoTitle: '',
    seoDescription: '',
  });

  // Load existing article if editing
  const { data: existing } = useQuery({
    queryKey: ['kb-article', id],
    enabled: isEditing,
    queryFn: async () => {
      const r = await api.get(`/kb/articles/${id}`);
      return r.data.data;
    },
  });

  const { data: categories = [] } = useQuery({
    queryKey: ['kb-categories'],
    queryFn: async () => {
      const r = await api.get('/kb/categories');
      return r.data.data ?? [];
    },
  });

  useEffect(() => {
    if (existing) {
      setForm({
        title:          existing.title          ?? '',
        excerpt:        existing.excerpt        ?? '',
        content:        existing.content        ?? '',
        tags:           existing.tags           ?? '',
        categoryId:     existing.categoryId     ?? '',
        isFeatured:     existing.isFeatured     ?? false,
        status:         existing.status         ?? 'DRAFT',
        seoTitle:       existing.seoTitle       ?? '',
        seoDescription: existing.seoDescription ?? '',
      });
    }
  }, [existing]);

  const saveMutation = useMutation({
    mutationFn: async (asDraft: boolean) => {
      const payload = { ...form, status: asDraft ? 'DRAFT' : 'PUBLISHED' };
      if (isEditing) {
        return api.put(`/kb/articles/${id}`, payload);
      }
      return api.post('/kb/articles', payload);
    },
    onSuccess: (_, asDraft) => {
      queryClient.invalidateQueries({ queryKey: ['kb-articles'] });
      toast.success(asDraft ? 'Saved as draft' : 'Article published!');
      navigate('/kb');
    },
    onError: () => toast.error('Failed to save article'),
  });

  const f = (field: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));

  const wordCount = form.content.trim().split(/\s+/).filter(Boolean).length;

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate('/kb')}>
          <ArrowLeft size={16} /> Back to KB
        </button>
        <div className={styles.headerActions}>
          <span className={styles.wordCount}>{wordCount} words</span>
          <Button variant="secondary" size="sm" icon={<Save size={14} />}
            loading={saveMutation.isPending}
            onClick={() => saveMutation.mutate(true)}>
            Save Draft
          </Button>
          <Button size="sm" icon={<Send size={14} />}
            loading={saveMutation.isPending}
            onClick={() => saveMutation.mutate(false)}>
            Publish
          </Button>
        </div>
      </div>

      <div className={styles.layout}>
        {/* Editor panel */}
        <div className={styles.editorPanel}>
          {/* Title */}
          <input
            className={styles.titleInput}
            placeholder="Article title…"
            value={form.title}
            onChange={f('title')}
          />

          {/* Excerpt */}
          <textarea
            className={styles.excerptInput}
            placeholder="Short excerpt (shown in search results)…"
            value={form.excerpt}
            onChange={f('excerpt')}
            rows={2}
          />

          {/* Content area — Markdown */}
          <div className={styles.editorWrap}>
            <div className={styles.editorToolbar}>
              <span className={styles.editorLabel}>Content (Markdown supported)</span>
              <button
                className={styles.previewToggle}
                onClick={() => {}} // preview handled below
              >
                <Eye size={14} /> Preview
              </button>
            </div>
            <textarea
              className={styles.contentTextarea}
              placeholder={`# Getting Started\n\nWrite your article content here using **Markdown**.\n\n## Section 1\n\nYour content...`}
              value={form.content}
              onChange={f('content')}
              rows={24}
              spellCheck
            />
          </div>
        </div>

        {/* Sidebar */}
        <aside className={styles.sidePanel}>

          {/* Status */}
          <div className={styles.sideSection}>
            <h3 className={styles.sideTitle}>Status</h3>
            <div className={styles.statusChips}>
              {['DRAFT', 'PUBLISHED', 'ARCHIVED'].map(s => (
                <button
                  key={s}
                  className={`${styles.statusChip} ${form.status === s ? styles.statusChipActive : ''}`}
                  onClick={() => setForm(prev => ({ ...prev, status: s }))}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* Category */}
          <div className={styles.sideSection}>
            <h3 className={styles.sideTitle}>Category</h3>
            <select className={styles.select} value={form.categoryId} onChange={f('categoryId')}>
              <option value="">No category</option>
              {categories.map((c: any) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          {/* Tags */}
          <div className={styles.sideSection}>
            <h3 className={styles.sideTitle}>Tags <span className={styles.hint}>(comma-separated)</span></h3>
            <input
              className={styles.input}
              placeholder="billing, refund, password…"
              value={form.tags}
              onChange={f('tags')}
            />
          </div>

          {/* Featured */}
          <div className={styles.sideSection}>
            <label className={styles.checkboxRow}>
              <input
                type="checkbox"
                checked={form.isFeatured}
                onChange={e => setForm(prev => ({ ...prev, isFeatured: e.target.checked }))}
                className={styles.checkbox}
              />
              <Star size={14} className={form.isFeatured ? styles.starActive : styles.starInactive} />
              <span>Featured article</span>
            </label>
          </div>

          {/* SEO */}
          <div className={styles.sideSection}>
            <h3 className={styles.sideTitle}>SEO</h3>
            <input
              className={styles.input}
              placeholder="SEO title (optional)"
              value={form.seoTitle}
              onChange={f('seoTitle')}
            />
            <textarea
              className={`${styles.input} ${styles.seoDesc}`}
              placeholder="Meta description (optional)"
              value={form.seoDescription}
              onChange={f('seoDescription')}
              rows={3}
            />
          </div>

          {/* Preview link if editing */}
          {isEditing && existing?.slug && (
            <div className={styles.sideSection}>
              <a
                href={`/portal?org=${existing.organizationId}`}
                target="_blank"
                rel="noreferrer"
                className={styles.previewLink}
              >
                <Eye size={14} /> Preview public page
              </a>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
