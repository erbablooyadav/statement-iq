import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './store/AuthContext';
import AppShell from './components/layout/AppShell';
import PwaInstallPrompt from './components/PwaInstallPrompt';
import Landing from './pages/Landing';
import Auth from './pages/Auth';
import Dashboard from './pages/Dashboard';
import Upload from './pages/Upload';
import StatementList from './pages/StatementList';
import StatementDetail from './pages/StatementDetail';
import Goals from './pages/Goals';
import Alerts from './pages/Alerts';
import Settings from './pages/Settings';
import Pricing from './pages/Pricing';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: 'var(--color-bg-primary)' }}>
        <div className="w-12 h-12 rounded-full border-4 border-t-transparent animate-spin" style={{ borderColor: 'var(--color-brand-500)', borderTopColor: 'transparent' }} />
      </div>
    );
  }
  return user ? <>{children}</> : <Navigate to="/auth" replace />;
}

export default function App() {
  const { user } = useAuth();

  return (
    <>
      {/* Global PWA Install Banner — visible to ALL visitors */}
      <PwaInstallPrompt />

      <Routes>
        {/* Public Routes */}
        <Route path="/" element={user ? <Navigate to="/dashboard" replace /> : <Landing />} />
        <Route path="/auth" element={user ? <Navigate to="/dashboard" replace /> : <Auth />} />
        <Route path="/pricing" element={<Pricing />} />

        {/* Authenticated Routes with App Shell */}
        <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/upload" element={<Upload />} />
          <Route path="/statements" element={<StatementList />} />
          <Route path="/statements/:id" element={<StatementDetail />} />
          <Route path="/goals" element={<Goals />} />
          <Route path="/alerts" element={<Alerts />} />
          <Route path="/settings" element={<Settings />} />
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}
