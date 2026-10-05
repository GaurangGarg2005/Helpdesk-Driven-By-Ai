import { Outlet } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import TopBar from '@/components/layout/TopBar';
import styles from './AppLayout.module.css';

export default function AppLayout() {
  return (
    <div className={styles.root}>
      <Sidebar />
      <div className={styles.main}>
        <TopBar />
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
