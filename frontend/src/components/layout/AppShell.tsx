import { Outlet, NavLink, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, Upload, FileText, Target, Bell, Settings,
  Shield, Menu, X
} from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../../store/AuthContext';
import Footer from './Footer';
import AiChat from './AiChat';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Home' },
  { to: '/upload', icon: Upload, label: 'Upload' },
  { to: '/statements', icon: FileText, label: 'Statements' },
  { to: '/goals', icon: Target, label: 'Goals' },
  { to: '/alerts', icon: Bell, label: 'Alerts' },
];

export default function AppShell() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--color-bg-primary)', color: 'var(--color-text-primary)' }}>

      {/* Top Header */}
      <header className="sticky top-0 z-50 glass-card" style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
        <div className="flex items-center justify-between px-4 py-3 max-w-7xl mx-auto">
          <NavLink to="/dashboard" className="flex items-center gap-2">
            <Shield className="w-6 h-6" style={{ color: 'var(--color-brand-500)' }} />
            <h1 className="text-lg font-bold gradient-text">StatementIQ</h1>
          </NavLink>
          
          <div className="flex items-center gap-3">
            {user?.plan === 'PRO' && (
              <span className="text-xs font-semibold px-2 py-1 rounded-full" style={{ background: 'var(--color-brand-600)', color: 'white' }}>
                PRO
              </span>
            )}
            
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              className="touch-target md:hidden"
              aria-label="Menu"
            >
              {menuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>

            {/* Desktop nav */}
            <nav className="hidden md:flex items-center gap-1">
              {navItems.map(({ to, icon: Icon, label }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    `flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                      isActive
                        ? 'gradient-brand text-white'
                        : 'hover:bg-[var(--color-bg-elevated)]'
                    }`
                  }
                >
                  <Icon className="w-4 h-4" />
                  {label}
                </NavLink>
              ))}
              <NavLink
                to="/settings"
                className={({ isActive }) =>
                  `flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive ? 'gradient-brand text-white' : 'hover:bg-[var(--color-bg-elevated)]'
                  }`
                }
              >
                <Settings className="w-4 h-4" />
              </NavLink>
            </nav>
          </div>
        </div>

        {/* Mobile dropdown menu */}
        {menuOpen && (
          <div className="md:hidden animate-slide-up px-4 pb-4 space-y-1" style={{ borderTop: '1px solid var(--color-border)' }}>
            {navItems.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                onClick={() => setMenuOpen(false)}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-all ${
                    isActive ? 'gradient-brand text-white' : 'hover:bg-[var(--color-bg-elevated)]'
                  }`
                }
              >
                <Icon className="w-5 h-5" />
                {label}
              </NavLink>
            ))}
            <NavLink to="/settings" onClick={() => setMenuOpen(false)} className="flex items-center gap-3 px-4 py-3 rounded-lg font-medium hover:bg-[var(--color-bg-elevated)]">
              <Settings className="w-5 h-5" /> Settings
            </NavLink>
            <button onClick={logout} className="flex items-center gap-3 px-4 py-3 rounded-lg font-medium w-full text-left" style={{ color: 'var(--color-danger-500)' }}>
              Sign Out
            </button>
          </div>
        )}
      </header>

      {/* Main Content */}
      <main className="flex-1 px-4 py-6 max-w-7xl mx-auto w-full animate-fade-in">
        <Outlet />
      </main>

      {/* Footer (desktop) */}
      <div className="hidden md:block">
        <Footer />
      </div>

      {/* Bottom Navigation (Mobile) */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 glass-card safe-bottom" style={{ borderRadius: 0, borderBottom: 'none', borderLeft: 'none', borderRight: 'none' }}>
        <div className="flex justify-around py-2">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex flex-col items-center gap-1 px-3 py-1 rounded-lg transition-all touch-target ${
                  isActive ? 'text-[var(--color-brand-400)]' : 'text-[var(--color-text-muted)]'
                }`
              }
            >
              <Icon className="w-5 h-5" />
              <span className="text-[10px] font-medium">{label}</span>
            </NavLink>
          ))}
        </div>
      </nav>

      {/* Bottom nav spacer on mobile */}
      <div className="md:hidden h-20" />

      {/* AI Chat Float Component */}
      <AiChat />
    </div>
  );
}
