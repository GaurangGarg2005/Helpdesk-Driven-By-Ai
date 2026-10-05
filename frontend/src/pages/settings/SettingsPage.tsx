import React, { useState } from 'react';
import { Settings, Bell, Shield, Palette, Key, Globe, ChevronRight } from 'lucide-react';
import styles from './SettingsPage.module.css';
import GeneralSettings from './sections/GeneralSettings';
import BrandingSettings from './sections/BrandingSettings';
import ApiKeysSettings from './sections/ApiKeysSettings';
import WebhooksSettings from './sections/WebhooksSettings';
import SecuritySettings from './sections/SecuritySettings';

const NAV = [
  { id: 'general',   label: 'General',       icon: Settings,  desc: 'Org name, timezone, language' },
  { id: 'branding',  label: 'Branding',      icon: Palette,   desc: 'Colors, logo, custom domain' },
  { id: 'webhooks',  label: 'Webhooks',      icon: Globe,     desc: 'Outbound event subscriptions' },
  { id: 'api-keys',  label: 'API Keys',      icon: Key,       desc: 'Programmatic access tokens' },
  { id: 'security',  label: 'Security',      icon: Shield,    desc: 'Audit logs & session control' },
] as const;

type SectionId = typeof NAV[number]['id'];

const SECTION_MAP: Record<SectionId, React.ReactNode> = {
  'general':   <GeneralSettings />,
  'branding':  <BrandingSettings />,
  'api-keys':  <ApiKeysSettings />,
  'webhooks':  <WebhooksSettings />,
  'security':  <SecuritySettings />,
};

export default function SettingsPage() {
  const [active, setActive] = useState<SectionId>('general');

  return (
    <div className={styles.page}>
      {/* Page heading */}
      <div className={styles.header}>
        <h1 className={styles.title}>Settings</h1>
        <p className={styles.subtitle}>Manage your organization configuration and preferences.</p>
      </div>

      <div className={styles.layout}>
        {/* Left nav */}
        <nav className={styles.nav}>
          {NAV.map(({ id, label, icon: Icon, desc }) => (
            <button
              key={id}
              className={`${styles.navItem} ${active === id ? styles.navItemActive : ''}`}
              onClick={() => setActive(id)}
            >
              <div className={`${styles.navIcon} ${active === id ? styles.navIconActive : ''}`}>
                <Icon size={16} />
              </div>
              <div className={styles.navText}>
                <span className={styles.navLabel}>{label}</span>
                <span className={styles.navDesc}>{desc}</span>
              </div>
              <ChevronRight size={14} className={styles.navChevron} />
            </button>
          ))}
        </nav>

        {/* Right panel */}
        <main className={styles.panel}>
          {SECTION_MAP[active]}
        </main>
      </div>
    </div>
  );
}
