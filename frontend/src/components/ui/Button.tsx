import React, { forwardRef } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'ai';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  icon?: React.ReactNode;
  iconRight?: React.ReactNode;
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(({
  variant = 'primary', size = 'md', loading, icon, iconRight,
  children, disabled, className = '', ...rest
}, ref) => (
  <button
    ref={ref}
    className={`${styles.btn} ${styles[variant]} ${styles[size]} ${className}`}
    disabled={disabled || loading}
    {...rest}
  >
    {loading ? (
      <span className={`${styles.spinner} animate-spin`} />
    ) : icon ? (
      <span className={styles.icon}>{icon}</span>
    ) : null}
    {children && <span>{children}</span>}
    {iconRight && !loading && <span className={styles.icon}>{iconRight}</span>}
  </button>
));

Button.displayName = 'Button';
export default Button;
