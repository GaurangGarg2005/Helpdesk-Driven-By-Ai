import React from 'react';
import styles from './Badge.module.css';

interface BadgeProps {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'ai';
  size?: 'sm' | 'md';
  children: React.ReactNode;
  dot?: boolean;
}

export function Badge({ variant = 'default', size = 'md', children, dot }: BadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[variant]} ${styles[size]}`}>
      {dot && <span className={styles.dot} />}
      {children}
    </span>
  );
}

export default Badge;

// Convenience helpers for ticket fields
export function StatusBadge({ status }: { status: string }) {
  const map: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'danger'> = {
    OPEN:                'danger',
    IN_PROGRESS:         'primary',
    WAITING_ON_CUSTOMER: 'warning',
    RESOLVED:            'success',
    CLOSED:              'default',
  };
  const labels: Record<string, string> = {
    OPEN:                'Open',
    IN_PROGRESS:         'In Progress',
    WAITING_ON_CUSTOMER: 'Waiting',
    RESOLVED:            'Resolved',
    CLOSED:              'Closed',
  };
  return <Badge variant={map[status] ?? 'default'} dot>{labels[status] ?? status}</Badge>;
}

export function PriorityBadge({ priority }: { priority: string }) {
  const map: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'danger'> = {
    LOW:    'success',
    MEDIUM: 'primary',
    HIGH:   'warning',
    URGENT: 'danger',
  };
  return <Badge variant={map[priority] ?? 'default'}>{priority}</Badge>;
}
