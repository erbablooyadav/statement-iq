import { Link } from 'react-router-dom';
import { Check, Shield, ArrowLeft, Zap, Star, Key, X } from 'lucide-react';
import Footer from '../components/layout/Footer';

const plans = [
  {
    name: 'Free',
    price: '₹0',
    period: '/forever',
    desc: 'Get started with basic analysis',
    badge: null,
    features: [
      '1 CC + 1 Bank statement/month',
      'Basic spend analysis (regex parsing)',
      'Category-wise breakdown',
      'Hidden charge detection',
      '1 saving goal',
      'Bill due reminders',
    ],
    notIncluded: [
      'AI-powered insights',
      'Smart Swipe optimizer',
      'Monthly report card',
      'Excel export',
    ],
    cta: 'Start Free',
    ctaTo: '/auth',
    ctaStyle: 'border' as const,
  },
  {
    name: 'Pro + BYOK',
    price: '₹79',
    period: '/month',
    desc: 'Use your own AI keys. Save 70%.',
    badge: '💡 Smart Saver',
    features: [
      'Unlimited statement uploads',
      'Full AI insights (your API key)',
      'Claude, Gemini, OpenAI, or Local',
      'Smart Swipe daily card optimizer',
      'Up to 5 saving goals',
      'Monthly financial report card',
      'Export to CSV & Excel',
      'AI financial chatbot',
      'Password-protected PDFs',
    ],
    notIncluded: [],
    cta: 'Choose BYOK',
    ctaTo: '/auth',
    ctaStyle: 'outlined' as const,
  },
  {
    name: 'Pro',
    price: '₹249',
    period: '/month',
    desc: 'Everything included. Zero setup.',
    badge: '⭐ Most Popular',
    features: [
      'Everything in BYOK +',
      'Platform AI keys included — no setup',
      '30 CC + 30 Bank statements/day',
      'Full AI insights (Claude Sonnet)',
      'Smart Swipe daily card optimizer',
      'Up to 5 saving goals',
      'Monthly financial report card',
      'Export to CSV & Excel',
      'AI financial chatbot',
      'Password-protected PDFs',
      'Priority email support',
    ],
    notIncluded: [],
    cta: 'Upgrade to Pro',
    ctaTo: '/auth',
    ctaStyle: 'gradient' as const,
  },
];

const faqs = [
  {
    q: 'What is BYOK (Bring Your Own Key)?',
    a: 'You can use your own AI API keys from Claude, Gemini, OpenAI, or even a local model on your machine. This gives you full AI features at a fraction of the cost — you only pay for your actual API usage (usually pennies per statement).',
  },
  {
    q: 'Why is Pro + BYOK cheaper?',
    a: 'Because you\'re bringing your own AI keys, we save on our biggest cost — AI API calls. We pass that saving directly to you. You get the exact same features as Pro, just at ₹79 instead of ₹249.',
  },
  {
    q: 'What does the ₹249/month cover?',
    a: 'Server hosting, MongoDB database, CDN, email notifications,24/7 uptime monitoring, feature development, security patches, and of course — the AI API costs for Claude Sonnet on every analysis. StatementIQ Pro pays for itself if you catch even one hidden charge.',
  },
  {
    q: 'Can I use a local AI model (100% private)?',
    a: 'Yes! Choose "Local Model" in Settings → AI Provider. Install Ollama, pull a model like llama3, and your statement data never leaves your computer. Completely private.',
  },
  {
    q: 'Is my financial data safe?',
    a: 'Absolutely. PDFs are processed in memory and never stored. No bank login needed. Encryption in transit and at rest. DPDP Act compliant. Export or delete your data anytime.',
  },
  {
    q: 'Can I cancel anytime?',
    a: 'Yes, cancel anytime — no questions asked. You keep Pro features until the end of your billing period.',
  },
];

export default function Pricing() {
  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--color-bg-primary)' }}>
      {/* Header */}
      <header className="sticky top-0 z-50 glass-card" style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
        <div className="flex items-center justify-between px-4 py-3 max-w-7xl mx-auto">
          <Link to="/" className="flex items-center gap-2">
            <Shield className="w-6 h-6" style={{ color: 'var(--color-brand-500)' }} />
            <span className="text-lg font-bold gradient-text">StatementIQ</span>
          </Link>
          <div className="flex items-center gap-3">
            <Link to="/" className="text-sm font-medium hover:opacity-80 transition-all" style={{ color: 'var(--color-text-secondary)' }}>
              <ArrowLeft className="w-4 h-4 inline mr-1" /> Home
            </Link>
            <Link to="/auth" className="px-4 py-2 rounded-xl text-sm font-semibold gradient-brand text-white">
              Get Started
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1 px-4 py-12">
        <div className="max-w-5xl mx-auto">
          {/* Title */}
          <div className="text-center mb-12">
            <h1 className="text-3xl md:text-5xl font-extrabold mb-4" style={{ color: 'var(--color-text-primary)' }}>
              Pay only for what you need
            </h1>
            <p className="text-lg max-w-xl mx-auto" style={{ color: 'var(--color-text-secondary)' }}>
              Bring your own AI keys and save 70%. Or let us handle everything.
            </p>
          </div>

          {/* Plans Grid */}
          <div className="grid md:grid-cols-3 gap-6 mb-16">
            {plans.map((plan) => (
              <div
                key={plan.name}
                className="glass-card p-6 relative flex flex-col"
                style={plan.ctaStyle === 'gradient' ? { border: '2px solid var(--color-brand-500)' } : {}}
              >
                {plan.badge && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-4 py-1 rounded-full gradient-brand text-white text-xs font-semibold whitespace-nowrap">
                    {plan.badge}
                  </div>
                )}

                <div className="mb-6">
                  <h2 className="text-lg font-bold" style={{ color: 'var(--color-text-primary)' }}>{plan.name}</h2>
                  <div className="flex items-baseline gap-1 mt-2">
                    <span className="text-4xl font-extrabold" style={{ color: 'var(--color-text-primary)' }}>{plan.price}</span>
                    <span className="text-sm" style={{ color: 'var(--color-text-muted)' }}>{plan.period}</span>
                  </div>
                  <p className="text-sm mt-1" style={{ color: 'var(--color-text-muted)' }}>{plan.desc}</p>
                </div>

                <ul className="space-y-3 mb-4 flex-1">
                  {plan.features.map((feature) => (
                    <li key={feature} className="flex items-start gap-2">
                      <Check className="w-4 h-4 flex-shrink-0 mt-0.5" style={{ color: 'var(--color-accent-500)' }} />
                      <span className="text-sm" style={{ color: 'var(--color-text-primary)' }}>{feature}</span>
                    </li>
                  ))}
                  {plan.notIncluded.map((feature) => (
                    <li key={feature} className="flex items-start gap-2 opacity-40">
                      <X className="w-4 h-4 flex-shrink-0 mt-0.5" />
                      <span className="text-sm line-through">{feature}</span>
                    </li>
                  ))}
                </ul>

                <Link
                  to={plan.ctaTo}
                  className={`block w-full py-3 rounded-xl font-semibold text-center transition-all mt-auto ${
                    plan.ctaStyle === 'gradient' ? 'gradient-brand text-white hover:opacity-90' : ''
                  }`}
                  style={plan.ctaStyle !== 'gradient' ? { border: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' } : {}}
                >
                  {plan.cta} {plan.ctaStyle === 'gradient' && <Zap className="w-4 h-4 inline ml-1" />}
                </Link>
              </div>
            ))}
          </div>

          {/* Value Proposition */}
          <div className="glass-card p-8 text-center mb-16">
            <h2 className="text-xl font-bold mb-2">💡 StatementIQ Pro pays for itself</h2>
            <p className="text-sm max-w-lg mx-auto" style={{ color: 'var(--color-text-secondary)' }}>
              Indian banks charge ₹200–₹500 in hidden fees per statement on average. Our AI catches what you miss.
              If we help you save just ₹300/month, <strong>Pro is free</strong>.
            </p>
          </div>

          {/* FAQ */}
          <div>
            <h2 className="text-2xl font-bold text-center mb-8" style={{ color: 'var(--color-text-primary)' }}>Common Questions</h2>
            <div className="grid md:grid-cols-2 gap-4 max-w-3xl mx-auto">
              {faqs.map(({ q, a }) => (
                <div key={q} className="glass-card p-5">
                  <p className="font-semibold text-sm mb-2" style={{ color: 'var(--color-text-primary)' }}>{q}</p>
                  <p className="text-xs leading-relaxed" style={{ color: 'var(--color-text-muted)' }}>{a}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
