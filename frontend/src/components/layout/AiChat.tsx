import { useState, useRef, useEffect } from 'react';
import { MessageSquare, X, Send, Loader2, Bot, Sparkles } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useAuth } from '../../store/AuthContext';

export default function AiChat() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<{ role: 'user' | 'ai'; content: string }[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll
  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || loading) return;
    const userMsg = input;
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);
    setLoading(true);

    try {
      const res = await apiClient.post('/insights/chat', { message: userMsg, history: messages });
      if (res.data.success) {
        setMessages(prev => [...prev, { role: 'ai', content: res.data.data }]);
      }
    } catch (e: any) {
      setMessages(prev => [...prev, { 
        role: 'ai', 
        content: e.response?.data?.error?.message || 'Sorry, I encountered an error. Please try again or check your AI provider settings.' 
      }]);
    }
    setLoading(false);
  };

  if (!isOpen) {
    return (
      <button 
        onClick={() => setIsOpen(true)}
        className="fixed bottom-24 md:bottom-8 right-4 md:right-8 w-14 h-14 rounded-full gradient-brand shadow-xl flex items-center justify-center text-white hover:scale-110 transition-transform z-50 group"
      >
        <MessageSquare className="w-6 h-6" />
        <span className="absolute -top-10 right-0 bg-white dark:bg-slate-800 text-slate-800 dark:text-white text-xs px-3 py-1.5 rounded-xl shadow border border-slate-200 dark:border-slate-700 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap hidden md:block">
          Ask StatementIQ AI
        </span>
      </button>
    );
  }

  return (
    <div className="fixed bottom-0 md:bottom-8 right-0 md:right-8 w-full md:w-[400px] h-[85vh] md:h-[600px] bg-white dark:bg-slate-900 md:rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col z-50 animate-slide-up">
      {/* Header */}
      <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gradient-brand text-white md:rounded-t-2xl">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center backdrop-blur-sm">
            <Bot className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-sm">StatementIQ AI</h3>
            <p className="text-xs text-white/80">Your financial co-pilot</p>
          </div>
        </div>
        <button onClick={() => setIsOpen(false)} className="hover:bg-white/20 p-2 rounded-xl transition-colors">
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.length === 0 && (
          <div className="text-center py-8 text-slate-500 flex flex-col items-center">
            <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center mb-4">
              <Sparkles className="w-8 h-8 text-purple-500" />
            </div>
            <p className="text-sm font-medium">Hello {user?.displayName}!</p>
            <p className="text-xs mt-1 max-w-[250px]">Ask me about your spending habits, recent transactions, or tips on how to reach your goals faster.</p>
          </div>
        )}

        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[85%] rounded-2xl p-3 text-sm ${
              m.role === 'user' 
                ? 'bg-purple-600 text-white rounded-tr-sm' 
                : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 rounded-tl-sm'
            }`}>
              {m.content}
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex justify-start">
            <div className="bg-slate-100 dark:bg-slate-800 rounded-2xl rounded-tl-sm p-4 flex gap-2">
              <div className="w-2 h-2 rounded-full bg-slate-400 animate-bounce" />
              <div className="w-2 h-2 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '0.2s' }} />
              <div className="w-2 h-2 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '0.4s' }} />
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className="p-4 border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 md:rounded-b-2xl">
        <div className="relative">
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && sendMessage()}
            placeholder="Ask about your finances..."
            className="w-full pl-4 pr-12 py-3 rounded-xl bg-slate-100 dark:bg-slate-800 border-transparent focus:border-purple-500 focus:bg-white dark:focus:bg-slate-900 outline-none transition-all text-sm"
          />
          <button 
            onClick={sendMessage}
            disabled={!input.trim() || loading}
            className="absolute right-2 top-2 p-1.5 rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-50 transition-colors"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
