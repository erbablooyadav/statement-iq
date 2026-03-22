import { useState } from 'react';
import { useAuth } from '../store/AuthContext';
import { Shield, Mail, Lock, User, Eye, EyeOff } from 'lucide-react';

export default function Auth() {
  const { loginWithGoogle, loginWithEmail, signupWithEmail } = useAuth();
  const [isSignup, setIsSignup] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isSignup) {
        await signupWithEmail(email, password, name);
      } else {
        await loginWithEmail(email, password);
      }
    } catch (err: any) {
      setError(err.message?.replace('Firebase: ', '') || 'Authentication failed');
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background: 'var(--color-bg-primary)' }}>
      <div className="w-full max-w-md animate-slide-up">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-2">
            <Shield className="w-10 h-10" style={{ color: 'var(--color-brand-500)' }} />
            <h1 className="text-3xl font-bold gradient-text">StatementIQ</h1>
          </div>
          <p style={{ color: 'var(--color-text-secondary)' }}>
            {isSignup ? 'Create your account' : 'Welcome back'}
          </p>
        </div>

        {/* Auth Card */}
        <div className="glass-card p-8">
          {/* Google Sign In */}
          <button
            onClick={loginWithGoogle}
            className="w-full flex items-center justify-center gap-3 px-6 py-3 rounded-xl font-semibold transition-all hover:scale-[1.02] mb-6"
            style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)' }}
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Continue with Google
          </button>

          <div className="relative mb-6">
            <div className="absolute inset-0 flex items-center"><div className="w-full" style={{ borderTop: '1px solid var(--color-border)' }} /></div>
            <div className="relative flex justify-center text-sm">
              <span className="px-3" style={{ background: 'var(--glass-bg)', color: 'var(--color-text-muted)' }}>or</span>
            </div>
          </div>

          {/* Email Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {isSignup && (
              <div>
                <label className="block text-sm font-medium mb-1.5" style={{ color: 'var(--color-text-secondary)' }}>Full Name</label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5" style={{ color: 'var(--color-text-muted)' }} />
                  <input
                    type="text" value={name} onChange={e => setName(e.target.value)} required
                    className="w-full pl-11 pr-4 py-3 rounded-xl focus:outline-none focus:ring-2"
                    style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)', '--tw-ring-color': 'var(--color-brand-500)' } as any}
                    placeholder="Rahul Sharma"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium mb-1.5" style={{ color: 'var(--color-text-secondary)' }}>Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5" style={{ color: 'var(--color-text-muted)' }} />
                <input
                  type="email" value={email} onChange={e => setEmail(e.target.value)} required
                  className="w-full pl-11 pr-4 py-3 rounded-xl focus:outline-none focus:ring-2"
                  style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)', '--tw-ring-color': 'var(--color-brand-500)' } as any}
                  placeholder="rahul@example.com"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-1.5" style={{ color: 'var(--color-text-secondary)' }}>Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5" style={{ color: 'var(--color-text-muted)' }} />
                <input
                  type={showPassword ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} required
                  className="w-full pl-11 pr-12 py-3 rounded-xl focus:outline-none focus:ring-2"
                  style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)', '--tw-ring-color': 'var(--color-brand-500)' } as any}
                  placeholder="••••••••"
                  minLength={6}
                />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 touch-target" style={{ color: 'var(--color-text-muted)' }}>
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            {error && (
              <p className="text-sm py-2 px-3 rounded-lg" style={{ color: 'var(--color-danger-400)', background: 'rgba(244, 63, 94, 0.1)' }}>
                {error}
              </p>
            )}

            <button
              type="submit" disabled={loading}
              className="w-full py-3 rounded-xl font-semibold text-white gradient-brand transition-all hover:opacity-90 disabled:opacity-50"
            >
              {loading ? 'Please wait...' : isSignup ? 'Create Account' : 'Sign In'}
            </button>
          </form>

          <p className="text-center mt-6 text-sm" style={{ color: 'var(--color-text-muted)' }}>
            {isSignup ? 'Already have an account?' : "Don't have an account?"}
            <button onClick={() => { setIsSignup(!isSignup); setError(''); }} className="ml-1 font-semibold" style={{ color: 'var(--color-brand-400)' }}>
              {isSignup ? 'Sign In' : 'Sign Up'}
            </button>
          </p>
        </div>

        {/* Privacy note */}
        <p className="text-center text-xs mt-6" style={{ color: 'var(--color-text-muted)' }}>
          🔒 Your financial data stays private. We never sell your data.
        </p>
      </div>
    </div>
  );
}
