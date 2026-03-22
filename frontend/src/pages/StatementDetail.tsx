import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft, Loader2, CreditCard, Building, Calendar, AlertTriangle, TrendingDown,
  TrendingUp, IndianRupee, Search, Filter, Shield, Zap, Sparkles, ChevronRight, CheckCircle2
} from 'lucide-react';
import { apiClient } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';

type Tab = 'overview' | 'transactions' | 'insights';

export default function StatementDetail() {
  const { id } = useParams<{ id: string }>();
  const { getToken } = useAuth();

  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const [statement, setStatement] = useState<any>(null);
  const [transactions, setTransactions] = useState<any[]>([]);
  const [insight, setInsight] = useState<any>(null);
  const [chartData, setChartData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Pagination for transactions
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchStatementData();
  }, [id, getToken]);

  useEffect(() => {
    if (activeTab === 'overview' && chartData.length === 0) {
      if (statement) {
        fetchChartData();
      }
    } else if (activeTab === 'transactions') {
      fetchTransactions(page);
    } else if (activeTab === 'insights' && !insight) {
      fetchInsight();
    }
  }, [activeTab, page, statement]);

  const fetchStatementData = async () => {
    try {
      const token = await getToken();
      const res = await apiClient.get(`/statements/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        setStatement(res.data.data);
      }
    } catch (err) {
      console.error('Error fetching statement metadata', err);
    } finally {
      if (activeTab === 'overview') setLoading(false);
    }
  };

  const fetchChartData = async () => {
    try {
      const token = await getToken();
      const res = await apiClient.get(`/statements/${id}/transactions/chart-data`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        setChartData(res.data.data);
      }
    } catch (err) {
      console.error('Error fetching chart data', err);
    }
  };

  const fetchTransactions = async (pageNumber: number) => {
    setLoading(true);
    try {
      const token = await getToken();
      const res = await apiClient.get(`/statements/${id}/transactions`, {
        params: { page: pageNumber, size: 20 },
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        setTransactions(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
      }
    } catch (err) {
      console.error('Error fetching transactions', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchInsight = async () => {
    setLoading(true);
    try {
      const token = await getToken();
      const res = await apiClient.get(`/statements/${id}/insights`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        setInsight(res.data.data);
      }
    } catch (err) {
      console.error('Error fetching insights', err);
      // It's possible insights aren't generated yet or at all if not required, but usually they are.
    } finally {
      setLoading(false);
    }
  };

  if (loading && !statement) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--color-brand-500)' }} />
      </div>
    );
  }

  if (!statement) {
    return (
      <div className="text-center py-16 animate-fade-in">
        <AlertTriangle className="w-12 h-12 mx-auto mb-4" style={{ color: 'var(--color-warning-500)' }} />
        <p className="text-lg font-medium" style={{ color: 'var(--color-text-primary)' }}>Statement not found</p>
        <Link to="/statements" className="mt-4 inline-block text-sm transition-colors hover:text-white" style={{ color: 'var(--color-brand-400)' }}>
          ← Back to statements
        </Link>
      </div>
    );
  }

  const isCC = statement.statementType === 'CC';

  const COLORS = ['#6366f1', '#a855f7', '#ec4899', '#f43f5e', '#facc15', '#10b981', '#3b82f6'];

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Back link */}
      <Link to="/statements" className="inline-flex items-center gap-2 text-sm transition-colors hover:text-white" style={{ color: 'var(--color-brand-400)' }}>
        <ArrowLeft className="w-4 h-4" /> All Statements
      </Link>

      {/* Header Card */}
      <div className="glass-card p-6 border-l-4" style={{ borderLeftColor: isCC ? '#6366f1' : '#10b981' }}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ background: isCC ? 'rgba(99, 102, 241, 0.15)' : 'rgba(16, 185, 129, 0.15)' }}>
              {isCC ? <CreditCard className="w-6 h-6" style={{ color: 'var(--color-brand-400)' }} /> : <Building className="w-6 h-6" style={{ color: 'var(--color-accent-400)' }} />}
            </div>
            <div>
              <h1 className="text-xl font-bold" style={{ color: 'var(--color-text-primary)' }}>
                {statement.bankName} {isCC ? `••${statement.cardLast4 || ''}` : `••${statement.accountLast4 || ''}`}
              </h1>
              <div className="flex items-center gap-2 text-sm mt-0.5" style={{ color: 'var(--color-text-muted)' }}>
                <Calendar className="w-3.5 h-3.5" />
                {statement.statementMonth}
                {statement.parseConfidence && (
                  <>
                    <span className="opacity-50">•</span>
                    <span className="flex items-center gap-1" style={{ color: statement.parseConfidence > 0.8 ? 'var(--color-accent-400)' : 'var(--color-warning-400)' }}>
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      {Math.round(statement.parseConfidence * 100)}% accuracy
                    </span>
                  </>
                )}
              </div>
            </div>
          </div>
          
          {isCC && statement.billDueDate && (
            <div className="p-3 rounded-xl flex items-center gap-4 text-right" style={{ background: 'rgba(251, 191, 36, 0.05)', border: '1px solid rgba(251, 191, 36, 0.15)' }}>
              <div>
                <p className="text-xs font-medium" style={{ color: 'var(--color-warning-400)' }}>Total Due</p>
                <p className="text-lg font-bold" style={{ color: 'var(--color-text-primary)' }}>₹{(statement.totalAmountDue || 0).toLocaleString()}</p>
              </div>
              <div className="h-8 w-px bg-yellow-500/20"></div>
              <div>
                <p className="text-xs font-medium" style={{ color: 'var(--color-warning-400)' }}>Due Date</p>
                <p className="text-sm font-bold" style={{ color: 'var(--color-text-primary)' }}>{statement.billDueDate}</p>
              </div>
            </div>
          )}
        </div>

        {/* Stats Row */}
        <div className="grid grid-cols-3 gap-4 mt-6">
          <div className="p-4 rounded-xl flex items-center gap-3 transition-colors hover:bg-slate-800/50" style={{ background: 'var(--color-bg-elevated)', border: '1px solid rgba(255,255,255,0.02)' }}>
            <div className="w-10 h-10 rounded-full flex items-center justify-center bg-rose-500/10">
              <TrendingDown className="w-5 h-5 text-rose-500" />
            </div>
            <div>
              <p className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>Total Spent</p>
              <p className="text-lg font-bold text-rose-500">₹{(statement.totalDebit || 0).toLocaleString()}</p>
            </div>
          </div>
          <div className="p-4 rounded-xl flex items-center gap-3 transition-colors hover:bg-slate-800/50" style={{ background: 'var(--color-bg-elevated)', border: '1px solid rgba(255,255,255,0.02)' }}>
            <div className="w-10 h-10 rounded-full flex items-center justify-center bg-emerald-500/10">
              <TrendingUp className="w-5 h-5 text-emerald-500" />
            </div>
            <div>
              <p className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>Total Credits</p>
              <p className="text-lg font-bold text-emerald-500">₹{(statement.totalCredit || 0).toLocaleString()}</p>
            </div>
          </div>
          <div className="p-4 rounded-xl flex items-center gap-3 transition-colors hover:bg-slate-800/50" style={{ background: 'var(--color-bg-elevated)', border: '1px solid rgba(255,255,255,0.02)' }}>
            <div className="w-10 h-10 rounded-full flex items-center justify-center bg-indigo-500/10">
              <IndianRupee className="w-5 h-5 text-indigo-500" />
            </div>
            <div>
              <p className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>Transactions</p>
              <p className="text-lg font-bold text-indigo-400">{statement.transactionCount}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b" style={{ borderColor: 'var(--color-border)' }}>
        {['overview', 'transactions', 'insights'].map((tab) => (
          <button
            key={tab}
            onClick={() => { setActiveTab(tab as Tab); setPage(0); }}
            className={`px-6 py-4 text-sm font-semibold capitalize transition-colors relative ${activeTab === tab ? 'text-indigo-400' : 'text-slate-400 hover:text-slate-200'}`}
          >
            {tab}
            {activeTab === tab && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500 rounded-t-full" />
            )}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="min-h-[400px]">
        {loading && (
          <div className="flex items-center justify-center h-48">
            <Loader2 className="w-6 h-6 animate-spin text-indigo-500" />
          </div>
        )}

        {/* OVERVIEW TAB */}
        {!loading && activeTab === 'overview' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-slide-up">
            <div className="glass-card p-6 flex flex-col items-center justify-center h-80">
              <h3 className="font-bold text-lg mb-6 self-start w-full">Spend by Category</h3>
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="80%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      cx="50%" cy="50%"
                      innerRadius={60} outerRadius={80}
                      paddingAngle={5}
                      dataKey="value"
                      stroke="none"
                    >
                      {chartData.map((_entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <RechartsTooltip 
                      formatter={(value: any) => `₹${Number(value).toLocaleString()}`}
                      contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px', color: '#f8fafc' }}
                      itemStyle={{ color: '#f8fafc' }}
                    />
                    <Legend verticalAlign="bottom" height={36} iconType="circle" />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="text-center text-slate-400 text-sm">
                  <PieChart className="w-12 h-12 mx-auto mb-2 opacity-50" />
                  <p>Not enough transaction data for chart.</p>
                  <button onClick={() => setActiveTab('transactions')} className="text-indigo-400 mt-2 underline">View Transactions</button>
                </div>
              )}
            </div>

            <div className="glass-card p-6">
              <h3 className="font-bold text-lg mb-4">Statement Summary</h3>
              <div className="space-y-4">
                <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/50">
                  <span className="text-sm text-slate-400">Total Credits</span>
                  <span className="font-semibold text-emerald-400">₹{(statement.totalCredit || 0).toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/50">
                  <span className="text-sm text-slate-400">Total Debits</span>
                  <span className="font-semibold text-rose-400">₹{(statement.totalDebit || 0).toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/50">
                  <span className="text-sm text-slate-400">Closing Balance</span>
                  <span className="font-semibold text-slate-200">₹{(statement.closingBalance || 0).toLocaleString()}</span>
                </div>
                
                <div className="mt-8 pt-6 border-t border-slate-700/50">
                  <div className="flex items-start gap-3 p-4 rounded-xl bg-indigo-500/10 border border-indigo-500/20">
                    <Sparkles className="w-5 h-5 text-indigo-400 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="text-sm font-semibold text-indigo-300">AI Deep Dive Available</p>
                      <p className="text-xs text-indigo-400/80 mt-1 mb-2">We've generated custom insights and found hidden patterns in your spending.</p>
                      <button onClick={() => setActiveTab('insights')} className="text-xs font-bold text-white bg-indigo-500 hover:bg-indigo-600 px-3 py-1.5 rounded-lg transition-colors">
                        View Insights
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TRANSACTIONS TAB */}
        {!loading && activeTab === 'transactions' && (
          <div className="glass-card overflow-hidden animate-slide-up">
            <div className="p-4 border-b border-slate-700/50 flex flex-col sm:flex-row gap-4 justify-between items-center bg-slate-800/30">
              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input 
                  type="text" 
                  placeholder="Search merchants..." 
                  value={searchTerm}
                  onChange={e => setSearchTerm(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg py-2 pl-9 pr-4 text-sm focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all text-slate-200"
                />
              </div>
              <button className="flex items-center gap-2 text-sm text-slate-300 hover:text-white px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 transition-colors">
                <Filter className="w-4 h-4" /> Filter
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-900/50 text-slate-400 font-medium border-b border-slate-700/50">
                  <tr>
                    <th className="px-6 py-4">Date</th>
                    <th className="px-6 py-4">Merchant / Details</th>
                    <th className="px-6 py-4">Category</th>
                    <th className="px-6 py-4 text-right">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {transactions.filter(t => t.merchantName?.toLowerCase().includes(searchTerm.toLowerCase()) || 
                                            t.description?.toLowerCase().includes(searchTerm.toLowerCase()))
                  .map((t, idx) => (
                    <tr key={t.id || idx} className="hover:bg-slate-800/30 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap text-slate-300">
                        {t.transactionDate}
                      </td>
                      <td className="px-6 py-4 max-w-xs">
                        <p className="font-semibold text-slate-200 truncate" title={t.merchantName || t.description}>
                          {t.merchantName || 'Unknown'}
                        </p>
                        <p className="text-xs text-slate-500 truncate mt-0.5" title={t.rawDescription}>
                          {t.description || t.rawDescription}
                        </p>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                          {t.category || 'Uncategorized'}
                        </span>
                        {t.isFee && (
                          <span className="ml-2 px-2.5 py-1 text-xs font-medium rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20">
                            Fee
                          </span>
                        )}
                        {t.isRecurring && (
                          <span className="ml-2 px-2.5 py-1 text-xs font-medium rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
                            Subscription
                          </span>
                        )}
                      </td>
                      <td className={`px-6 py-4 whitespace-nowrap text-right font-medium ${t.transactionType === 'CREDIT' ? 'text-emerald-400' : 'text-slate-200'}`}>
                        {t.transactionType === 'CREDIT' ? '+' : ''}₹{(t.amount || 0).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                  {transactions.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-6 py-12 text-center text-slate-400">
                        <p>No transactions found.</p>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="p-4 border-t border-slate-700/50 flex justify-between items-center bg-slate-800/30">
                <button 
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 text-sm bg-slate-800 rounded-lg disabled:opacity-50 text-slate-300 hover:text-white transition-colors"
                >
                  Previous
                </button>
                <span className="text-sm text-slate-400">Page {page + 1} of {totalPages}</span>
                <button 
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-4 py-2 text-sm bg-slate-800 rounded-lg disabled:opacity-50 text-slate-300 hover:text-white transition-colors"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}

        {/* INSIGHTS TAB */}
        {!loading && activeTab === 'insights' && (
          <div className="space-y-6 animate-slide-up">
            {insight ? (
              <>
                {/* AI Summary Narrative */}
                {insight.narrativeSummary && (
                  <div className="glass-card p-6 border-l-4 border-indigo-500 relative overflow-hidden">
                    <div className="absolute top-0 right-0 p-4 opacity-5">
                      <Sparkles className="w-24 h-24" />
                    </div>
                    <h3 className="flex items-center gap-2 font-bold text-lg mb-3 text-indigo-300">
                      <Sparkles className="w-5 h-5" /> AI Spending Analysis
                    </h3>
                    <p className="text-slate-300 leading-relaxed text-sm whitespace-pre-line relative z-10">
                      {insight.narrativeSummary}
                    </p>
                  </div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Hidden Charges */}
                  <div className="glass-card p-6 flex flex-col h-full">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="font-bold flex items-center gap-2 text-rose-400">
                        <AlertTriangle className="w-5 h-5" /> Hidden Charges Detected
                      </h3>
                      <span className="bg-rose-500/10 text-rose-400 text-xs font-bold px-2 py-1 rounded-lg">
                        {insight.hiddenCharges?.length || 0} Found
                      </span>
                    </div>
                    
                    {insight.hiddenCharges && insight.hiddenCharges.length > 0 ? (
                      <div className="space-y-3 flex-grow">
                        {insight.hiddenCharges.map((charge: any, idx: number) => (
                          <div key={idx} className="bg-slate-800/50 p-3 rounded-xl border border-rose-500/20">
                            <div className="flex justify-between items-start mb-1">
                              <span className="font-semibold text-sm text-slate-200">{charge.type?.replace('_', ' ')}</span>
                              <span className="font-bold text-rose-500 flex items-center gap-0.5">
                                <TrendingDown className="w-3.5 h-3.5" /> ₹{charge.amount}
                              </span>
                            </div>
                            <p className="text-xs text-slate-400 mb-2">{charge.description}</p>
                            <p className="text-xs font-medium text-emerald-400 bg-emerald-500/10 inline-block px-2 py-1 rounded-md">
                              💡 {charge.advice}
                            </p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="flex flex-col items-center justify-center flex-grow py-8 text-center bg-slate-800/30 rounded-xl border border-dashed border-slate-700">
                        <Shield className="w-8 h-8 text-emerald-500 mb-2 opacity-80" />
                        <p className="font-medium text-emerald-400">Clean Statement!</p>
                        <p className="text-xs text-slate-400 mt-1">We didn't find any hidden fees or penalties.</p>
                      </div>
                    )}
                  </div>

                  {/* Subscriptions */}
                  <div className="glass-card p-6 flex flex-col h-full">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="font-bold flex items-center gap-2 text-blue-400">
                        <Zap className="w-5 h-5" /> Active Subscriptions
                      </h3>
                    </div>
                    
                    {insight.subscriptions && insight.subscriptions.length > 0 ? (
                      <div className="space-y-2 flex-grow">
                        {insight.subscriptions.map((sub: any, idx: number) => (
                          <div key={idx} className="flex items-center justify-between bg-slate-800/50 p-3 rounded-xl">
                            <div>
                              <p className="font-semibold text-sm text-slate-200">{sub.merchantName}</p>
                              <p className="text-xs text-slate-400 flex items-center gap-2 mt-0.5">
                                {sub.frequency} 
                                {sub.isNew && <span className="bg-blue-500/20 text-blue-300 px-1.5 rounded-full text-[10px]">NEW</span>}
                              </p>
                            </div>
                            <p className="font-bold text-slate-300">₹{sub.amount}</p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="flex flex-col items-center justify-center flex-grow py-8 text-center bg-slate-800/30 rounded-xl border border-dashed border-slate-700">
                        <p className="text-slate-400 text-sm">No recurring subscriptions detected.</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Saving Suggestions */}
                {insight.savingSuggestions && insight.savingSuggestions.length > 0 && (
                  <div className="glass-card p-6 bg-gradient-to-br from-slate-900 to-slate-800 border-t-4 border-emerald-500">
                    <h3 className="font-bold text-lg mb-4 text-emerald-400 flex items-center gap-2">
                      <Target className="w-5 h-5" /> Saving Opportunities
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      {insight.savingSuggestions.map((suggestion: string, idx: number) => (
                        <div key={idx} className="flex gap-3 bg-slate-800 p-4 rounded-xl items-start">
                          <CheckCircle2 className="w-5 h-5 text-emerald-500 flex-shrink-0 mt-0.5" />
                          <p className="text-sm text-slate-300 leading-relaxed">{suggestion}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="glass-card p-12 text-center border-dashed border-2 border-slate-700">
                <Sparkles className="w-12 h-12 text-indigo-500 mx-auto mb-4 opacity-50" />
                <h3 className="text-xl font-bold text-slate-300 mb-2">No Insights Available</h3>
                <p className="text-slate-400 text-sm max-w-md mx-auto">
                  We couldn't generate deep insights for this statement. This could happen if there aren't enough transactions yet.
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// Ensure Target icon is defined since we used it in the Insight tab
function Target(props: any) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="10" />
      <circle cx="12" cy="12" r="6" />
      <circle cx="12" cy="12" r="2" />
    </svg>
  );
}
