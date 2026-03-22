import { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { useAuth } from '../store/AuthContext';
import {
  Bell, AlertTriangle, CreditCard, Calendar, Shield, Sparkles, CheckCircle, Check, Loader2
} from 'lucide-react';

interface Alert {
  id: string;
  type: 'HIDDEN_CHARGE' | 'OVERSPEND' | 'BILL_DUE' | 'REWARD_LEAKAGE' | 'SECURITY' | 'SAVING_OPPORTUNITY';
  title: string;
  description: string;
  amount?: number;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  read: boolean;
  createdAt: string;
}

export default function Alerts() {
  const { getToken } = useAuth();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    try {
      const token = await getToken();
      const res = await apiClient.get('/alerts', {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        setAlerts(res.data.data);
      }
    } catch (e) {
      console.error('Failed to fetch alerts', e);
    }
    setLoading(false);
  };

  const markAsRead = async (id: string) => {
    try {
      const token = await getToken();
      await apiClient.patch(`/alerts/${id}/read`, {}, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setAlerts(prev => prev.map(a => a.id === id ? { ...a, read: true } : a));
    } catch (e) {
      console.error('Failed to mark read', e);
    }
  };

  const getAlertIcon = (type: string, severity: string) => {
    let color = 'var(--color-brand-400)';
    if (severity === 'HIGH') color = '#ef4444'; // red
    else if (severity === 'MEDIUM') color = '#f59e0b'; // amber

    switch (type) {
      case 'HIDDEN_CHARGE': return <AlertTriangle className="w-5 h-5" style={{ color }} />;
      case 'BILL_DUE': return <Calendar className="w-5 h-5" style={{ color }} />;
      case 'OVERSPEND': return <AlertTriangle className="w-5 h-5" style={{ color }} />;
      case 'REWARD_LEAKAGE': return <CreditCard className="w-5 h-5" style={{ color }} />;
      case 'SECURITY': return <Shield className="w-5 h-5" style={{ color }} />;
      case 'SAVING_OPPORTUNITY': return <Sparkles className="w-5 h-5" style={{ color: '#10b981' }} />;
      default: return <Bell className="w-5 h-5" style={{ color }} />;
    }
  };

  const formatCurrency = (amount?: number) => {
    if (!amount) return null;
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--color-brand-400)' }} />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold flex items-center gap-2" style={{ color: 'var(--color-text-primary)' }}>
          <Bell className="w-6 h-6" style={{ color: 'var(--color-accent-400)' }} /> 
          Alerts & Insights
        </h1>
      </div>

      {alerts.length === 0 ? (
        <div className="glass-card p-12 text-center animate-slide-up">
          <div className="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center" style={{ background: 'var(--color-bg-elevated)' }}>
            <CheckCircle className="w-10 h-10" style={{ color: 'var(--color-brand-400)' }} />
          </div>
          <h2 className="text-xl font-bold mb-2" style={{ color: 'var(--color-text-primary)' }}>You're all caught up!</h2>
          <p className="text-sm max-w-sm mx-auto" style={{ color: 'var(--color-text-secondary)' }}>
            Upload statements regularly so our AI can catch hidden charges and notify you before bills are due.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {alerts.map(alert => (
            <div key={alert.id} className={`glass-card p-5 transition-all outline outline-1 flex gap-4 ${alert.read ? 'opacity-70 outline-transparent' : 'outline-[var(--color-border)] shadow-md shadow-brand-500/10'}`}>
              
              <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style={{ background: 'var(--color-bg-elevated)' }}>
                {getAlertIcon(alert.type, alert.severity)}
              </div>

              <div className="flex-1">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-bold text-base" style={{ color: 'var(--color-text-primary)' }}>
                      {alert.title}
                      {!alert.read && <span className="ml-2 inline-block w-2 h-2 rounded-full" style={{ background: 'var(--color-brand-500)' }} />}
                    </h3>
                    <p className="text-sm mt-1" style={{ color: 'var(--color-text-secondary)' }}>{alert.description}</p>
                  </div>
                  {alert.amount && (
                    <div className="text-right font-bold text-lg whitespace-nowrap" style={{ color: alert.type === 'SAVING_OPPORTUNITY' ? '#10b981' : '#ef4444' }}>
                      {alert.type === 'SAVING_OPPORTUNITY' ? '+' : '-'}{formatCurrency(alert.amount)}
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-between mt-4 border-t pt-3" style={{ borderColor: 'var(--color-border)' }}>
                  <span className="text-xs font-semibold uppercase" style={{ color: 'var(--color-text-muted)' }}>
                    {new Date(alert.createdAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </span>
                  {!alert.read && (
                    <button 
                      onClick={() => markAsRead(alert.id)}
                      className="text-xs font-bold px-3 py-1.5 rounded-lg flex items-center gap-1 hover:bg-[var(--color-bg-elevated)] transition-colors"
                      style={{ color: 'var(--color-brand-400)' }}
                    >
                      <Check className="w-3.5 h-3.5" /> Mark Read
                    </button>
                  )}
                </div>
              </div>

            </div>
          ))}
        </div>
      )}
    </div>
  );
}
