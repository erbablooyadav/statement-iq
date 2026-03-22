import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FileText, Calendar, CreditCard, Building, ChevronRight, Loader2 } from 'lucide-react';
import { apiClient } from '../api/client';
import { useAuth } from '../store/AuthContext';

interface StatementItem {
  id: string;
  statementType: string;
  bankName: string;
  statementMonth: string;
  cardLast4?: string;
  accountLast4?: string;
  parseStatus: string;
  transactionCount: number;
  totalDebit: number;
  totalCredit: number;
  uploadedAt: string;
}

export default function StatementList() {
  const { getToken } = useAuth();
  const [statements, setStatements] = useState<StatementItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStatements = async () => {
      try {
        const token = await getToken();
        const res = await apiClient.get('/statements', {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.data.success) setStatements(res.data.data.content || []);
      } catch (err) {
        console.error(err);
      }
      setLoading(false);
    };
    fetchStatements();
  }, [getToken]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--color-brand-500)' }} />
      </div>
    );
  }

  if (statements.length === 0) {
    return (
      <div className="text-center py-16 animate-fade-in">
        <FileText className="w-16 h-16 mx-auto mb-4" style={{ color: 'var(--color-text-muted)' }} />
        <h2 className="text-xl font-bold mb-2" style={{ color: 'var(--color-text-primary)' }}>No statements yet</h2>
        <p className="mb-6" style={{ color: 'var(--color-text-secondary)' }}>Upload your first statement to get started</p>
        <Link to="/upload" className="inline-flex px-6 py-3 rounded-xl gradient-brand text-white font-semibold">
          Upload Statement
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold" style={{ color: 'var(--color-text-primary)' }}>Statements</h1>
        <Link to="/upload" className="px-4 py-2 rounded-lg gradient-brand text-white text-sm font-semibold">
          + Upload
        </Link>
      </div>

      <div className="space-y-3">
        {statements.map((stmt) => (
          <Link key={stmt.id} to={`/statements/${stmt.id}`} className="block glass-card p-4 transition-all hover:scale-[1.01] hover:border-[var(--color-brand-500)]">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: stmt.statementType === 'CC' ? 'rgba(99, 102, 241, 0.15)' : 'rgba(16, 185, 129, 0.15)' }}>
                  {stmt.statementType === 'CC' ? <CreditCard className="w-5 h-5" style={{ color: 'var(--color-brand-400)' }} /> : <Building className="w-5 h-5" style={{ color: 'var(--color-accent-400)' }} />}
                </div>
                <div>
                  <p className="font-semibold text-sm" style={{ color: 'var(--color-text-primary)' }}>
                    {stmt.bankName} {stmt.statementType === 'CC' ? `•• ${stmt.cardLast4 || ''}` : `•• ${stmt.accountLast4 || ''}`}
                  </p>
                  <div className="flex items-center gap-2 text-xs" style={{ color: 'var(--color-text-muted)' }}>
                    <Calendar className="w-3 h-3" />
                    {stmt.statementMonth || 'Processing...'}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <div className="text-right">
                  <p className="text-sm font-bold" style={{ color: 'var(--color-text-primary)' }}>
                    {stmt.transactionCount} txns
                  </p>
                  {stmt.parseStatus === 'COMPLETED' && (
                    <p className="text-xs" style={{ color: 'var(--color-danger-400)' }}>₹{(stmt.totalDebit || 0).toLocaleString()}</p>
                  )}
                  {stmt.parseStatus === 'PROCESSING' && (
                    <span className="text-xs px-2 py-0.5 rounded-full" style={{ background: 'rgba(251, 191, 36, 0.15)', color: 'var(--color-warning-400)' }}>Processing</span>
                  )}
                </div>
                <ChevronRight className="w-5 h-5" style={{ color: 'var(--color-text-muted)' }} />
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
