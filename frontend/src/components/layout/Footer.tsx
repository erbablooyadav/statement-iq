import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="w-full py-6 px-4 mt-auto" style={{ borderTop: '1px solid var(--color-border)' }}>
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4 text-sm" style={{ color: 'var(--color-text-muted)' }}>
        <p>© 2026 StatementIQ. Built with ❤️ in India</p>
        <div className="flex items-center gap-4">
          <Link to="/" className="hover:underline transition-all" style={{ color: 'var(--color-text-muted)' }}>
            Home
          </Link>
          <span>·</span>
          <Link to="/pricing" className="hover:underline transition-all" style={{ color: 'var(--color-text-muted)' }}>
            Pricing
          </Link>
          <span>·</span>
          <a href="#" className="hover:underline transition-all" style={{ color: 'var(--color-text-muted)' }}>
            Privacy
          </a>
          <span>·</span>
          <a href="#" className="hover:underline transition-all" style={{ color: 'var(--color-text-muted)' }}>
            Terms
          </a>
        </div>
        <p className="text-xs">Your data. Your control. Always.</p>
      </div>
    </footer>
  );
}
