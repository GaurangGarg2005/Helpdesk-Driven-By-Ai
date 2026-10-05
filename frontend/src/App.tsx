import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

// Layouts
import AppLayout from '@/components/layout/AppLayout';
import AuthLayout from '@/components/layout/AuthLayout';

// Agent/Admin Auth pages
import LoginPage       from '@/pages/auth/LoginPage';
import RegisterPage    from '@/pages/auth/RegisterPage';
import AgentRegisterPage from '@/pages/auth/AgentRegisterPage';

// Customer Auth pages
import CustomerLoginPage    from '@/pages/customer/CustomerLoginPage';
import CustomerRegisterPage from '@/pages/customer/CustomerRegisterPage';

// Customer Portal pages
import CustomerPortalPage      from '@/pages/customer/CustomerPortalPage';
import CustomerTicketFormPage  from '@/pages/customer/CustomerTicketFormPage';
import CustomerTicketChatPage  from '@/pages/customer/CustomerTicketChatPage';

// Agent/Admin pages
import DashboardPage   from '@/pages/dashboard/DashboardPage';
import TicketsPage     from '@/pages/tickets/TicketsPage';
import TicketDetailPage from '@/pages/tickets/TicketDetailPage';
import NewTicketPage   from '@/pages/tickets/NewTicketPage';
import ChatPage        from '@/pages/chat/ChatPage';
import KbPage          from '@/pages/kb/KbPage';
import KbEditorPage    from '@/pages/kb/KbEditorPage';
import CompanyKnowledgePage from '@/pages/kb/CompanyKnowledgePage';
import AnalyticsPage   from '@/pages/analytics/AnalyticsPage';
import TeamPage        from '@/pages/org/TeamPage';
import SettingsPage    from '@/pages/settings/SettingsPage';

// Public portal
import PortalPage from '@/pages/portal/PortalPage';

// ── Route guards ──────────────────────────────────────────────────────────────

/** Requires any valid login. Redirects to /login if not authenticated. */
function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore(s => s.token);
  return token ? <>{children}</> : <Navigate to="/login" replace />;
}

/** Requires CUSTOMER role. Redirects to /customer/login if wrong role or not logged in. */
function CustomerRoute({ children }: { children: React.ReactNode }) {
  const { token, user } = useAuthStore(s => ({ token: s.token, user: s.user }));
  if (!token) return <Navigate to="/customer/login" replace />;
  if (user?.role !== 'CUSTOMER') return <Navigate to="/customer/login" replace />;
  return <>{children}</>;
}

/** Requires AGENT, ADMIN, or OWNER role. */
function AgentRoute({ children }: { children: React.ReactNode }) {
  const { token, user } = useAuthStore(s => ({ token: s.token, user: s.user }));
  if (!token) return <Navigate to="/login" replace />;
  if (user?.role === 'CUSTOMER') return <Navigate to="/customer/portal" replace />;
  return <>{children}</>;
}

/** Role-aware index redirect: AGENT → /tickets, everyone else → /dashboard */
function AgentIndexRedirect() {
  const user = useAuthStore(s => s.user);
  return <Navigate to={user?.role === 'AGENT' ? '/tickets' : '/dashboard'} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>

        {/* ── Agent / Admin Auth ── */}
        <Route element={<AuthLayout />}>
          <Route path="/login"          element={<LoginPage />} />
          <Route path="/register"        element={<RegisterPage />} />
          <Route path="/register/agent" element={<AgentRegisterPage />} />
        </Route>

        {/* ── Customer Auth ── */}
        <Route path="/customer/login"    element={<CustomerLoginPage />} />
        <Route path="/customer/register" element={<CustomerRegisterPage />} />

        {/* ── Customer Portal (authenticated, CUSTOMER role required) ── */}
        <Route path="/customer/portal" element={
          <CustomerRoute><CustomerPortalPage /></CustomerRoute>
        } />
        <Route path="/customer/portal/new" element={
          <CustomerRoute><CustomerTicketFormPage /></CustomerRoute>
        } />
        <Route path="/customer/portal/tickets/:id" element={
          <CustomerRoute><CustomerTicketChatPage /></CustomerRoute>
        } />

        {/* ── Public customer-facing portal (no auth needed) ── */}
        <Route path="/portal"          element={<PortalPage />} />
        <Route path="/portal/:orgSlug" element={<PortalPage />} />

        {/* ── Agent / Admin App ── */}
        <Route path="/" element={
          <AgentRoute>
            <AppLayout />
          </AgentRoute>
        }>
          <Route index element={<AgentIndexRedirect />} />
          <Route path="dashboard"    element={<DashboardPage />} />
          <Route path="tickets"      element={<TicketsPage />} />
          <Route path="tickets/new"  element={<NewTicketPage />} />
          <Route path="tickets/:id"  element={<TicketDetailPage />} />
          <Route path="chat"         element={<ChatPage />} />
          <Route path="kb"           element={<KbPage />} />
          <Route path="kb/new"       element={<KbEditorPage />} />
          <Route path="kb/:id/edit"  element={<KbEditorPage />} />
          <Route path="kb/company"   element={<CompanyKnowledgePage />} />
          <Route path="analytics"    element={<AnalyticsPage />} />
          <Route path="team"         element={<TeamPage />} />
          <Route path="settings"     element={<SettingsPage />} />
          <Route path="*"            element={<Navigate to="/dashboard" replace />} />
        </Route>

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
