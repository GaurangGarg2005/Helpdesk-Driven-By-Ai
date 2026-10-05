import { Outlet } from 'react-router-dom';
import styles from './AuthLayout.module.css';

export default function AuthLayout() {
  return (
    <div className={styles.root}>
      {/* Left panel — branding */}
      <div className={styles.branding}>
        <div className={styles.brandingInner}>
          <div className={styles.logo}>
            <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
              <rect width="36" height="36" rx="10" fill="#4F46E5" />
              <path d="M9 12h18M9 18h12M9 24h15" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
              <circle cx="27" cy="24" r="4.5" fill="#14B8A6" />
            </svg>
            <span>HelpDeskAI</span>
          </div>

          <div className={styles.brandingContent}>
            <h2 className={styles.tagline}>
              AI-powered support.<br />
              Enterprise-grade results.
            </h2>
            <p className={styles.taglineSub}>
              Resolve 40% more tickets automatically. Delight every customer, every time.
            </p>

            <div className={styles.stats}>
              {[
                { value: '40%', label: 'Auto-resolved' },
                { value: '2min', label: 'Avg. response' },
                { value: '98%', label: 'CSAT score' },
              ].map(s => (
                <div key={s.label} className={styles.stat}>
                  <span className={styles.statValue}>{s.value}</span>
                  <span className={styles.statLabel}>{s.label}</span>
                </div>
              ))}
            </div>
          </div>

          <div className={styles.brandingDecoration}>
            <div className={styles.orb1} />
            <div className={styles.orb2} />
          </div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className={styles.formPanel}>
        <div className={styles.formWrapper}>
          <Outlet />
        </div>
      </div>
    </div>
  );
}
