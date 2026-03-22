import { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import {
  Target, Plus, Flame, TrendingUp, X, Calendar,
  Trophy, ChevronRight, Loader2, Sparkles
} from 'lucide-react';

interface Goal {
  id: string;
  name: string;
  emoji: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  streak: number;
  longestStreak: number;
  status: 'ACTIVE' | 'COMPLETED' | 'PAUSED';
  createdAt: string;
}

const TEMPLATES = [
  { emoji: '✈️', name: 'Travel Fund', amount: 50000, desc: 'Your next dream vacation' },
  { emoji: '🏠', name: 'Home Down Payment', amount: 500000, desc: 'Future home ownership' },
  { emoji: '📱', name: 'New Gadget', amount: 30000, desc: 'That shiny new device' },
  { emoji: '🎓', name: 'Education Fund', amount: 100000, desc: 'Invest in learning' },
  { emoji: '🚗', name: 'New Car Fund', amount: 300000, desc: 'Drive your dream' },
  { emoji: '💰', name: 'Emergency Fund', amount: 100000, desc: '3-6 months of expenses' },
];

export default function Goals() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showCheckIn, setShowCheckIn] = useState<string | null>(null);
  const [checkInAmount, setCheckInAmount] = useState('');
  const [checkInNote, setCheckInNote] = useState('');
  const [saving, setSaving] = useState(false);

  // Create form
  const [newGoal, setNewGoal] = useState({ emoji: '🎯', name: '', targetAmount: '', targetDate: '' });

  useEffect(() => { loadGoals(); }, []);

  const loadGoals = async () => {
    try {
      const res = await apiClient.get('/goals');
      if (res.data.success) setGoals(res.data.data);
    } catch (e) { console.error('Failed to load goals', e); }
    setLoading(false);
  };

  const createGoal = async () => {
    if (!newGoal.name || !newGoal.targetAmount) return;
    setSaving(true);
    try {
      await apiClient.post('/goals', {
        name: newGoal.name,
        emoji: newGoal.emoji,
        targetAmount: parseFloat(newGoal.targetAmount),
        targetDate: newGoal.targetDate || null,
      });
      setShowCreate(false);
      setNewGoal({ emoji: '🎯', name: '', targetAmount: '', targetDate: '' });
      loadGoals();
    } catch (e: any) {
      alert(e.response?.data?.error?.message || 'Failed to create goal');
    }
    setSaving(false);
  };

  const doCheckIn = async (goalId: string) => {
    if (!checkInAmount) return;
    setSaving(true);
    try {
      await apiClient.post(`/goals/${goalId}/checkins`, {
        amount: parseFloat(checkInAmount),
        note: checkInNote,
      });
      setShowCheckIn(null);
      setCheckInAmount('');
      setCheckInNote('');
      loadGoals();
    } catch (e: any) {
      alert(e.response?.data?.error?.message || 'Check-in failed');
    }
    setSaving(false);
  };

  const deleteGoal = async (goalId: string) => {
    if (!confirm('Delete this goal and all check-ins?')) return;
    try {
      await apiClient.delete(`/goals/${goalId}`);
      loadGoals();
    } catch (e) { alert('Failed to delete'); }
  };

  const getProgress = (goal: Goal) => {
    if (goal.targetAmount <= 0) return 0;
    return Math.min(100, (goal.currentAmount / goal.targetAmount) * 100);
  };

  const formatCurrency = (n: number) => '₹' + n.toLocaleString('en-IN');

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--color-brand-400)' }} />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto animate-slide-up">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Target className="w-6 h-6" style={{ color: 'var(--color-accent-500)' }} /> Goals
        </h1>
        <button onClick={() => setShowCreate(true)} className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold gradient-brand text-white">
          <Plus className="w-4 h-4" /> New Goal
        </button>
      </div>

      {/* Goal Cards */}
      {goals.length === 0 ? (
        <div className="text-center py-16">
          <div className="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-4xl" style={{ background: 'var(--color-bg-elevated)' }}>🎯</div>
          <h2 className="text-xl font-bold mb-2">Start Your First Saving Goal</h2>
          <p className="mb-6" style={{ color: 'var(--color-text-muted)' }}>Pick a goal, save daily, build streaks. Like Duolingo — but for money.</p>

          <h3 className="text-sm font-semibold mb-3" style={{ color: 'var(--color-text-secondary)' }}>Popular Templates</h3>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3 max-w-lg mx-auto">
            {TEMPLATES.map(t => (
              <button key={t.name} onClick={() => { setNewGoal({ emoji: t.emoji, name: t.name, targetAmount: t.amount.toString(), targetDate: '' }); setShowCreate(true); }}
                className="glass-card p-4 text-left hover:scale-[1.02] transition-all">
                <span className="text-2xl">{t.emoji}</span>
                <p className="text-sm font-semibold mt-2">{t.name}</p>
                <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>{formatCurrency(t.amount)}</p>
              </button>
            ))}
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {goals.map(goal => {
            const progress = getProgress(goal);
            return (
              <div key={goal.id} className="glass-card p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <span className="text-3xl">{goal.emoji || '🎯'}</span>
                    <div>
                      <h3 className="font-bold text-lg">{goal.name}</h3>
                      <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>
                        {formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)}
                      </p>
                    </div>
                  </div>
                  {goal.status === 'COMPLETED' ? (
                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 flex items-center gap-1">
                      <Trophy className="w-3 h-3" /> Completed!
                    </span>
                  ) : (
                    <button onClick={() => deleteGoal(goal.id)} className="text-xs px-2 py-1 rounded hover:opacity-70" style={{ color: 'var(--color-text-muted)' }}>✕</button>
                  )}
                </div>

                {/* Progress bar */}
                <div className="h-3 rounded-full overflow-hidden mb-3" style={{ background: 'var(--color-bg-elevated)' }}>
                  <div className="h-full rounded-full transition-all duration-700" style={{ width: `${progress}%`, background: progress >= 100 ? 'var(--color-accent-500)' : 'var(--color-brand-500)' }} />
                </div>
                <div className="flex items-center justify-between text-xs mb-4" style={{ color: 'var(--color-text-muted)' }}>
                  <span>{progress.toFixed(0)}% complete</span>
                  <span>{formatCurrency(goal.targetAmount - goal.currentAmount)} remaining</span>
                </div>

                {/* Streak + Check-in */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-1">
                      <Flame className="w-4 h-4" style={{ color: goal.streak > 0 ? '#ef4444' : 'var(--color-text-muted)' }} />
                      <span className="text-sm font-bold">{goal.streak}</span>
                      <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>day streak</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <TrendingUp className="w-4 h-4" style={{ color: 'var(--color-accent-400)' }} />
                      <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>Best: {goal.longestStreak} days</span>
                    </div>
                  </div>

                  {goal.status === 'ACTIVE' && (
                    <button onClick={() => setShowCheckIn(goal.id)} className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold gradient-brand text-white">
                      <Sparkles className="w-4 h-4" /> Check In
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Goal Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(0,0,0,0.6)' }}>
          <div className="glass-card p-6 w-full max-w-md animate-slide-up">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold">New Saving Goal</h2>
              <button onClick={() => setShowCreate(false)} className="touch-target"><X className="w-5 h-5" /></button>
            </div>

            <div className="space-y-4">
              <div className="flex flex-wrap gap-2 justify-center">
                {['🎯', '✈️', '🏠', '📱', '🎓', '🚗', '💰', '🎧'].map(e => (
                  <button key={e} onClick={() => setNewGoal(prev => ({ ...prev, emoji: e }))}
                    className={`text-2xl p-2 rounded-lg transition-all ${newGoal.emoji === e ? 'bg-[var(--color-brand-500)]/20 ring-1 ring-[var(--color-brand-500)]' : ''}`}>{e}</button>
                ))}
              </div>
              <input value={newGoal.name} onChange={e => setNewGoal(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Goal name (e.g., Europe Trip)" className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] outline-none focus:border-[var(--color-brand-500)]" />
              <div className="relative">
                <span className="absolute left-4 top-3 text-lg font-bold" style={{ color: 'var(--color-text-muted)' }}>₹</span>
                <input value={newGoal.targetAmount} onChange={e => setNewGoal(prev => ({ ...prev, targetAmount: e.target.value }))}
                  type="number" placeholder="Target amount" className="w-full pl-10 pr-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] outline-none focus:border-[var(--color-brand-500)]" />
              </div>
              <div>
                <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--color-text-muted)' }}>Target Date (optional)</label>
                <input value={newGoal.targetDate} onChange={e => setNewGoal(prev => ({ ...prev, targetDate: e.target.value }))}
                  type="date" className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] outline-none focus:border-[var(--color-brand-500)]" />
              </div>

              <button onClick={createGoal} disabled={saving || !newGoal.name || !newGoal.targetAmount}
                className="w-full py-3 rounded-xl text-sm font-semibold gradient-brand text-white disabled:opacity-50 flex items-center justify-center gap-2">
                {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Target className="w-4 h-4" />}
                Create Goal
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Check-in Modal */}
      {showCheckIn && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(0,0,0,0.6)' }}>
          <div className="glass-card p-6 w-full max-w-md animate-slide-up">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold flex items-center gap-2">
                <Sparkles className="w-5 h-5" style={{ color: 'var(--color-accent-400)' }} /> Daily Check-In
              </h2>
              <button onClick={() => setShowCheckIn(null)} className="touch-target"><X className="w-5 h-5" /></button>
            </div>

            <p className="text-sm mb-4" style={{ color: 'var(--color-text-secondary)' }}>How much did you save today?</p>

            <div className="space-y-4">
              <div className="relative">
                <span className="absolute left-4 top-3 text-lg font-bold" style={{ color: 'var(--color-text-muted)' }}>₹</span>
                <input value={checkInAmount} onChange={e => setCheckInAmount(e.target.value)} autoFocus
                  type="number" placeholder="Amount saved" className="w-full pl-10 pr-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] outline-none focus:border-[var(--color-brand-500)] text-lg" />
              </div>
              <input value={checkInNote} onChange={e => setCheckInNote(e.target.value)}
                placeholder="Note (optional)" className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] outline-none focus:border-[var(--color-brand-500)]" />
              <button onClick={() => doCheckIn(showCheckIn)} disabled={saving || !checkInAmount}
                className="w-full py-3 rounded-xl text-sm font-semibold gradient-brand text-white disabled:opacity-50 flex items-center justify-center gap-2">
                {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Flame className="w-4 h-4" />}
                Save & Keep Streak
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
