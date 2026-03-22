import { useState, useEffect } from 'react';
import { useAuth } from '../store/AuthContext';
import { apiClient } from '../api/client';
import {
  Settings as SettingsIcon, User, Bell, Shield, Key, Monitor,
  Sun, Moon, LogOut, Trash2, Download, ChevronRight, Check,
  ExternalLink, AlertTriangle, Loader2, Cpu
} from 'lucide-react';

type Tab = 'profile' | 'ai' | 'notifications' | 'appearance' | 'privacy';

const AI_PROVIDERS = [
  { key: 'CLAUDE', name: 'Claude (Anthropic)', icon: '🟣', description: 'Most advanced reasoning. Best for financial analysis.', link: 'https://console.anthropic.com/settings/keys', prefix: 'sk-ant-', guide: 'Go to console.anthropic.com → API Keys → Create Key' },
  { key: 'GEMINI', name: 'Gemini (Google)', icon: '🔵', description: 'Free tier available (15 RPM). Great for getting started.', link: 'https://aistudio.google.com/apikey', prefix: 'AIza', guide: 'Go to aistudio.google.com/apikey → Create API Key' },
  { key: 'OPENAI', name: 'OpenAI', icon: '🟢', description: 'GPT-4o Mini — affordable. Good all-round choice.', link: 'https://platform.openai.com/api-keys', prefix: 'sk-', guide: 'Go to platform.openai.com/api-keys → Create new secret key' },
  { key: 'LOCAL', name: 'Local Model', icon: '🖥️', description: '100% private. Runs on your machine. No data leaves.', link: 'https://ollama.com/download', prefix: '', guide: 'Install Ollama → Run: ollama serve → Pull model: ollama pull llama3' },
];

export default function Settings() {
  const { user, logout } = useAuth();
  const [tab, setTab] = useState<Tab>('profile');
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'dark');
  const [aiSettings, setAiSettings] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [newKey, setNewKey] = useState('');
  const [newModel, setNewModel] = useState('');
  const [selectedProvider, setSelectedProvider] = useState('');
  const [localUrl, setLocalUrl] = useState('');
  const [localModel, setLocalModel] = useState('llama3');
  const [localApiKey, setLocalApiKey] = useState('');
  const [testing, setTesting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [notifications, setNotifications] = useState({
    billDueReminders: true, overspendAlerts: true, goalNudges: true,
    monthlyReportCard: true, weeklyGoalSummary: true,
  });

  useEffect(() => {
    if (tab === 'ai') loadAiSettings();
  }, [tab]);

  const loadAiSettings = async () => {
    try {
      const res = await apiClient.get('/settings/ai');
      if (res.data.success) {
        setAiSettings(res.data.data);
        const pref = res.data.data.preferredProvider || 'CLAUDE';
        setSelectedProvider(pref);
        setNewModel(res.data.data.configuredModels?.[pref] || '');
        setLocalUrl(res.data.data.localModelUrl || '');
        setLocalModel(res.data.data.localModelName || 'llama3');
      }
    } catch (e) { console.error('Failed to load AI settings', e); }
  };

  const showMsg = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 3000);
  };

  const savePreferredProvider = async (provider: string) => {
    setSelectedProvider(provider);
    setNewModel(aiSettings?.configuredModels?.[provider] || '');
    try {
      await apiClient.put('/settings/ai/provider', { provider });
      showMsg('success', `Provider set to ${AI_PROVIDERS.find(p => p.key === provider)?.name}`);
      loadAiSettings();
    } catch (e) { showMsg('error', 'Failed to save provider'); }
  };

  const saveApiKey = async (provider: string) => {
    if (!newKey.trim()) {
      showMsg('error', 'API key is required');
      return;
    }
    try {
      await apiClient.put('/settings/ai/key', { provider, apiKey: newKey, modelOverride: newModel });
      showMsg('success', 'API key & Model saved!');
      setNewKey('');
      loadAiSettings();
    } catch (e) { showMsg('error', 'Failed to save API key'); }
  };

  const deleteApiKey = async (provider: string) => {
    try {
      await apiClient.delete(`/settings/ai/key/${provider}`);
      showMsg('success', 'API key removed');
      loadAiSettings();
    } catch (e) { showMsg('error', 'Failed to remove key'); }
  };

  const saveLocalUrl = async () => {
    try {
      await apiClient.put('/settings/ai/local-url', { url: localUrl, model: localModel, apiKey: localApiKey });
      showMsg('success', 'Local model URL saved');
      loadAiSettings();
    } catch (e) { showMsg('error', 'Failed to save'); }
  };

  const testProvider = async (provider: string) => {
    setTesting(true);
    try {
      const body: any = { provider };
      if (provider === 'LOCAL') {
        body.localUrl = localUrl;
        body.model = localModel;
        if (localApiKey) body.apiKey = localApiKey;
      } else {
        if (newKey) body.apiKey = newKey;
        if (newModel) body.model = newModel;
      }
      const res = await apiClient.post('/settings/ai/test', body);
      if (res.data.data?.success) showMsg('success', '✅ Connection successful!');
      else showMsg('error', res.data.data?.error || 'Connection failed');
    } catch (e: any) { showMsg('error', e.response?.data?.error?.message || 'Test failed'); }
    setTesting(false);
  };

  const changeTheme = (t: string) => {
    setTheme(t);
    localStorage.setItem('theme', t);
    if (t === 'light') document.documentElement.setAttribute('data-theme', 'light');
    else document.documentElement.removeAttribute('data-theme');
  };

  const handleExport = async () => {
    try {
      const res = await apiClient.get('/settings/export');
      const blob = new Blob([JSON.stringify(res.data.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'statementiq-export.json'; a.click();
      showMsg('success', 'Data exported!');
    } catch (e) { showMsg('error', 'Export failed'); }
  };

  const handleDeleteAccount = async () => {
    try {
      await apiClient.delete('/settings/account');
      await logout();
    } catch (e) { showMsg('error', 'Deletion failed'); }
  };

  const saveNotifications = async () => {
    try {
      await apiClient.put('/settings/notifications', notifications);
      showMsg('success', 'Preferences saved');
    } catch (e) { showMsg('error', 'Failed to save'); }
  };

  const tabs = [
    { key: 'profile' as Tab, icon: User, label: 'Profile' },
    { key: 'ai' as Tab, icon: Cpu, label: 'AI Provider' },
    { key: 'notifications' as Tab, icon: Bell, label: 'Notifications' },
    { key: 'appearance' as Tab, icon: Monitor, label: 'Appearance' },
    { key: 'privacy' as Tab, icon: Shield, label: 'Privacy & Data' },
  ];

  return (
    <div className="max-w-4xl mx-auto animate-slide-up">
      <h1 className="text-2xl font-bold mb-6">Settings</h1>

      {/* Toast */}
      {message && (
        <div className={`mb-4 p-3 rounded-xl text-sm font-medium flex items-center gap-2 animate-slide-up ${message.type === 'success' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
          {message.type === 'success' ? <Check className="w-4 h-4" /> : <AlertTriangle className="w-4 h-4" />}
          {message.text}
        </div>
      )}

      {/* Tab bar */}
      <div className="flex overflow-x-auto gap-2 mb-6 pb-2">
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-all ${tab === t.key ? 'gradient-brand text-white' : 'glass-card hover:opacity-80'}`}>
            <t.icon className="w-4 h-4" /> {t.label}
          </button>
        ))}
      </div>

      {/* Profile Tab */}
      {tab === 'profile' && (
        <div className="space-y-4">
          <div className="glass-card p-6">
            <div className="flex items-center gap-4 mb-6">
              <div className="w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold" style={{ background: 'var(--color-brand-600)' }}>
                {user?.displayName?.[0] || user?.email?.[0] || '?'}
              </div>
              <div>
                <h3 className="text-lg font-semibold">{user?.displayName || 'User'}</h3>
                <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>{user?.email}</p>
                <span className="text-xs px-2 py-0.5 rounded-full mt-1 inline-block" style={{ background: 'var(--color-brand-600)', color: 'white' }}>
                  {user?.plan || 'FREE'} Plan
                </span>
              </div>
            </div>
          </div>
          <button onClick={logout} className="w-full glass-card p-4 flex items-center gap-3 rounded-xl text-left hover:opacity-80 transition-all" style={{ color: 'var(--color-danger-500)' }}>
            <LogOut className="w-5 h-5" /> Sign Out
          </button>
        </div>
      )}

      {/* AI Provider Tab */}
      {tab === 'ai' && (
        <div className="space-y-4">
          <p className="text-sm mb-2" style={{ color: 'var(--color-text-secondary)' }}>
            Choose your AI provider for statement analysis and insights. Bring your own API key — it's free!
          </p>

          {AI_PROVIDERS.map(p => (
            <div key={p.key} className={`glass-card p-5 transition-all cursor-pointer ${selectedProvider === p.key ? 'ring-2 ring-[var(--color-brand-500)]' : ''}`}>
              <div className="flex items-center justify-between mb-3" onClick={() => savePreferredProvider(p.key)}>
                <div className="flex items-center gap-3">
                  <span className="text-2xl">{p.icon}</span>
                  <div>
                    <h3 className="font-semibold">{p.name}</h3>
                    <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>{p.description}</p>
                  </div>
                </div>
                {selectedProvider === p.key && (
                  <div className="w-6 h-6 rounded-full flex items-center justify-center" style={{ background: 'var(--color-brand-500)' }}>
                    <Check className="w-4 h-4 text-white" />
                  </div>
                )}
              </div>

              {/* Configured key/model indicator */}
              {(aiSettings?.configuredKeys?.[p.key] || aiSettings?.configuredModels?.[p.key]) && (
                <div className="flex items-center justify-between p-2 rounded-lg mb-3" style={{ background: 'var(--color-bg-elevated)' }}>
                  <div className="flex flex-col gap-1">
                    {aiSettings?.configuredKeys?.[p.key] && (
                      <span className="text-xs font-mono" style={{ color: 'var(--color-text-muted)' }}>
                        🔑 {aiSettings.configuredKeys[p.key]}
                      </span>
                    )}
                    {aiSettings?.configuredModels?.[p.key] && (
                      <span className="text-xs font-medium" style={{ color: 'var(--color-brand-400)' }}>
                        Model: {aiSettings.configuredModels[p.key]}
                      </span>
                    )}
                  </div>
                  <button onClick={() => deleteApiKey(p.key)} className="text-xs px-2 py-1 rounded hover:opacity-70" style={{ color: 'var(--color-danger-400)' }}>Remove</button>
                </div>
              )}

              {/* API Key or Local URL input */}
              {selectedProvider === p.key && (
                <div className="space-y-3 pt-3" style={{ borderTop: '1px solid var(--color-border)' }}>
                  {/* Setup guide */}
                  <div className="p-3 rounded-lg text-xs" style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-secondary)' }}>
                    <strong>📖 How to get your key:</strong>
                    <p className="mt-1">{p.guide}</p>
                    <a href={p.link} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 mt-2 font-medium hover:underline" style={{ color: 'var(--color-brand-400)' }}>
                      Open {p.name} Console <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>

                  {p.key === 'LOCAL' ? (
                    <>
                      <input value={localUrl} onChange={e => setLocalUrl(e.target.value)}
                        placeholder="API Base URL (e.g. http://localhost:11434, https://api.sambanova.ai)" className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] text-sm outline-none focus:border-[var(--color-brand-500)]" />
                      <input value={localModel} onChange={e => setLocalModel(e.target.value)}
                        placeholder="Model name (e.g., llama3, Meta-Llama-3-70B-Instruct)" className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] text-sm outline-none focus:border-[var(--color-brand-500)]" />
                      <input value={localApiKey} onChange={e => setLocalApiKey(e.target.value)} type="password"
                        placeholder="API Key (Optional for Ollama, required for Sambanova/Groq)" className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] text-sm outline-none focus:border-[var(--color-brand-500)] font-mono" />
                      <div className="flex gap-2">
                        <button onClick={saveLocalUrl} className="flex-1 py-2 rounded-xl text-sm font-medium gradient-brand text-white">Save Config</button>
                        <button onClick={() => testProvider('LOCAL')} disabled={testing} className="flex-1 py-2 rounded-xl text-sm font-medium border border-[var(--color-border)] hover:opacity-80 flex items-center justify-center gap-2">
                          {testing ? <Loader2 className="w-4 h-4 animate-spin" /> : null} Test Connection
                        </button>
                      </div>
                    </>
                  ) : (
                    <>
                      <input value={newKey} onChange={e => setNewKey(e.target.value)} type="password"
                        placeholder={`Paste your ${p.name} API key (${p.prefix}...)`}
                        className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] text-sm outline-none focus:border-[var(--color-brand-500)] font-mono" />
                      
                      <input value={newModel} onChange={e => setNewModel(e.target.value)}
                        placeholder="Model name (e.g., gpt-4o, claude-3-5-sonnet) — Optional"
                        className="w-full px-4 py-3 rounded-xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] text-sm outline-none focus:border-[var(--color-brand-500)]" />

                      <div className="flex gap-2">
                        <button onClick={() => saveApiKey(p.key)} className="flex-1 py-2 rounded-xl text-sm font-medium gradient-brand text-white">Save Key & Model</button>
                        <button onClick={() => testProvider(p.key)} disabled={testing} className="flex-1 py-2 rounded-xl text-sm font-medium border border-[var(--color-border)] hover:opacity-80 flex items-center justify-center gap-2">
                          {testing ? <Loader2 className="w-4 h-4 animate-spin" /> : null} Test Connection
                        </button>
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Notifications Tab */}
      {tab === 'notifications' && (
        <div className="glass-card p-6 space-y-4">
          {[
            { key: 'billDueReminders', label: 'Bill Due Reminders', desc: 'Get notified 5 days before your bill is due' },
            { key: 'overspendAlerts', label: 'Overspend Alerts', desc: 'Alert when spending spikes 30%+ in a category' },
            { key: 'goalNudges', label: 'Goal Nudges', desc: "Reminders when you haven't checked in" },
            { key: 'monthlyReportCard', label: 'Monthly Report Card', desc: 'Financial health summary on the 1st of each month' },
            { key: 'weeklyGoalSummary', label: 'Weekly Goal Summary', desc: 'Progress update every Sunday' },
          ].map(n => (
            <div key={n.key} className="flex items-center justify-between">
              <div>
                <p className="font-medium text-sm">{n.label}</p>
                <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>{n.desc}</p>
              </div>
              <button
                onClick={() => setNotifications(prev => ({ ...prev, [n.key]: !(prev as any)[n.key] }))}
                className={`w-12 h-6 rounded-full transition-all relative ${(notifications as any)[n.key] ? 'bg-[var(--color-brand-500)]' : 'bg-[var(--color-bg-elevated)]'}`}>
                <div className={`absolute top-0.5 w-5 h-5 rounded-full bg-white transition-all ${(notifications as any)[n.key] ? 'left-6' : 'left-0.5'}`} />
              </button>
            </div>
          ))}
          <button onClick={saveNotifications} className="w-full py-3 rounded-xl text-sm font-semibold gradient-brand text-white mt-4">Save Preferences</button>
        </div>
      )}

      {/* Appearance Tab */}
      {tab === 'appearance' && (
        <div className="glass-card p-6 space-y-4">
          <h3 className="font-semibold mb-2">Theme</h3>
          <div className="grid grid-cols-3 gap-3">
            {[
              { key: 'dark', icon: Moon, label: 'Dark' },
              { key: 'light', icon: Sun, label: 'Light' },
              { key: 'system', icon: Monitor, label: 'System' },
            ].map(t => (
              <button key={t.key} onClick={() => changeTheme(t.key)}
                className={`flex flex-col items-center gap-2 p-4 rounded-xl border transition-all ${theme === t.key ? 'border-[var(--color-brand-500)] bg-[var(--color-brand-500)]/10' : 'border-[var(--color-border)]'}`}>
                <t.icon className="w-6 h-6" />
                <span className="text-sm font-medium">{t.label}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Privacy & Data Tab */}
      {tab === 'privacy' && (
        <div className="space-y-4">
          <div className="glass-card p-6">
            <h3 className="font-semibold mb-2">🔒 Privacy Commitment</h3>
            <ul className="text-sm space-y-2" style={{ color: 'var(--color-text-secondary)' }}>
              <li>✅ PDFs deleted from memory immediately after processing</li>
              <li>✅ No transaction data stored in application logs</li>
              <li>✅ Zero bank login or SMS access required</li>
              <li>✅ End-to-end encryption in transit and at rest</li>
              <li>✅ DPDP Act compliant — export or delete anytime</li>
            </ul>
          </div>

          <button onClick={handleExport} className="w-full glass-card p-4 flex items-center justify-between rounded-xl hover:opacity-80 transition-all">
            <div className="flex items-center gap-3">
              <Download className="w-5 h-5" style={{ color: 'var(--color-brand-400)' }} />
              <div>
                <p className="font-medium text-sm">Export My Data</p>
                <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>Download all your data as JSON</p>
              </div>
            </div>
            <ChevronRight className="w-5 h-5" style={{ color: 'var(--color-text-muted)' }} />
          </button>

          {!showDeleteConfirm ? (
            <button onClick={() => setShowDeleteConfirm(true)} className="w-full glass-card p-4 flex items-center justify-between rounded-xl hover:opacity-80 transition-all">
              <div className="flex items-center gap-3">
                <Trash2 className="w-5 h-5" style={{ color: 'var(--color-danger-500)' }} />
                <div>
                  <p className="font-medium text-sm" style={{ color: 'var(--color-danger-500)' }}>Delete Account</p>
                  <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>Permanently delete all your data</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5" style={{ color: 'var(--color-text-muted)' }} />
            </button>
          ) : (
            <div className="glass-card p-6 border border-red-500/30">
              <div className="flex items-center gap-2 mb-3" style={{ color: 'var(--color-danger-500)' }}>
                <AlertTriangle className="w-5 h-5" />
                <h3 className="font-semibold">This action is irreversible</h3>
              </div>
              <p className="text-sm mb-4" style={{ color: 'var(--color-text-secondary)' }}>
                This will permanently delete your account, all uploaded statements, transactions, goals, and insights. This cannot be undone.
              </p>
              <div className="flex gap-3">
                <button onClick={() => setShowDeleteConfirm(false)} className="flex-1 py-2 rounded-xl text-sm font-medium border border-[var(--color-border)]">Cancel</button>
                <button onClick={handleDeleteAccount} className="flex-1 py-2 rounded-xl text-sm font-medium bg-red-600 text-white">Delete Permanently</button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
