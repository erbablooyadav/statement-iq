import { useState, useEffect } from 'react';
import { Download, X, Shield, Zap, WifiOff, Smartphone, Lock, Sparkles, Monitor, Chrome } from 'lucide-react';

const PWA_DISMISS_KEY = 'statementiq_pwa_dismissed_at';
const DISMISS_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

const installBenefits = [
  { icon: Zap, title: 'Instant Launch', desc: 'Opens in under 1 second — no browser tabs' },
  { icon: WifiOff, title: 'Works Offline', desc: 'View your data even without internet' },
  { icon: Lock, title: 'Privacy-First', desc: 'No app store tracking or permissions bloat' },
  { icon: Smartphone, title: 'Feels Native', desc: 'Full-screen, home screen icon, push notifications' },
];

function getManualInstallInstructions(): { browser: string; steps: string[] } {
  const ua = navigator.userAgent;
  if (/CriOS/i.test(ua)) return { browser: 'Chrome (iOS)', steps: ['Tap the Share button (box with arrow)', 'Scroll down and tap "Add to Home Screen"', 'Tap "Add"'] };
  if (/Safari/i.test(ua) && /iPhone|iPad/i.test(ua)) return { browser: 'Safari (iOS)', steps: ['Tap the Share button (box with arrow) at the bottom', 'Scroll down and tap "Add to Home Screen"', 'Tap "Add"'] };
  if (/Firefox/i.test(ua)) return { browser: 'Firefox', steps: ['Click the ⋯ menu button in the address bar', 'Select "Install" or "Add to Home Screen"'] };
  if (/Edg/i.test(ua)) return { browser: 'Microsoft Edge', steps: ['Click the ⋯ menu in the top right', 'Click "Apps" → "Install this site as an app"'] };
  if (/Chrome/i.test(ua)) return { browser: 'Google Chrome', steps: ['Click the install icon (⊕) in the address bar', 'Or: Menu (⋮) → "Install StatementIQ"'] };
  return { browser: 'your browser', steps: ['Look for "Install" or "Add to Home Screen" in your browser menu'] };
}

export default function PwaInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showBanner, setShowBanner] = useState(false);
  const [showDetails, setShowDetails] = useState(false);
  const [isStandalone, setIsStandalone] = useState(false);
  const [isDismissed, setIsDismissed] = useState(false);

  useEffect(() => {
    // Don't show if already installed as PWA
    if (window.matchMedia('(display-mode: standalone)').matches ||
        (window.navigator as any).standalone === true) {
      setIsStandalone(true);
      return;
    }

    // Check 7-day dismissal
    const dismissedAt = localStorage.getItem(PWA_DISMISS_KEY);
    if (dismissedAt && Date.now() - parseInt(dismissedAt) < DISMISS_DURATION_MS) {
      setIsDismissed(true);
      return;
    }

    // Show banner even if beforeinstallprompt hasn't fired (for all browsers)
    setTimeout(() => setShowBanner(true), 800);

    const handleBeforeInstallPrompt = (e: any) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
  }, []);

  const handleInstall = async () => {
    if (deferredPrompt) {
      // Native prompt available (Chrome/Edge)
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === 'accepted') {
        setShowBanner(false);
        setDeferredPrompt(null);
      }
    } else {
      // Show manual instructions modal
      setShowDetails(true);
    }
  };

  const handleDismiss = () => {
    localStorage.setItem(PWA_DISMISS_KEY, Date.now().toString());
    setShowBanner(false);
    setIsDismissed(true);
  };

  // Don't render if already installed or recently dismissed
  if (isStandalone || isDismissed) return null;

  const manualInstructions = getManualInstallInstructions();

  return (
    <>
      {/* Top Install Banner */}
      <div
        id="pwa-install-banner"
        style={{
          background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #6366f1 100%)',
          transform: showBanner ? 'translateY(0)' : 'translateY(-100%)',
          opacity: showBanner ? 1 : 0,
          transition: 'transform 0.5s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.4s ease',
          position: 'relative',
          overflow: 'hidden',
          zIndex: 9999,
        }}
        className="text-white"
      >
        {/* Shimmer */}
        <div style={{
          position: 'absolute', inset: 0,
          background: 'linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.08) 50%, transparent 100%)',
          animation: 'shimmer 3s ease-in-out infinite',
        }} />

        <div className="relative z-10 max-w-7xl mx-auto px-4 py-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center"
              style={{ background: 'rgba(255,255,255,0.15)', backdropFilter: 'blur(8px)' }}>
              <Shield className="w-5 h-5 text-white" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <p className="font-bold text-sm">Install StatementIQ</p>
                <Sparkles className="w-3.5 h-3.5 text-yellow-300 flex-shrink-0" />
              </div>
              <p className="text-xs text-white/80 truncate">
                No app store needed · Instant, lightweight & private
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-shrink-0">
            <button
              onClick={() => setShowDetails(true)}
              className="hidden sm:flex items-center text-xs text-white/90 underline underline-offset-2 hover:text-white transition-colors"
            >
              Why install?
            </button>
            <button
              onClick={handleInstall}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-bold transition-all hover:scale-105 active:scale-95"
              style={{
                background: 'white',
                color: '#4f46e5',
                boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
              }}
            >
              <Download className="w-4 h-4" />
              <span className="hidden sm:inline">Install Now</span>
              <span className="sm:hidden">Install</span>
            </button>
            <button
              onClick={handleDismiss}
              className="p-1.5 rounded-lg transition-colors hover:bg-white/20"
              aria-label="Dismiss install banner"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* "Why Install?" / Manual Instructions Modal */}
      {showDetails && (
        <div
          className="fixed inset-0 z-[10000] flex items-center justify-center p-4"
          style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowDetails(false)}
        >
          <div
            className="w-full max-w-md rounded-2xl p-6 space-y-5 max-h-[90vh] overflow-y-auto"
            style={{
              background: 'var(--color-bg-secondary, #1e293b)',
              border: '1px solid var(--color-border, #334155)',
              boxShadow: '0 25px 50px rgba(0,0,0,0.4)',
              animation: 'slide-up 0.3s ease-out',
            }}
            onClick={e => e.stopPropagation()}
          >
            {/* Header */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl flex items-center justify-center"
                  style={{ background: 'linear-gradient(135deg, #4f46e5, #7c3aed)' }}>
                  <Shield className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h2 className="text-lg font-bold" style={{ color: 'var(--color-text-primary, #f8fafc)' }}>
                    Install StatementIQ
                  </h2>
                  <p className="text-xs" style={{ color: 'var(--color-text-muted, #64748b)' }}>
                    Better than an app store download
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowDetails(false)}
                className="p-2 rounded-lg hover:bg-white/10 transition-colors"
              >
                <X className="w-5 h-5" style={{ color: 'var(--color-text-secondary, #94a3b8)' }} />
              </button>
            </div>

            {/* Benefits */}
            <div className="space-y-3">
              {installBenefits.map(({ icon: Icon, title, desc }) => (
                <div key={title} className="flex items-start gap-3 p-3 rounded-xl"
                  style={{ background: 'var(--color-bg-elevated, #334155)' }}>
                  <div className="flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center"
                    style={{ background: 'linear-gradient(135deg, #4f46e5, #7c3aed)' }}>
                    <Icon className="w-4 h-4 text-white" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold" style={{ color: 'var(--color-text-primary, #f8fafc)' }}>
                      {title}
                    </p>
                    <p className="text-xs mt-0.5" style={{ color: 'var(--color-text-secondary, #94a3b8)' }}>
                      {desc}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* vs App Store */}
            <div className="rounded-xl p-4" style={{ background: 'rgba(79, 70, 229, 0.1)', border: '1px solid rgba(79, 70, 229, 0.2)' }}>
              <p className="text-xs font-semibold mb-2" style={{ color: '#818cf8' }}>
                💡 Why not a Play Store / App Store app?
              </p>
              <ul className="text-xs space-y-1" style={{ color: 'var(--color-text-secondary, #94a3b8)' }}>
                <li>• <strong>Zero download size</strong> — already loaded, just add to home screen</li>
                <li>• <strong>Always up-to-date</strong> — no manual updates from the store</li>
                <li>• <strong>No permissions required</strong> — no access to contacts, gallery, etc.</li>
                <li>• <strong>Cross-platform</strong> — works on Android, iOS, Windows, Mac, Linux</li>
                <li>• <strong>Uninstall anytime</strong> — leaves no trace on your device</li>
              </ul>
            </div>

            {/* Install CTA or Manual Instructions */}
            {deferredPrompt ? (
              <button
                onClick={() => { setShowDetails(false); handleInstall(); }}
                className="w-full flex items-center justify-center gap-2 py-3 rounded-xl text-white font-bold transition-all hover:scale-[1.02] active:scale-[0.98]"
                style={{
                  background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                  boxShadow: '0 4px 15px rgba(79, 70, 229, 0.4)',
                }}
              >
                <Download className="w-5 h-5" />
                Install StatementIQ
              </button>
            ) : (
              <div className="rounded-xl p-4" style={{ background: 'var(--color-bg-elevated, #334155)' }}>
                <div className="flex items-center gap-2 mb-3">
                  <Monitor className="w-4 h-4" style={{ color: '#818cf8' }} />
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-text-primary, #f8fafc)' }}>
                    How to install on {manualInstructions.browser}
                  </p>
                </div>
                <ol className="text-xs space-y-2" style={{ color: 'var(--color-text-secondary, #94a3b8)' }}>
                  {manualInstructions.steps.map((step, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold"
                        style={{ background: 'rgba(79, 70, 229, 0.3)', color: '#a5b4fc' }}>
                        {i + 1}
                      </span>
                      <span>{step}</span>
                    </li>
                  ))}
                </ol>
              </div>
            )}

            <p className="text-center text-[10px]" style={{ color: 'var(--color-text-muted, #64748b)' }}>
              Takes less than 3 seconds · No app store account needed
            </p>
          </div>
        </div>
      )}
    </>
  );
}
