import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Plus, BookOpen, Eye, ThumbsUp, Star } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import styles from './KbPage.module.css';

export default function KbPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const queryClient = useQueryClient();

  const { data: articles, isLoading } = useQuery({
    queryKey: ['kb-articles', status],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '50' });
      if (status !== 'ALL') params.set('status', status);
      const r = await api.get(`/kb/articles?${params}`);
      return r.data.data?.content ?? [];
    },
  });

  const { data: categories = [] } = useQuery({
    queryKey: ['kb-categories'],
    queryFn: async () => {
      const r = await api.get('/kb/categories');
      return r.data.data ?? [];
    },
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) => api.post(`/kb/articles/${id}/publish`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['kb-articles'] }),
  });

  const filtered = search
    ? (articles ?? []).filter((a: any) =>
        a.title?.toLowerCase().includes(search.toLowerCase()) ||
        a.tags?.toLowerCase().includes(search.toLowerCase())
      )
    : articles ?? [];

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Knowledge Base</h1>
          <p className={styles.subtitle}>{(articles ?? []).length} articles</p>
        </div>
        <div className={styles.actions}>
          <Link to="/kb/new">
            <Button size="sm" icon={<Plus size={14} />}>New Article</Button>
          </Link>
        </div>
      </div>

      {/* Stats bar */}
      <div className={styles.statsBar}>
        {categories.slice(0, 5).map((cat: any) => (
          <div key={cat.id} className={styles.statChip}>
            <BookOpen size={12} />
            <span>{cat.name}</span>
            <span className={styles.statCount}>{cat.articleCount}</span>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className={styles.filtersRow}>
        <div className={styles.statusTabs}>
          {['ALL', 'DRAFT', 'PUBLISHED', 'ARCHIVED'].map(s => (
            <button
              key={s}
              className={`${styles.tab} ${status === s ? styles.tabActive : ''}`}
              onClick={() => setStatus(s)}
            >
              {s}
            </button>
          ))}
        </div>
        <div className={styles.searchWrap}>
          <Search size={14} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="Search articles…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Articles grid */}
      {isLoading ? (
        <div className={styles.loadingGrid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton" style={{ height: 140, borderRadius: 12 }} />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className={styles.empty}>
          <BookOpen size={40} color="var(--neutral-300)" />
          <p>No articles yet</p>
          <Link to="/kb/new"><Button size="sm">Write your first article</Button></Link>
        </div>
      ) : (
        <div className={styles.grid}>
          {filtered.map((article: any) => (
            <Link key={article.id} to={`/kb/${article.id}`} className={styles.card}>
              <div className={styles.cardHeader}>
                <span className={`${styles.statusDot} ${styles[`dot_${article.status}`]}`} />
                <span className={styles.statusText}>{article.status}</span>
                {article.isFeatured && (
                  <Star size={12} className={styles.featuredStar} />
                )}
              </div>
              <h3 className={styles.cardTitle}>{article.title}</h3>
              {article.excerpt && (
                <p className={styles.cardExcerpt}>{article.excerpt}</p>
              )}
              {article.tags && (
                <div className={styles.cardTags}>
                  {article.tags.split(',').slice(0, 3).map((t: string) => (
                    <Badge key={t} variant="default" size="sm">{t.trim()}</Badge>
                  ))}
                </div>
              )}
              <div className={styles.cardFooter}>
                <span className={styles.cardMeta}><Eye size={12} /> {article.viewCount}</span>
                <span className={styles.cardMeta}><ThumbsUp size={12} /> {article.helpfulCount}</span>
                {article.status === 'DRAFT' && (
                  <button
                    className={styles.publishBtn}
                    onClick={e => { e.preventDefault(); publishMutation.mutate(article.id); }}
                  >
                    Publish
                  </button>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
