import { Link } from 'react-router-dom';
import { Shield, BarChart3, Bell, CreditCard, Target, ArrowRight, Lock, Zap, Eye } from 'lucide-react';

const features = [
  { icon: BarChart3, title: 'Analyze', desc: 'CC + Bank statement parsing. Full 360° money picture.', color: '#6366f1' },
  { icon: Bell, title: 'Alert', desc: 'Hidden charge detection. Bill due radar. Overspend warnings.', color: '#f59e0b' },
  { icon: CreditCard, title: 'Recommend', desc: 'Smart Swipe — which card to use today for max rewards.', color: '#10b981' },
  { icon: Target, title: 'Goals', desc: 'AI savings planner with daily check-ins and streaks.', color: '#f43f5e' },
];

const privacyFeatures = [
  { icon: Lock, text: 'Your PDF is deleted from memory immediately after processing' },
  { icon: Shield, text: 'Zero bank login or SMS access required' },
  { icon: Eye, text: 'No transaction data stored in application logs' },
  { icon: Zap, text: 'End-to-end encryption in transit and at rest' },
];

export default function Landing() {
  return (
    <div className="min-h-screen" style={{ background: 'var(--color-bg-primary)' }}>
      {/* Nav */}
      <nav className="flex items-center justify-between px-6 py-4 max-w-7xl mx-auto">
        <Link to="/" className="flex items-center gap-2">
          <Shield className="w-8 h-8" style={{ color: 'var(--color-brand-500)' }} />
          <span className="text-xl font-bold gradient-text">StatementIQ</span>
        </Link>
        <div className="flex gap-3">
          <Link to="/pricing" className="px-4 py-2 text-sm font-medium rounded-lg transition-all hover:bg-[var(--color-bg-elevated)]" style={{ color: 'var(--color-text-secondary)' }}>
            Pricing
          </Link>
          <Link to="/auth" className="px-5 py-2 text-sm font-semibold rounded-lg gradient-brand text-white transition-all hover:opacity-90">
            Get Started
          </Link>
        </div>
      </nav>

      {/* Hero */}
      <section className="px-6 pt-16 pb-20 max-w-7xl mx-auto text-center gradient-hero rounded-3xl mx-4 mt-4">
        <div className="animate-slide-up">
          <div className="inline-flex items-center gap-2 mb-6 px-4 py-2 rounded-full text-sm font-medium" style={{ background: 'rgba(99, 102, 241, 0.15)', color: 'var(--color-brand-400)' }}>
            <Lock className="w-4 h-4" />
            Privacy-first • No bank login needed
          </div>

          <h1 className="text-4xl md:text-6xl font-extrabold mb-6 leading-tight" style={{ color: 'var(--color-text-primary)' }}>
            Know your money<br />
            <span className="gradient-text">Beat your bank</span><br />
            Reach your goals
          </h1>

          <p className="text-lg md:text-xl mb-10 max-w-2xl mx-auto" style={{ color: 'var(--color-text-secondary)' }}>
            Upload your CC or bank statement PDF. Our AI analyzes every transaction, hunts hidden charges, recommends the right card for every spend, and helps you save towards your dreams.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/auth" className="inline-flex items-center justify-center gap-2 px-8 py-4 text-lg font-semibold rounded-2xl gradient-brand text-white transition-all hover:opacity-90 animate-pulse-glow">
              Start Free
              <ArrowRight className="w-5 h-5" />
            </Link>
            <Link to="/pricing" className="inline-flex items-center justify-center gap-2 px-8 py-4 text-lg font-semibold rounded-2xl border transition-all hover:bg-[var(--color-bg-elevated)]" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-primary)' }}>
              View Pricing
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="px-6 py-20 max-w-7xl mx-auto">
        <h2 className="text-3xl font-bold text-center mb-4 gradient-text">Four Pillars of Financial Intelligence</h2>
        <p className="text-center mb-12 max-w-xl mx-auto" style={{ color: 'var(--color-text-secondary)' }}>
          Everything you need to understand, control, and grow your money — in one app.
        </p>

        <div className="grid md:grid-cols-2 gap-6">
          {features.map(({ icon: Icon, title, desc, color }) => (
            <div key={title} className="glass-card p-6 transition-all hover:scale-[1.02] hover:border-[var(--color-brand-500)]">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style={{ background: `${color}15` }}>
                <Icon className="w-6 h-6" style={{ color }} />
              </div>
              <h3 className="text-xl font-bold mb-2" style={{ color: 'var(--color-text-primary)' }}>{title}</h3>
              <p style={{ color: 'var(--color-text-secondary)' }}>{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Privacy */}
      <section className="px-6 py-20 max-w-7xl mx-auto">
        <div className="glass-card p-8 md:p-12">
          <h2 className="text-3xl font-bold mb-2" style={{ color: 'var(--color-text-primary)' }}>
            🔒 Privacy First, Always.
          </h2>
          <p className="mb-8" style={{ color: 'var(--color-text-secondary)' }}>
            We deal with your most sensitive financial data. Here's how we protect it.
          </p>
          <div className="grid md:grid-cols-2 gap-4">
            {privacyFeatures.map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-start gap-3 p-4 rounded-xl" style={{ background: 'var(--color-bg-elevated)' }}>
                <Icon className="w-5 h-5 mt-0.5 flex-shrink-0" style={{ color: 'var(--color-accent-500)' }} />
                <span className="text-sm" style={{ color: 'var(--color-text-primary)' }}>{text}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="px-6 py-20 text-center">
        <h2 className="text-3xl font-bold mb-4 gradient-text">Ready to take control of your money?</h2>
        <p className="mb-8" style={{ color: 'var(--color-text-secondary)' }}>
          Free plan includes 1 CC + 1 Bank statement analysis per month. No credit card required.
        </p>
        <Link to="/auth" className="inline-flex items-center gap-2 px-10 py-4 text-lg font-semibold rounded-2xl gradient-brand text-white transition-all hover:opacity-90">
          Get Started Free <ArrowRight className="w-5 h-5" />
        </Link>
      </section>

      {/* Footer */}
      <footer className="px-6 py-8 text-center text-sm" style={{ color: 'var(--color-text-muted)', borderTop: '1px solid var(--color-border)' }}>
        <p>© 2025 StatementIQ. Built with ❤️ in India.</p>
        <p className="mt-1">Your data. Your control. Always.</p>
      </footer>
    </div>
  );
}
