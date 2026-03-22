import { useState, useEffect } from 'react';
import { useAuth } from '../store/AuthContext';
import { apiClient } from '../api/client';
import {
  TrendingUp, TrendingDown, CreditCard, Target, Bell,
  Upload, ArrowRight, IndianRupee, PiggyBank, Wallet, Loader2
} from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  const { user, getToken } = useAuth();
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState({
    hasStatements: false,
    totalIncome: 0,
    totalSpent: 0,
    totalSaved: 0,
    unreadAlerts: 0
  });

  useEffect(() => {
    fetchSummary();
  }, []);

  const fetchSummary = async () => {
    try {
      const token = await getToken();
      const res = await apiClient.get('/dashboard/summary', {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.data.success) {
        setSummary(res.data.data);
      }
    } catch (e) {
      console.error('Failed to fetch dashboard summary', e);
    }
    setLoading(false);
  };

  const formatCurrency = (n: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    }).format(n);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--color-brand-400)' }} />
      </div>
    );
  }

  if (!summary.hasStatements) {
    return <EmptyDashboard userName={user?.displayName || 'there'} />;
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Welcome Header */}
      <div>
        <h1 className="text-2xl font-bold" style={{ color: 'var(--color-text-primary)' }}>
          Good evening, {user?.displayName?.split(' ')[0]} 👋
        </h1>
        <p className="text-sm mt-1" style={{ color: 'var(--color-text-secondary)' }}>
          Here's your financial snapshot
        </p>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard icon={IndianRupee} label="Income" value={formatCurrency(summary.totalIncome)} />
        <StatCard icon={Wallet} label="Spent" value={formatCurrency(summary.totalSpent)} />
        <StatCard icon={PiggyBank} label="Saved" value={formatCurrency(summary.totalSaved)} positive />
        
        <Link to="/alerts" className="block outline-none hover:scale-105 transition-transform">
          <StatCard icon={Bell} label="Alerts" value={summary.unreadAlerts.toString()} alertMode={summary.unreadAlerts > 0} />
        </Link>
      </div>

      {/* Smart Swipe Widget */}
      <div className="glass-card p-6">
        <div className="flex items-center gap-2 mb-4">
          <CreditCard className="w-5 h-5" style={{ color: 'var(--color-brand-400)' }} />
          <h2 className="font-bold">Smart Swipe — Today's Best Cards</h2>
        </div>
        <div className="space-y-3">
          <CardRecommendation category="Groceries" card="ICICI Amazon Pay" benefit="5% cashback" saving="₹100" />
          <CardRecommendation category="Food Delivery" card="Axis Ace" benefit="5% cashback" saving="₹25" />
          <CardRecommendation category="Travel" card="HDFC Regalia" benefit="4X points" saving="₹200" />
        </div>
      </div>

      {/* Goal Progress */}
      <div className="glass-card p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Target className="w-5 h-5" style={{ color: 'var(--color-accent-500)' }} />
            <h2 className="font-bold">Goal Progress</h2>
          </div>
          <Link to="/goals" className="text-sm font-medium flex items-center gap-1" style={{ color: 'var(--color-brand-400)' }}>
            View all <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
        <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>
          Keep track of your active savings goals!
        </p>
      </div>
    </div>
  );
}

function EmptyDashboard({ userName }: { userName: string }) {
  return (
    <div className="space-y-8 animate-slide-up">
      <div className="text-center pt-8">
        <h1 className="text-3xl font-bold mb-2" style={{ color: 'var(--color-text-primary)' }}>
          Welcome, {userName.split(' ')[0]}! 👋
        </h1>
        <p className="text-lg" style={{ color: 'var(--color-text-secondary)' }}>
          Let's analyze your first statement
        </p>
      </div>

      {/* Upload CTA */}
      <Link to="/upload" className="block">
        <div className="glass-card p-8 text-center transition-all hover:scale-[1.02] hover:border-[var(--color-brand-500)]" style={{ border: '2px dashed var(--color-border)' }}>
          <div className="w-16 h-16 rounded-2xl gradient-brand flex items-center justify-center mx-auto mb-4 animate-float">
            <Upload className="w-8 h-8 text-white" />
          </div>
          <h2 className="text-xl font-bold mb-2">Upload Bank Statement</h2>
          <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>
            Drag & drop or click to upload your PDF statement.<br />Our AI will automatically categorize transactions and find hidden charges.
          </p>
          <button className="mt-6 px-6 py-2.5 rounded-xl text-sm font-semibold gradient-brand text-white">
            Upload PDF
          </button>
        </div>
      </Link>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, trend, positive, alertMode }: any) {
  return (
    <div className={`glass-card p-5 ${alertMode ? 'ring-2 ring-red-500/50 bg-red-500/5' : ''}`}>
      <div className="flex items-center gap-2 mb-3">
        <div className="p-2 rounded-lg" style={{ background: alertMode ? '#ef444420' : 'var(--color-bg-elevated)' }}>
          <Icon className="w-4 h-4" style={{ color: alertMode ? '#ef4444' : 'var(--color-text-secondary)' }} />
        </div>
        <span className="text-sm font-medium" style={{ color: 'var(--color-text-secondary)' }}>{label}</span>
      </div>
      <p className="text-xl font-bold mb-1" style={{ color: 'var(--color-text-primary)' }}>{value}</p>
      {trend && (
        <p className="text-xs font-medium flex items-center gap-1" style={{ color: positive ? '#10b981' : '#ef4444' }}>
          {positive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
          {trend}
        </p>
      )}
    </div>
  );
}

function CardRecommendation({ category, card, benefit, saving }: any) {
  return (
    <div className="flex items-center justify-between p-3 rounded-xl border" style={{ borderColor: 'var(--color-border)', background: 'var(--color-bg-elevated)' }}>
      <div className="flex items-center gap-3">
        <div className="p-2 mr-1 rounded-lg bg-[var(--color-bg-elevated)] border border-[var(--color-border)]">
           <CreditCard className="w-4 h-4 text-emerald-500" />
        </div>
        <div>
          <p className="text-sm font-semibold" style={{ color: 'var(--color-text-primary)' }}>{category}</p>
          <p className="text-xs font-semibold" style={{ color: 'var(--color-brand-400)' }}>{card}</p>
        </div>
      </div>
      <div className="text-right">
        <p className="text-sm font-bold text-emerald-400">{benefit}</p>
        <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>Save {saving}</p>
      </div>
    </div>
  );
}
