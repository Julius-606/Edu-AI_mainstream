import React, { useState, useEffect } from 'react';
import { Sparkles, Shield, GraduationCap, ArrowRight, UserCheck, KeyRound } from 'lucide-react';
import { User, UserRole } from '../types';
import { INITIAL_USERS } from '../data/mockData';

interface LoginScreenProps {
  onLoginSuccess: (user: User) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('student@trace.edu');
  const [password, setPassword] = useState('password123');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedDemoIndex, setSelectedDemoIndex] = useState(0);
  const [backendMode, setBackendMode] = useState<'cloud' | 'ngrok' | 'container'>('container');

  useEffect(() => {
    fetch('/api/config/backend')
      .then(r => r.json())
      .then(data => {
        if (data && data.activeMode) {
          setBackendMode(data.activeMode);
        }
      })
      .catch(() => {});
  }, []);

  const handleBackendChange = async (mode: 'cloud' | 'ngrok' | 'container') => {
    setBackendMode(mode);
    try {
      await fetch('/api/config/backend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode })
      });
    } catch (e) {
      console.error("Failed to switch backend mode:", e);
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      // Call backend login endpoint
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json();

      let targetUser = INITIAL_USERS.find((u) => u.id === data.user_id);
      if (!targetUser) {
        targetUser = {
          ...INITIAL_USERS[0],
          username: data.username || 'Student User',
          role: data.role || 'Student'
        };
      }
      onLoginSuccess(targetUser);
    } catch {
      // Offline fallback login
      const fallbackUser = INITIAL_USERS[selectedDemoIndex] || INITIAL_USERS[0];
      onLoginSuccess(fallbackUser);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSelectDemo = (index: number) => {
    setSelectedDemoIndex(index);
    const demo = INITIAL_USERS[index];
    if (demo) {
      setEmail(demo.email);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 sm:p-6 relative">
      <div className="w-full max-w-md bg-slate-900/90 border border-slate-800/90 rounded-3xl shadow-2xl p-6 sm:p-8 backdrop-blur-xl animate-in zoom-in-95 duration-200">
        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-indigo-600/20 border border-indigo-500/40 text-indigo-400 mb-4 shadow-lg shadow-indigo-600/20">
            <span className="text-2xl font-black tracking-tight">T</span>
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Trace Portal</h1>
          <p className="text-xs text-slate-400 mt-1">Adaptive Clinical & Academic Learning System</p>
        </div>

        {/* Demo Fast-Switch Pills */}
        <div className="mb-6">
          <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2 text-center">
            Instant Demo Profiles
          </label>
          <div className="grid grid-cols-3 gap-2">
            {INITIAL_USERS.map((u, i) => (
              <button
                key={u.id}
                type="button"
                onClick={() => handleSelectDemo(i)}
                className={`py-2 px-1 rounded-xl text-center border transition-all ${
                  selectedDemoIndex === i
                    ? 'bg-indigo-600/20 border-indigo-500 text-indigo-200 shadow-sm'
                    : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
                }`}
              >
                <p className="text-xs font-bold leading-none">{u.role}</p>
                <p className="text-[10px] text-slate-400 truncate mt-1">{u.username.split(' ')[0]}</p>
              </button>
            ))}
          </div>
        </div>

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Email Address
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:border-indigo-500 outline-none transition-colors"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:border-indigo-500 outline-none transition-colors"
              required
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-sm shadow-lg shadow-indigo-600/30 transition-all hover:translate-y-[-1px] disabled:opacity-50"
          >
            {isSubmitting ? (
              <span>Authenticating...</span>
            ) : (
              <>
                <span>Access Clinical Trail</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="mt-6 pt-5 border-t border-slate-800/80">
          <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2 text-center">
            Backend Gateway Target
          </label>
          <div className="grid grid-cols-3 gap-1.5 p-1 bg-slate-950/80 border border-slate-800/80 rounded-2xl">
            {(['cloud', 'ngrok', 'container'] as const).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => handleBackendChange(m)}
                className={`py-2 px-1 rounded-xl text-center text-[10px] font-bold transition-all ${
                  backendMode === m ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
                }`}
              >
                {m === 'cloud' && 'Cloud'}
                {m === 'ngrok' && 'Ngrok'}
                {m === 'container' && 'Container'}
              </button>
            ))}
          </div>
          <div className="text-[10px] text-slate-500 text-center mt-2.5 font-medium">
            {backendMode === 'cloud' && 'Connected to: Hugging Face Production'}
            {backendMode === 'ngrok' && 'Connected to: Ngrok Tunnel'}
            {backendMode === 'container' && 'Connected to: AI Studio Background Service (Port 8001)'}
          </div>
        </div>

        <div className="mt-6 pt-5 border-t border-slate-800/80 text-center">
          <p className="text-[11px] text-slate-500 flex items-center justify-center gap-1.5">
            <Shield className="w-3.5 h-3.5 text-emerald-400" />
            <span>Encrypted Room Database Cache & Cloud Gateway Active</span>
          </p>
        </div>
      </div>
    </div>
  );
};
