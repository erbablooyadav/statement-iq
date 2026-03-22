import { useState, useRef, useCallback } from 'react';
import { Upload as UploadIcon, FileText, X, Shield, ChevronDown, Loader2 } from 'lucide-react';
import { apiClient } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { useNavigate } from 'react-router-dom';

const SUPPORTED_BANKS = [
  'HDFC', 'ICICI', 'SBI', 'Axis', 'Kotak', 'IndusInd',
  'RBL', 'AMEX', 'Yes Bank', 'IDFC First', 'Federal Bank', 'Other'
];

export default function Upload() {
  const { getToken } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [file, setFile] = useState<File | null>(null);
  const [statementType, setStatementType] = useState<'CC' | 'BANK'>('CC');
  const [bankName, setBankName] = useState('');
  const [cardLast4, setCardLast4] = useState('');
  const [accountLast4, setAccountLast4] = useState('');
  const [password, setPassword] = useState('');
  const [uploading, setUploading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');
  const [dragActive, setDragActive] = useState(false);

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') setDragActive(true);
    else if (e.type === 'dragleave') setDragActive(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    const dropped = e.dataTransfer.files?.[0];
    if (dropped?.type === 'application/pdf') setFile(dropped);
    else setError('Only PDF files are accepted');
  }, []);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (selected) {
      if (selected.type !== 'application/pdf') {
        setError('Only PDF files are accepted');
        return;
      }
      if (selected.size > 10 * 1024 * 1024) {
        setError('File size must be under 10MB');
        return;
      }
      setFile(selected);
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!file || !bankName) return;
    setError('');
    setUploading(true);

    try {
      const token = await getToken();
      const formData = new FormData();
      formData.append('file', file);
      formData.append('statementType', statementType);
      formData.append('bankName', bankName);
      if (statementType === 'CC' && cardLast4) formData.append('cardLast4', cardLast4);
      if (statementType === 'BANK' && accountLast4) formData.append('accountLast4', accountLast4);
      if (password) formData.append('password', password);

      const response = await apiClient.post('/statements/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.data.success) {
        const statementId = response.data.data.id;
        setUploading(false);
        setProcessing(true);

        // Poll for processing status
        const pollInterval = setInterval(async () => {
          try {
            const statusRes = await apiClient.get(`/statements/${statementId}/status`, {
              headers: { Authorization: `Bearer ${token}` },
            });
            const status = statusRes.data.data;
            setProgress(status.progress);

            if (status.status === 'COMPLETED') {
              clearInterval(pollInterval);
              navigate(`/statements/${statementId}`);
            } else if (status.status === 'FAILED') {
              clearInterval(pollInterval);
              setProcessing(false);
              setError(status.error || 'Processing failed. Please try again.');
            }
          } catch {
            clearInterval(pollInterval);
            setProcessing(false);
            setError('Failed to check processing status');
          }
        }, 3000);
      }
    } catch (err: any) {
      setUploading(false);
      setError(err.response?.data?.error?.message || 'Upload failed. Please try again.');
    }
  };

  if (processing) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] animate-fade-in">
        <div className="glass-card p-8 text-center max-w-md w-full">
          <Loader2 className="w-12 h-12 mx-auto mb-4 animate-spin" style={{ color: 'var(--color-brand-500)' }} />
          <h2 className="text-xl font-bold mb-2" style={{ color: 'var(--color-text-primary)' }}>Analyzing Your Statement</h2>
          <p className="text-sm mb-6" style={{ color: 'var(--color-text-secondary)' }}>
            Our AI is categorizing transactions, hunting hidden charges, and generating insights...
          </p>
          
          {/* Progress bar */}
          <div className="w-full rounded-full h-3 mb-2" style={{ background: 'var(--color-bg-elevated)' }}>
            <div
              className="h-3 rounded-full gradient-brand transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>{progress}% complete</p>

          {/* Privacy note */}
          <div className="flex items-center gap-2 mt-6 p-3 rounded-lg" style={{ background: 'rgba(16, 185, 129, 0.1)' }}>
            <Shield className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--color-accent-500)' }} />
            <p className="text-xs" style={{ color: 'var(--color-accent-400)' }}>
              Your PDF is being deleted from memory right now.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto space-y-6 animate-slide-up">
      <div>
        <h1 className="text-2xl font-bold" style={{ color: 'var(--color-text-primary)' }}>Upload Statement</h1>
        <p className="text-sm mt-1" style={{ color: 'var(--color-text-secondary)' }}>
          Drop your credit card or bank statement PDF
        </p>
      </div>

      {/* Drop Zone */}
      <div
        onDragEnter={handleDrag} onDragLeave={handleDrag} onDragOver={handleDrag} onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`glass-card p-8 text-center cursor-pointer transition-all ${dragActive ? 'scale-[1.02]' : 'hover:border-[var(--color-brand-500)]'}`}
        style={{ border: dragActive ? '2px solid var(--color-brand-500)' : '2px dashed var(--color-border)' }}
      >
        <input ref={fileInputRef} type="file" accept=".pdf" onChange={handleFileSelect} className="hidden" />
        
        {file ? (
          <div className="flex items-center justify-center gap-3">
            <FileText className="w-8 h-8" style={{ color: 'var(--color-brand-400)' }} />
            <div className="text-left">
              <p className="font-medium" style={{ color: 'var(--color-text-primary)' }}>{file.name}</p>
              <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>{(file.size / 1024 / 1024).toFixed(2)} MB</p>
            </div>
            <button onClick={(e) => { e.stopPropagation(); setFile(null); }} className="touch-target" style={{ color: 'var(--color-text-muted)' }}>
              <X className="w-5 h-5" />
            </button>
          </div>
        ) : (
          <>
            <UploadIcon className="w-12 h-12 mx-auto mb-3" style={{ color: 'var(--color-text-muted)' }} />
            <p className="font-medium" style={{ color: 'var(--color-text-primary)' }}>
              {dragActive ? 'Drop your PDF here' : 'Tap to select or drag & drop your PDF'}
            </p>
            <p className="text-xs mt-1" style={{ color: 'var(--color-text-muted)' }}>PDF only • Max 10MB</p>
          </>
        )}
      </div>

      {/* Statement Type */}
      <div className="glass-card p-4">
        <label className="block text-sm font-medium mb-2" style={{ color: 'var(--color-text-secondary)' }}>Statement Type</label>
        <div className="grid grid-cols-2 gap-2">
          {(['CC', 'BANK'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setStatementType(type)}
              className="py-3 rounded-xl font-semibold text-sm transition-all"
              style={{
                background: statementType === type ? 'var(--color-brand-600)' : 'var(--color-bg-elevated)',
                color: statementType === type ? 'white' : 'var(--color-text-secondary)',
                border: `1px solid ${statementType === type ? 'var(--color-brand-600)' : 'var(--color-border)'}`,
              }}
            >
              {type === 'CC' ? '💳 Credit Card' : '🏦 Bank Account'}
            </button>
          ))}
        </div>
      </div>

      {/* Bank Selection */}
      <div className="glass-card p-4">
        <label className="block text-sm font-medium mb-2" style={{ color: 'var(--color-text-secondary)' }}>Bank Name</label>
        <div className="relative">
          <select
            value={bankName} onChange={e => setBankName(e.target.value)}
            className="w-full py-3 px-4 rounded-xl appearance-none focus:outline-none focus:ring-2"
            style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)', '--tw-ring-color': 'var(--color-brand-500)' } as any}
          >
            <option value="">Select your bank</option>
            {SUPPORTED_BANKS.map(bank => <option key={bank} value={bank}>{bank}</option>)}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 pointer-events-none" style={{ color: 'var(--color-text-muted)' }} />
        </div>
      </div>

      {/* Card/Account Last 4 */}
      <div className="glass-card p-4">
        <label className="block text-sm font-medium mb-2" style={{ color: 'var(--color-text-secondary)' }}>
          {statementType === 'CC' ? 'Card Last 4 Digits' : 'Account Last 4 Digits'} (optional)
        </label>
        <input
          type="text" maxLength={4} pattern="\d{4}"
          value={statementType === 'CC' ? cardLast4 : accountLast4}
          onChange={e => statementType === 'CC' ? setCardLast4(e.target.value) : setAccountLast4(e.target.value)}
          className="w-full py-3 px-4 rounded-xl focus:outline-none focus:ring-2"
          style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)', '--tw-ring-color': 'var(--color-brand-500)' } as any}
          placeholder="1234"
        />
      </div>

      {/* PDF Password */}
      <div className="glass-card p-4">
        <label className="block text-sm font-medium mb-2" style={{ color: 'var(--color-text-secondary)' }}>
          PDF Password (optional)
        </label>
        <input
          type="password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          className="w-full py-3 px-4 rounded-xl focus:outline-none focus:ring-2"
          style={{ background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)', border: '1px solid var(--color-border)', '--tw-ring-color': 'var(--color-brand-500)' } as any}
          placeholder="Bank PDF password (if any)"
        />
      </div>

      {/* Error */}
      {error && (
        <p className="text-sm py-2 px-3 rounded-lg" style={{ color: 'var(--color-danger-400)', background: 'rgba(244, 63, 94, 0.1)' }}>
          {error}
        </p>
      )}

      {/* Upload Button */}
      <button
        onClick={handleUpload}
        disabled={!file || !bankName || uploading}
        className="w-full py-4 rounded-xl font-semibold text-white gradient-brand transition-all hover:opacity-90 disabled:opacity-50 text-lg"
      >
        {uploading ? 'Uploading...' : 'Analyze Statement'}
      </button>

      {/* Privacy */}
      <div className="flex items-center gap-2 p-3 rounded-lg" style={{ background: 'rgba(16, 185, 129, 0.1)' }}>
        <Shield className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--color-accent-500)' }} />
        <p className="text-xs" style={{ color: 'var(--color-accent-400)' }}>
          Your PDF is never stored — deleted from memory immediately after processing.
        </p>
      </div>
    </div>
  );
}
