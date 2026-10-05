import { useQuery } from '@tanstack/react-query';
import { Users, Plus } from 'lucide-react';
import api from '@/lib/api';
import Button from '@/components/ui/Button';
import styles from './TeamPage.module.css';

export default function TeamPage() {
  const { data: teams, isLoading } = useQuery({
    queryKey: ['teams'],
    queryFn: async () => {
      const res = await api.get('/org/teams');
      return res.data.data;
    },
  });

  const { data: departments } = useQuery({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get('/org/departments');
      return res.data.data;
    },
  });

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>Team</h1>
        <Button size="sm" icon={<Plus size={14} />}>Add Team</Button>
      </div>

      <div className={styles.grid}>
        {/* Teams */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>
            <Users size={16} /> Teams
          </h2>
          {isLoading && (
            <div className={styles.loadingList}>
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="skeleton" style={{ height: 64, borderRadius: 12 }} />
              ))}
            </div>
          )}
          {!isLoading && (!teams || teams.length === 0) && (
            <p className={styles.empty}>No teams yet. Create your first team to get started.</p>
          )}
          {(teams ?? []).map((team: any) => (
            <div key={team.id} className={styles.card}>
              <div className={styles.cardIcon}>
                {team.name?.[0]?.toUpperCase()}
              </div>
              <div className={styles.cardBody}>
                <p className={styles.cardName}>{team.name}</p>
                <p className={styles.cardMeta}>{team.memberCount ?? 0} members</p>
                {team.description && <p className={styles.cardDesc}>{team.description}</p>}
              </div>
            </div>
          ))}
        </section>

        {/* Departments */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Departments</h2>
          {(departments ?? []).map((dept: any) => (
            <div key={dept.id} className={styles.card}>
              <div className={`${styles.cardIcon} ${styles.cardIconPurple}`}>
                {dept.name?.[0]?.toUpperCase()}
              </div>
              <div className={styles.cardBody}>
                <p className={styles.cardName}>{dept.name}</p>
                {dept.description && <p className={styles.cardDesc}>{dept.description}</p>}
              </div>
            </div>
          ))}
          {(!departments || departments.length === 0) && (
            <p className={styles.empty}>No departments configured.</p>
          )}
        </section>
      </div>
    </div>
  );
}
