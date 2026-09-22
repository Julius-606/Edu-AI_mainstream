import React, { useState, useEffect } from 'react';
import { X, RefreshCw, Database, Download, Upload, CheckCircle2, AlertCircle } from 'lucide-react';
import { TraceStore } from '../lib/store';

interface SyncModalProps {
  onClose: () => void;
  onSnapshotRestored: () => void;
}

export const SyncModal: React.FC<SyncModalProps> = ({ onClose, onSnapshotRestored }) => {
  const [serverStatus, setServerStatus] = useState<'checking' | 'online' | 'offline'>('checking');
  const [hasGeminiKey, setHasGeminiKey] = useState<boolean>(false);
  const [syncing, setSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/health')
      .then((res) => res.json())
      .then((data) => {
        setServerStatus('online');
        setHasGeminiKey(Boolean(data.hasGeminiKey));
      })
      .catch(() => {
        setServerStatus('offline');
      });
  }, []);

  const handleExportBackup = () => {
    const data = {
      user: TraceStore.getUser(),
      units: TraceStore.getUnits(),
      quizHistory: TraceStore.getQuizHistory(),
      timetable: TraceStore.getTimetable(),
      bookmarks: TraceStore.getBookmarks(),
      chatSessions: TraceStore.getChatSessions(),
      timestamp: new Date().toISOString()
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `trace-learning-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    setSyncMessage('Learning trail successfully exported to JSON backup.');
  };

  const handleRestoreDefaultSnapshot = () => {
    setSyncing(true);
    setTimeout(() => {
      const count = TraceStore.restoreSnapshot();
      setSyncing(false);
      setSyncMessage(`Restored official curriculum snapshot with ${count} comprehensive clinical units.`);
      onSnapshotRestored();
    }, 400);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-emerald-600/20 text-emerald-400">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Settings & Synchronization</h3>
              <p className="text-xs text-slate-400">Offline Room DB caching, backend status & cloud sync</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {/* Server Connection Status */}
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div
                className={`w-3 h-3 rounded-full ${
                  serverStatus === 'online'
                    ? 'bg-emerald-400 animate-pulse'
                    : serverStatus === 'checking'
                    ? 'bg-amber-400'
                    : 'bg-red-400'
                }`}
              />
              <div>
                <p className="text-xs font-semibold text-white">
                  Trace Local & API Gateway: {serverStatus.toUpperCase()}
                </p>
                <p className="text-[11px] text-slate-400">
                  {serverStatus === 'online'
                    ? hasGeminiKey
                      ? 'Connected to Express backend with Gemini API key active'
                      : 'Connected to Express backend with academic deterministic AI engine'
                    : 'Running in resilient offline-first client mode'}
                </p>
              </div>
            </div>
            <button
              onClick={() => {
                setServerStatus('checking');
                fetch('/api/health')
                  .then((res) => res.json())
                  .then((d) => {
                    setServerStatus('online');
                    setHasGeminiKey(Boolean(d.hasGeminiKey));
                  })
                  .catch(() => setServerStatus('offline'));
              }}
              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg"
              title="Refresh connection"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>

          {syncMessage && (
            <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-800/60 text-emerald-300 text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{syncMessage}</span>
            </div>
          )}

          {/* Backup & Restore Controls */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Data Persistence & Snapshot
            </h4>

            <button
              onClick={handleExportBackup}
              className="w-full flex items-center justify-between p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 hover:border-slate-700 transition-colors text-left"
            >
              <div className="flex items-center gap-3">
                <Download className="w-4 h-4 text-indigo-400 shrink-0" />
                <div>
                  <p className="text-xs font-semibold text-white">Export Learning Trail</p>
                  <p className="text-[11px] text-slate-400">Download completed modules, quiz history, and notes</p>
                </div>
              </div>
              <span className="text-xs font-semibold text-indigo-400">JSON</span>
            </button>

            <button
              onClick={handleRestoreDefaultSnapshot}
              disabled={syncing}
              className="w-full flex items-center justify-between p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 hover:border-indigo-500/50 transition-colors text-left"
            >
              <div className="flex items-center gap-3">
                <RefreshCw className={`w-4 h-4 text-emerald-400 shrink-0 ${syncing ? 'animate-spin' : ''}`} />
                <div>
                  <p className="text-xs font-semibold text-white">Reset / Restore Curriculum Snapshot</p>
                  <p className="text-[11px] text-slate-400">Reset default units, sample assessments, and syllabus outline</p>
                </div>
              </div>
              <span className="text-xs font-semibold text-emerald-400">Restore</span>
            </button>
          </div>
        </div>

        <div className="p-4 border-t border-slate-800 bg-slate-950/60 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
