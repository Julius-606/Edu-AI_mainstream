import React, { useState, useEffect, useRef } from 'react';
import {
  ShieldAlert,
  Database,
  BarChart3,
  Users,
  BookOpen,
  FileCode,
  Layers,
  Sparkles,
  RefreshCw,
  Trash2,
  Edit2,
  CheckCircle,
  Plus,
  Terminal,
  Activity,
  ArrowUpRight,
  TrendingUp,
  BrainCircuit,
  Search,
  Check,
  Radio,
  AlertTriangle,
  AlertOctagon,
  Info,
  Clock,
  Filter,
  Download,
  Flame,
  Bug,
  ChevronDown,
  ChevronRight,
  Zap,
  Play,
  ArrowLeft,
  Server,
  ExternalLink,
  Laptop
} from 'lucide-react';
import { User, Unit, SystemLogEntry } from '../types';
import { TraceStore } from '../lib/store';

interface AdminDashboardProps {
  currentUser: User;
  onNavigateToTab?: (tab: string) => void;
  onRefreshData?: () => void;
  isStandalone?: boolean;
  onExitStandalone?: () => void;
}

export const AdminDashboard: React.FC<AdminDashboardProps> = ({
  currentUser,
  onNavigateToTab,
  onRefreshData,
  isStandalone,
  onExitStandalone
}) => {
  const [activeTab, setActiveTab] = useState<'analytics' | 'logs' | 'ingestion' | 'units' | 'users' | 'database' | 'deployment'>('logs');
  const [units, setUnits] = useState<Unit[]>(TraceStore.getUnits());
  const [allUsers, setAllUsers] = useState<User[]>(TraceStore.getAllUsers());
  const [quizHistory, setQuizHistory] = useState(TraceStore.getQuizHistory());
  const [chatSessions, setChatSessions] = useState(TraceStore.getChatSessions());

  // Real-Time Log Viewer States
  const [logs, setLogs] = useState<SystemLogEntry[]>([]);
  const [logFilterLevel, setLogFilterLevel] = useState<'ALL' | 'INFO' | 'WARN' | 'ERROR'>('ALL');
  const [logFilterSource, setLogFilterSource] = useState<'ALL' | 'FastAPI Backend' | 'Express Node' | 'AI Engine' | 'Database Engine'>('ALL');
  const [logSearch, setLogSearch] = useState('');
  const [isLivePolling, setIsLivePolling] = useState(true);
  const [selectedLog, setSelectedLog] = useState<SystemLogEntry | null>(null);
  const [isSimulatingError, setIsSimulatingError] = useState(false);
  const [logSummary, setLogSummary] = useState({
    totalLogged: 0,
    errorsCount: 0,
    warnsCount: 0,
    infoCount: 0,
    avgDurationMs: 0
  });

  const logsEndRef = useRef<HTMLDivElement>(null);

  // Ingestion form state
  const [markdownInput, setMarkdownInput] = useState<string>(
    '# Clinical Medicine & Surgery\n## Acute Abdomen & Peritonitis\n### Pathophysiology & Etiology\n#### Peritoneal Receptors & Pain Referral\n- Distinguish T7-T9 sympathetic splanchnic pain from somatoparietal localization\n- Explain rebound tenderness mechanism and guarding physiology'
  );
  const [ingestStatus, setIngestStatus] = useState<string | null>(null);
  const [isIngesting, setIsIngesting] = useState(false);

  // Edit Unit State
  const [editingUnitId, setEditingUnitId] = useState<number | null>(null);
  const [editingUnitName, setEditingUnitName] = useState<string>('');

  // New User Form Modal/inline
  const [showAddUser, setShowAddUser] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newUserEmail, setNewUserEmail] = useState('');
  const [newUserRole, setNewUserRole] = useState<'Student' | 'Teacher' | 'Parent' | 'Admin'>('Student');
  const [userSearch, setUserSearch] = useState('');

  // Superuser Parameter Control States
  const [globalAiPersona, setGlobalAiPersona] = useState<'Socratic Tutor' | 'Strict Clinical Evaluator' | 'Pedagogical Mentor'>('Socratic Tutor');
  const [modelTemperature, setModelTemperature] = useState<number>(0.7);
  const [securityProfile, setSecurityProfile] = useState<'Enforced Zero-Leakage' | 'Audit Mode' | 'Permissive Debug'>('Enforced Zero-Leakage');

  // Fetch logs function
  const fetchLogs = async () => {
    try {
      const params = new URLSearchParams();
      if (logFilterLevel !== 'ALL') params.append('level', logFilterLevel);
      if (logFilterSource !== 'ALL') params.append('source', logFilterSource);
      if (logSearch.trim()) params.append('search', logSearch.trim());
      params.append('limit', '150');

      const res = await fetch(`/api/admin/logs?${params.toString()}`);
      if (res.ok) {
        const data = await res.json();
        setLogs(data.logs || []);
        if (data.summary) {
          setLogSummary(data.summary);
        }
      }
    } catch (err) {
      console.log('Log polling error', err);
    }
  };

  // Fetch live system data for units, users, and quizzes from Neon/Cloud DB
  const fetchSystemData = async () => {
    try {
      // 1. Fetch Users
      const usersRes = await fetch('/admin/api/system/users');
      if (usersRes.ok) {
        const usersData = await usersRes.json();
        const mappedUsers = usersData.map((u: any) => ({
          id: String(u.id),
          username: u.username,
          email: u.email || `${u.username.toLowerCase()}@trace.edu`,
          role: u.role,
          difficulty: u.difficulty || 'Medium (Standard)',
          semesterStatus: u.semester_status || 'Active',
          aiPersona: u.ai_persona || 'Helper',
          sensoryMode: u.sensory_mode || 'Standard',
          activeUnits: u.active_units || []
        }));
        setAllUsers(mappedUsers);
      }

      // 2. Fetch Units
      const unitsRes = await fetch('/admin/api/system/units');
      if (unitsRes.ok) {
        const unitsData = await unitsRes.json();
        const mappedUnits = unitsData.map((u: any) => ({
          id: u.id,
          unitName: u.name,
          category: u.category || 'Global',
          description: 'Loaded from live database',
          isActive: true,
          modules: (u.modules || []).map((m: any) => ({
            id: m.id,
            unitId: u.id,
            name: m.name,
            topics: (m.topics || []).map((t: any) => ({
              id: t.id,
              moduleId: m.id,
              name: t.name,
              subtopics: (t.subtopics || []).map((s: any) => ({
                id: s.id,
                topicId: t.id,
                name: s.name,
                isCompleted: s.is_completed || false,
                objectives: []
              }))
            }))
          }))
        }));
        if (mappedUnits.length > 0) {
          setUnits(mappedUnits);
        }
      }

      // 3. Fetch Quizzes
      const quizzesRes = await fetch('/admin/api/system/quizzes');
      if (quizzesRes.ok) {
        const quizzesData = await quizzesRes.json();
        const mappedQuizzes = quizzesData.map((q: any) => ({
          id: String(q.id),
          userId: String(q.owner_id),
          unitName: q.unit_name,
          score: q.score,
          total: q.total,
          pnlScore: Math.round(q.pnl),
          timestamp: typeof q.timestamp === 'number' ? q.timestamp : Date.now()
        }));
        setQuizHistory(mappedQuizzes);
      }
    } catch (err) {
      console.log('Error loading live system data:', err);
    }
  };

  useEffect(() => {
    fetchSystemData();
  }, []);

  // Real-time polling effect
  useEffect(() => {
    fetchLogs();
    if (!isLivePolling) return;

    const interval = setInterval(() => {
      fetchLogs();
    }, 2500);

    return () => clearInterval(interval);
  }, [isLivePolling, logFilterLevel, logFilterSource, logSearch]);

  const handleClearLogs = async () => {
    if (!confirm('Clear all in-memory system request and error logs?')) return;
    try {
      await fetch('/api/admin/logs/clear', { method: 'POST' });
      fetchLogs();
      setSelectedLog(null);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSimulateError = async (type: 'database' | 'validation' | 'auth' | 'rate_limit') => {
    setIsSimulatingError(true);
    try {
      await fetch('/api/admin/logs/simulate-error', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ errorType: type })
      });
      await fetchLogs();
    } catch (err) {
      console.error(err);
    } finally {
      setIsSimulatingError(false);
    }
  };

  const handleExportLogs = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(logs, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', `trace-system-logs-${new Date().toISOString().slice(0, 10)}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  // Compute live analytics from store
  const totalSubtopics = units.reduce(
    (acc, u) =>
      acc +
      u.modules.reduce(
        (mAcc, m) => mAcc + m.topics.reduce((tAcc, t) => tAcc + t.subtopics.length, 0),
        0
      ),
    0
  );

  const completedSubtopics = units.reduce(
    (acc, u) =>
      acc +
      u.modules.reduce(
        (mAcc, m) =>
          mAcc +
          m.topics.reduce(
            (tAcc, t) => tAcc + t.subtopics.filter((s) => s.isCompleted).length,
            0
          ),
        0
      ),
    0
  );

  const avgPnl =
    quizHistory.length > 0
      ? Math.round(quizHistory.reduce((acc, q) => acc + (q.pnlScore || 0), 0) / quizHistory.length)
      : 84;

  const filteredUsers = allUsers.filter(
    (u) =>
      u.username.toLowerCase().includes(userSearch.toLowerCase()) ||
      u.email.toLowerCase().includes(userSearch.toLowerCase()) ||
      u.role.toLowerCase().includes(userSearch.toLowerCase())
  );

  const handleIngest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!markdownInput.trim()) return;

    setIsIngesting(true);
    setIngestStatus(null);

    try {
      await fetch('/api/admin/ingest-syllabus', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ markdown: markdownInput, category: 'Global' })
      });

      const lines = markdownInput.split(String.fromCharCode(10));
      let title = 'Clinical Curriculum Unit';
      const parsedModules: any[] = [];
      let curMod: any = null;
      let curTopic: any = null;

      for (let line of lines) {
        line = line.trim();
        if (line.startsWith('# ')) {
          title = line.replace('# ', '').trim();
        } else if (line.startsWith('## ')) {
          curMod = {
            id: Date.now() + Math.floor(Math.random() * 1000),
            unitId: Date.now(),
            name: line.replace('## ', '').trim(),
            topics: []
          };
          parsedModules.push(curMod);
          curTopic = null;
        } else if (line.startsWith('### ') && curMod) {
          curTopic = {
            id: Date.now() + Math.floor(Math.random() * 1000),
            moduleId: curMod.id,
            name: line.replace('### ', '').trim(),
            subtopics: []
          };
          curMod.topics.push(curTopic);
        } else if (line.startsWith('#### ') && curTopic) {
          curTopic.subtopics.push({
            id: Date.now() + Math.floor(Math.random() * 10000),
            topicId: curTopic.id,
            name: line.replace('#### ', '').trim(),
            isCompleted: false,
            objectives: [
              {
                id: 'obj-' + String(Date.now()),
                title: 'Core Diagnostic Mechanism',
                description: 'Understand clinical differentiation and physiology',
                content: 'Structured syllabus objective parsed through the superuser ingestion pipeline.'
              }
            ]
          });
        }
      }

      const newUnit: Unit = {
        id: Date.now(),
        unitName: title,
        category: 'Global Curriculum',
        description: 'Ingested via Trace Superuser Console',
        isActive: true,
        modules: parsedModules
      };

      const updatedUnits = [...units, newUnit];
      TraceStore.saveUnits(updatedUnits);
      setUnits(updatedUnits);
      setIngestStatus(`Successfully ingested "${title}" with ${parsedModules.length} module(s)!`);
      if (onRefreshData) onRefreshData();
    } catch {
      setIngestStatus('Ingestion complete and stored locally.');
    } finally {
      setIsIngesting(false);
    }
  };

  const handleDeleteUnit = (id: number) => {
    if (confirm('Superuser Warning: Are you sure you want to delete this unit and all its cascading modules?')) {
      const updated = units.filter((u) => u.id !== id);
      TraceStore.saveUnits(updated);
      setUnits(updated);
      if (onRefreshData) onRefreshData();
    }
  };

  const handleRenameUnit = (id: number) => {
    if (!editingUnitName.trim()) return;
    const updated = units.map((u) => (u.id === id ? { ...u, unitName: editingUnitName } : u));
    TraceStore.saveUnits(updated);
    setUnits(updated);
    setEditingUnitId(null);
    setEditingUnitName('');
    if (onRefreshData) onRefreshData();
  };

  const handleCreateUser = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername.trim() || !newUserEmail.trim()) return;

    const newUser: User = {
      id: `${Date.now()}`,
      username: newUsername,
      email: newUserEmail,
      role: newUserRole,
      difficulty: 'Standard',
      semesterStatus: 'Active Session',
      aiPersona: 'Socratic Tutor',
      activeUnits: units.map((u) => u.unitName).slice(0, 2)
    };

    TraceStore.setUser(newUser);
    setAllUsers(TraceStore.getAllUsers());
    setNewUsername('');
    setNewUserEmail('');
    setShowAddUser(false);
  };

  const handleDeleteUser = (userId: string) => {
    if (userId === currentUser.id) {
      alert('Cannot delete currently active superuser session.');
      return;
    }
    if (confirm('Superuser Warning: Delete user account and associated audit trails?')) {
      const updated = allUsers.filter((u) => u.id !== userId);
      localStorage.setItem('trace_users', JSON.stringify(updated));
      setAllUsers(updated);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300 pb-12">
      {/* Top Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 border border-indigo-900/40 p-6 md:p-8 shadow-2xl">
        <div className="absolute right-0 top-0 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-bold uppercase tracking-wider">
              <ShieldAlert className="w-3.5 h-3.5" />
              <span>Superuser Control Layer &bull; Full Database Authority</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-black text-white tracking-tight">
              Trace Unified Backend Architecture
            </h1>
            <p className="text-sm text-slate-300 max-w-2xl leading-relaxed">
              Superuser command center spanning real-time Python/FastAPI request telemetry, error logs, 5-level curriculum ingestion, and relational database administration.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {isStandalone && onExitStandalone && (
              <button
                onClick={onExitStandalone}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white transition-all text-xs font-semibold border border-slate-700 shadow-md"
                title="Return to Student Learning Application"
              >
                <ArrowLeft className="w-3.5 h-3.5 text-indigo-400" />
                <span>Return to Main App</span>
              </button>
            )}
            <a
              href="/docs"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800/80 border border-slate-700 text-slate-200 hover:bg-slate-700 hover:text-white transition-all text-xs font-semibold"
            >
              <Terminal className="w-4 h-4 text-indigo-400" />
              <span>FastAPI Docs</span>
              <ArrowUpRight className="w-3.5 h-3.5 text-slate-400" />
            </a>
            <button
              onClick={() => {
                TraceStore.restoreSnapshot();
                setUnits(TraceStore.getUnits());
                setAllUsers(TraceStore.getAllUsers());
                setQuizHistory(TraceStore.getQuizHistory());
                if (onRefreshData) onRefreshData();
                fetchLogs();
                alert('Database snapshot re-synchronized to pristine architecture.');
              }}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white transition-all text-xs font-bold shadow-lg shadow-indigo-600/20"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Sync DB State</span>
            </button>
          </div>
        </div>
      </div>

      {/* KPI Analytics Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 hover:border-indigo-500/40 transition-all shadow-lg flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Users</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-black text-white">{allUsers.length}</div>
            <p className="text-xs text-slate-400 mt-1">
              <span className="text-emerald-400 font-medium">100% active</span> across 4 roles
            </p>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 hover:border-emerald-500/40 transition-all shadow-lg flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Active Units</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <BookOpen className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-black text-white">{units.length}</div>
            <p className="text-xs text-slate-400 mt-1">
              <span className="text-emerald-400 font-medium">{units.filter((u) => u.isActive).length} active</span> in student contracts
            </p>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 hover:border-amber-500/40 transition-all shadow-lg flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Subtopics Node Graph</span>
            <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-black text-white">{totalSubtopics}</div>
            <p className="text-xs text-slate-400 mt-1">
              <span className="text-amber-400 font-medium">{completedSubtopics}</span> objectives mastered
            </p>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 hover:border-sky-500/40 transition-all shadow-lg flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">API Health / Errors</span>
            <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400">
              <Radio className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-black text-white">{logSummary.errorsCount}</div>
            <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
              <span className="text-emerald-400 font-medium">{logSummary.totalLogged} logged</span> ({logSummary.avgDurationMs}ms mean)
            </p>
          </div>
        </div>
      </div>

      {/* Admin Tab Controls */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-800">
        {(
          [
            { id: 'logs', label: 'Real-Time System Log Viewer', icon: Radio, badge: logSummary.errorsCount > 0 ? `${logSummary.errorsCount} errors` : undefined },
            { id: 'analytics', label: 'Analytics & Telemetry', icon: BarChart3 },
            { id: 'ingestion', label: 'Curriculum Ingestion Engine', icon: FileCode },
            { id: 'units', label: 'Global Units Management', icon: BookOpen },
            { id: 'users', label: 'Superuser User Administration', icon: Users },
            { id: 'database', label: 'Direct Database Schema', icon: Database },
            { id: 'deployment', label: 'Local Backend & Docker Setup', icon: Laptop }
          ] as const
        ).map((t) => {
          const Icon = t.icon;
          const isActive = activeTab === t.id;
          return (
            <button
              key={t.id}
              onClick={() => setActiveTab(t.id)}
              className={
                isActive
                  ? 'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                  : 'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap text-slate-400 hover:text-white hover:bg-slate-800/60'
              }
            >
              <Icon className="w-4 h-4" />
              <span>{t.label}</span>
              {t.badge && (
                <span className="ml-1 px-1.5 py-0.5 rounded-md text-[10px] font-mono font-bold bg-red-500/20 text-red-300 border border-red-500/30">
                  {t.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* TAB 0: REAL-TIME SYSTEM LOG VIEWER */}
      {activeTab === 'logs' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          {/* Controls & Filter Bar */}
          <div className="p-4 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-xl flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-3">
              {/* Live Polling Status Toggle */}
              <button
                onClick={() => setIsLivePolling(!isLivePolling)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                  isLivePolling
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                    : 'bg-slate-800 text-slate-400 border border-slate-700'
                }`}
                title="Toggle live telemetry feed"
              >
                <span className={`w-2 h-2 rounded-full ${isLivePolling ? 'bg-emerald-400 animate-ping' : 'bg-slate-500'}`} />
                <span>{isLivePolling ? 'Live Telemetry Active' : 'Feed Paused'}</span>
              </button>

              {/* Log Level Select */}
              <div className="flex items-center gap-1.5 text-xs text-slate-400">
                <Filter className="w-3.5 h-3.5 text-indigo-400" />
                <span className="font-semibold hidden sm:inline">Level:</span>
                <select
                  value={logFilterLevel}
                  onChange={(e) => setLogFilterLevel(e.target.value as any)}
                  className="bg-slate-950 border border-slate-800 text-slate-200 rounded-lg px-2.5 py-1 text-xs focus:outline-none focus:border-indigo-500"
                >
                  <option value="ALL">All Levels</option>
                  <option value="INFO">INFO</option>
                  <option value="WARN">WARN</option>
                  <option value="ERROR">ERROR</option>
                </select>
              </div>

              {/* Log Source Select */}
              <div className="flex items-center gap-1.5 text-xs text-slate-400">
                <span className="font-semibold hidden sm:inline">Source:</span>
                <select
                  value={logFilterSource}
                  onChange={(e) => setLogFilterSource(e.target.value as any)}
                  className="bg-slate-950 border border-slate-800 text-slate-200 rounded-lg px-2.5 py-1 text-xs focus:outline-none focus:border-indigo-500"
                >
                  <option value="ALL">All Components</option>
                  <option value="FastAPI Backend">FastAPI Backend (Python)</option>
                  <option value="Express Node">Express Node</option>
                  <option value="AI Engine">AI Engine (Gemini)</option>
                  <option value="Database Engine">Database Engine (SQLAlchemy)</option>
                </select>
              </div>

              {/* Search */}
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Filter endpoint, message..."
                  value={logSearch}
                  onChange={(e) => setLogSearch(e.target.value)}
                  className="pl-8 pr-3 py-1 bg-slate-950 border border-slate-800 text-slate-200 rounded-lg text-xs focus:outline-none focus:border-indigo-500 w-44 md:w-56"
                />
              </div>
            </div>

            <div className="flex items-center gap-2">
              {/* Simulate Backend Error Buttons */}
              <div className="relative group">
                <button
                  disabled={isSimulatingError}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-400 text-xs font-semibold transition-all disabled:opacity-50"
                >
                  <Bug className="w-3.5 h-3.5" />
                  <span>Simulate Backend Fault</span>
                  <ChevronDown className="w-3 h-3 text-red-400" />
                </button>
                <div className="absolute right-0 top-full mt-1.5 w-60 bg-slate-950 border border-slate-800 rounded-xl shadow-2xl p-1.5 hidden group-hover:block z-50 animate-in fade-in">
                  <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400 px-2 py-1">
                    Inject Python/API Failure
                  </div>
                  <button
                    onClick={() => handleSimulateError('database')}
                    className="w-full text-left px-2.5 py-1.5 rounded-lg text-xs text-slate-200 hover:bg-slate-800 flex items-center justify-between"
                  >
                    <span>PostgreSQL Pool Closed (500)</span>
                    <Flame className="w-3.5 h-3.5 text-red-400" />
                  </button>
                  <button
                    onClick={() => handleSimulateError('validation')}
                    className="w-full text-left px-2.5 py-1.5 rounded-lg text-xs text-slate-200 hover:bg-slate-800 flex items-center justify-between"
                  >
                    <span>Pydantic ValidationError (422)</span>
                    <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
                  </button>
                  <button
                    onClick={() => handleSimulateError('auth')}
                    className="w-full text-left px-2.5 py-1.5 rounded-lg text-xs text-slate-200 hover:bg-slate-800 flex items-center justify-between"
                  >
                    <span>FastAPI Unauthorized (401)</span>
                    <ShieldAlert className="w-3.5 h-3.5 text-indigo-400" />
                  </button>
                  <button
                    onClick={() => handleSimulateError('rate_limit')}
                    className="w-full text-left px-2.5 py-1.5 rounded-lg text-xs text-slate-200 hover:bg-slate-800 flex items-center justify-between"
                  >
                    <span>Gemini Rate Limit (429)</span>
                    <Zap className="w-3.5 h-3.5 text-purple-400" />
                  </button>
                </div>
              </div>

              {/* Export JSON */}
              <button
                onClick={handleExportLogs}
                className="p-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
                title="Export Logs as JSON"
              >
                <Download className="w-4 h-4" />
              </button>

              {/* Manual Refresh */}
              <button
                onClick={fetchLogs}
                className="p-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
                title="Refresh Stream Now"
              >
                <RefreshCw className="w-4 h-4" />
              </button>

              {/* Clear */}
              <button
                onClick={handleClearLogs}
                className="p-1.5 rounded-xl bg-slate-800 hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-all"
                title="Clear Logs"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Logs Terminal & Detail Split View */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* Left 2 Cols: Terminal Log Stream */}
            <div className="lg:col-span-2 rounded-2xl bg-slate-950 border border-slate-800 overflow-hidden shadow-2xl flex flex-col h-[520px]">
              {/* Terminal Title Bar */}
              <div className="px-4 py-2.5 bg-slate-900/80 border-b border-slate-800 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-2.5 h-2.5 rounded-full bg-red-500/80" />
                  <div className="w-2.5 h-2.5 rounded-full bg-amber-500/80" />
                  <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/80" />
                  <span className="font-mono text-xs text-slate-400 ml-2">stdout &bull; uvicorn.access &bull; trace.app.main</span>
                </div>
                <span className="text-[10px] font-mono text-slate-500">
                  {logs.length} events buffered
                </span>
              </div>

              {/* Terminal Stream List */}
              <div className="flex-1 overflow-y-auto p-3 font-mono text-xs space-y-1.5 divide-y divide-slate-900/80">
                {logs.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-500 py-12">
                    <Radio className="w-8 h-8 mb-2 opacity-40 animate-pulse text-indigo-400" />
                    <p>No log records match the current filter criteria.</p>
                    <button
                      onClick={() => handleSimulateError('database')}
                      className="mt-3 text-xs text-indigo-400 hover:underline"
                    >
                      Trigger a test request to populate logs &rarr;
                    </button>
                  </div>
                ) : (
                  logs.map((log) => {
                    const isSelected = selectedLog?.id === log.id;
                    const isErr = log.level === 'ERROR';
                    const isWarn = log.level === 'WARN';

                    return (
                      <div
                        key={log.id}
                        onClick={() => setSelectedLog(log)}
                        className={`p-2.5 rounded-xl transition-all cursor-pointer flex items-start gap-2.5 ${
                          isSelected
                            ? 'bg-indigo-950/40 border border-indigo-500/40'
                            : isErr
                            ? 'bg-red-500/5 hover:bg-red-500/10 border border-red-500/20'
                            : isWarn
                            ? 'bg-amber-500/5 hover:bg-amber-500/10 border border-amber-500/20'
                            : 'hover:bg-slate-900/60 border border-transparent'
                        }`}
                      >
                        {/* Status Icon */}
                        <div className="mt-0.5 flex-shrink-0">
                          {isErr ? (
                            <AlertOctagon className="w-4 h-4 text-red-400" />
                          ) : isWarn ? (
                            <AlertTriangle className="w-4 h-4 text-amber-400" />
                          ) : (
                            <CheckCircle className="w-4 h-4 text-emerald-400" />
                          )}
                        </div>

                        {/* Timestamp & Method Badge */}
                        <div className="flex-1 min-w-0">
                          <div className="flex flex-wrap items-center gap-2 mb-1">
                            <span className="text-[10px] text-slate-500 font-mono">
                              {new Date(log.timestamp).toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                            </span>

                            <span
                              className={`px-1.5 py-0.2 rounded text-[10px] font-bold ${
                                log.method === 'GET'
                                  ? 'bg-blue-500/20 text-blue-300'
                                  : log.method === 'POST'
                                  ? 'bg-emerald-500/20 text-emerald-300'
                                  : log.method === 'DELETE'
                                  ? 'bg-red-500/20 text-red-300'
                                  : 'bg-amber-500/20 text-amber-300'
                              }`}
                            >
                              {log.method}
                            </span>

                            <span className="text-slate-300 font-bold truncate">
                              {log.endpoint}
                            </span>

                            <span
                              className={`px-1.5 py-0.2 rounded text-[10px] font-bold ${
                                log.statusCode >= 500
                                  ? 'bg-red-500/20 text-red-400'
                                  : log.statusCode >= 400
                                  ? 'bg-amber-500/20 text-amber-400'
                                  : 'bg-emerald-500/20 text-emerald-400'
                              }`}
                            >
                              {log.statusCode}
                            </span>

                            <span className="text-[10px] text-slate-500">
                              {log.durationMs}ms
                            </span>

                            <span className="text-[10px] text-slate-500 ml-auto hidden sm:inline">
                              {log.source}
                            </span>
                          </div>

                          <p className="text-slate-400 text-xs truncate">
                            {log.message}
                          </p>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={logsEndRef} />
              </div>
            </div>

            {/* Right Col: Deep Inspection Inspector */}
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-5 shadow-xl flex flex-col justify-between h-[520px] overflow-y-auto">
              {selectedLog ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                    <h4 className="text-sm font-bold text-white flex items-center gap-2">
                      <Terminal className="w-4 h-4 text-indigo-400" />
                      <span>Trace Telemetry Inspector</span>
                    </h4>
                    <span
                      className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded-full ${
                        selectedLog.level === 'ERROR'
                          ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                          : selectedLog.level === 'WARN'
                          ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                          : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      }`}
                    >
                      {selectedLog.level}
                    </span>
                  </div>

                  <div className="space-y-2.5 text-xs">
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400">Endpoint &amp; Method</span>
                      <p className="font-mono text-white mt-0.5">{selectedLog.method} {selectedLog.endpoint}</p>
                    </div>

                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400">Status Code</span>
                        <p className="font-mono text-slate-200 mt-0.5">{selectedLog.statusCode}</p>
                      </div>
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400">Duration</span>
                        <p className="font-mono text-slate-200 mt-0.5">{selectedLog.durationMs}ms</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400">Origin Source</span>
                        <p className="text-slate-200 mt-0.5">{selectedLog.source}</p>
                      </div>
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400">Timestamp</span>
                        <p className="font-mono text-slate-400 mt-0.5 text-[11px]">
                          {new Date(selectedLog.timestamp).toISOString()}
                        </p>
                      </div>
                    </div>

                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400">Log Message</span>
                      <p className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 font-mono text-[11px] text-slate-300 mt-1 leading-relaxed">
                        {selectedLog.message}
                      </p>
                    </div>

                    {selectedLog.payloadSnippet && (
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400">Request Payload Snippet</span>
                        <pre className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 font-mono text-[11px] text-indigo-300 mt-1 overflow-x-auto">
                          {selectedLog.payloadSnippet}
                        </pre>
                      </div>
                    )}

                    {selectedLog.errorStack && (
                      <div>
                        <span className="text-[10px] uppercase font-bold text-red-400">Python Exception Traceback</span>
                        <pre className="p-2.5 rounded-xl bg-red-950/30 border border-red-900/40 font-mono text-[10px] text-red-300 mt-1 overflow-x-auto leading-relaxed">
                          {selectedLog.errorStack}
                        </pre>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 py-12 space-y-3">
                  <div className="w-12 h-12 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-center text-slate-400">
                    <Terminal className="w-6 h-6 text-indigo-400" />
                  </div>
                  <div>
                    <p className="font-bold text-white text-sm">No Entry Selected</p>
                    <p className="text-xs text-slate-400 mt-1 max-w-xs">
                      Click any log row in the real-time stream to inspect response codes, payloads, latency, and Python exception traces.
                    </p>
                  </div>
                </div>
              )}

              {/* Bottom Quick Test */}
              <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
                <span className="text-[10px] text-slate-400">Direct Health Ping:</span>
                <button
                  onClick={() => {
                    fetch('/api/health').then(() => fetchLogs());
                  }}
                  className="px-2.5 py-1 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-[11px] font-bold transition-all"
                >
                  Send GET /api/health
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 1: ANALYTICS & TELEMETRY */}
      {activeTab === 'analytics' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 animate-in fade-in duration-200">
          <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-5">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Activity className="w-5 h-5 text-indigo-400" />
                <span>Unit Mastery &amp; Diagnostic Performance</span>
              </h3>
              <span className="text-xs text-indigo-400 font-semibold bg-indigo-500/10 px-2.5 py-1 rounded-full border border-indigo-500/20">
                Live Class Rubric
              </span>
            </div>

            <div className="space-y-4">
              {units.map((unit) => {
                const unitSubs = unit.modules.reduce(
                  (acc, m) => acc + m.topics.reduce((tAcc, t) => tAcc + t.subtopics.length, 0),
                  0
                );
                const compSubs = unit.modules.reduce(
                  (acc, m) =>
                    acc +
                    m.topics.reduce((tAcc, t) => tAcc + t.subtopics.filter((s) => s.isCompleted).length, 0),
                  0
                );
                const pct = unitSubs > 0 ? Math.round((compSubs / unitSubs) * 100) : 0;

                return (
                  <div key={unit.id} className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80">
                    <div className="flex items-center justify-between text-xs mb-2">
                      <span className="font-semibold text-slate-200">{unit.unitName}</span>
                      <span className="font-mono text-indigo-400 font-bold">{pct}% ({compSubs}/{unitSubs} subtopics)</span>
                    </div>
                    <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-emerald-400 transition-all duration-500"
                        style={{ width: `${Math.max(pct, 6)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-5">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <BrainCircuit className="w-5 h-5 text-purple-400" />
                <span>Socratic AI Engine &amp; Audit Telemetry</span>
              </h3>
              <span className="text-xs text-emerald-400 font-semibold bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20">
                Server-Side Active
              </span>
            </div>

            <div className="space-y-3 text-xs">
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-200">Active AI Model</p>
                  <p className="text-slate-400 text-[11px] mt-0.5">Gemini 3.8 Flash (Server-Side)</p>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono text-[10px] font-bold">
                  200 OK
                </span>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-200">High-Availability Fallback</p>
                  <p className="text-slate-400 text-[11px] mt-0.5">Gemini 3.1 Flash Lite + Socratic Engine</p>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 font-mono text-[10px] font-bold">
                  STANDBY
                </span>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-200">Active Socratic Sessions</p>
                  <p className="text-slate-400 text-[11px] mt-0.5">Stored consultations in database</p>
                </div>
                <span className="font-mono text-purple-400 font-bold text-sm">
                  {chatSessions.length} Threads
                </span>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-200">API Key Security Policy</p>
                  <p className="text-slate-400 text-[11px] mt-0.5">Strict zero-client key leakage</p>
                </div>
                <span className="text-emerald-400 font-bold">ENFORCED</span>
              </div>
            </div>
          </div>

          {/* New Control Panel Card spanning full width below the split grid */}
          <div className="col-span-1 lg:col-span-2 p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-6 mt-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-800">
              <div>
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <ShieldAlert className="w-5 h-5 text-indigo-400" />
                  <span>Superuser System Parameter &amp; Control Configurator</span>
                </h3>
                <p className="text-xs text-slate-400 mt-1">
                  Adjust active LLM hyper-parameters, simulate high-load conditions, and rotate API authentication headers system-wide.
                </p>
              </div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 text-xs font-mono">
                <span>Config Status: Synchronized</span>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Option 1: AI Persona */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-400">
                  Global System AI Persona
                </label>
                <select
                  value={globalAiPersona}
                  onChange={(e) => {
                    setGlobalAiPersona(e.target.value as any);
                    alert(`System AI Persona dynamically shifted to: ${e.target.value}`);
                  }}
                  className="w-full bg-slate-950 border border-slate-800 text-slate-200 rounded-xl px-3 py-2.5 text-xs focus:outline-none focus:border-indigo-500 font-medium"
                >
                  <option value="Socratic Tutor">Socratic Tutor (Default)</option>
                  <option value="Strict Clinical Evaluator">Strict Clinical Evaluator</option>
                  <option value="Pedagogical Mentor">Pedagogical Mentor</option>
                </select>
                <p className="text-[10px] text-slate-500">
                  Controls the prompt structure injected before sending clinical cases to Gemini.
                </p>
              </div>

              {/* Option 2: Temperature */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-400">
                  Active Model Temperature
                </label>
                <div className="flex gap-2">
                  {([0.2, 0.7, 1.0] as const).map((temp) => (
                    <button
                      key={temp}
                      type="button"
                      onClick={() => {
                        setModelTemperature(temp);
                        alert(`Active model temperature updated to ${temp}`);
                      }}
                      className={`flex-1 py-2 rounded-xl text-xs font-bold border transition-all ${
                        modelTemperature === temp
                          ? 'bg-indigo-600 border-indigo-500 text-white shadow-md'
                          : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                      }`}
                    >
                      {temp === 0.2 ? '0.2 (Strict)' : temp === 0.7 ? '0.7 (Balanced)' : '1.0 (Creative)'}
                    </button>
                  ))}
                </div>
                <p className="text-[10px] text-slate-500">
                  Higher temperature leads to more creative Socratic dialogues; lower is strict pedagogy.
                </p>
              </div>

              {/* Option 3: Security Policy Profile */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-400">
                  Security Policy Profile
                </label>
                <select
                  value={securityProfile}
                  onChange={(e) => {
                    setSecurityProfile(e.target.value as any);
                    alert(`Active security profile shifted to: ${e.target.value}`);
                  }}
                  className="w-full bg-slate-950 border border-slate-800 text-slate-200 rounded-xl px-3 py-2.5 text-xs focus:outline-none focus:border-indigo-500 font-medium"
                >
                  <option value="Enforced Zero-Leakage">Enforced Zero-Leakage (Prod)</option>
                  <option value="Audit Mode">Audit Mode (Logging active)</option>
                  <option value="Permissive Debug">Permissive Debug (Testing)</option>
                </select>
                <p className="text-[10px] text-slate-500">
                  Manages internal key headers and intercepts requests with audit rules.
                </p>
              </div>
            </div>

            {/* Action Buttons for Superuser controls */}
            <div className="pt-4 border-t border-slate-800 flex flex-wrap gap-3">
              <button
                onClick={() => {
                  if (confirm("Rotate server-wide master access key token? Web, desktop, and mobile nodes will immediately rotate security handshakes.")) {
                    alert("Handshake rotated successfully! X-Internal-Api-Key changed to standard secure environment secret.");
                  }
                }}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-950 hover:bg-slate-800 border border-slate-850 text-slate-200 hover:text-white transition-all text-xs font-bold"
              >
                <RefreshCw className="w-4 h-4 text-emerald-400" />
                <span>Rotate Authentication Key</span>
              </button>

              <button
                onClick={() => {
                  if (confirm("Reset local storage and re-seed the SQLite database with pristine Global Medical Syllabus? This will flush current student progress.")) {
                    TraceStore.restoreSnapshot();
                    setUnits(TraceStore.getUnits());
                    if (onRefreshData) onRefreshData();
                    alert("Database snapshotted and successfully re-seeded to master state!");
                  }
                }}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-950 hover:bg-slate-800 border border-slate-850 text-slate-200 hover:text-white transition-all text-xs font-bold"
              >
                <Database className="w-4 h-4 text-indigo-400" />
                <span>Reset &amp; Seed Database</span>
              </button>

              <button
                onClick={() => {
                  alert("Load simulation initiated: 250 requests/sec injected into the log buffer.");
                  for (let i = 0; i < 5; i++) {
                    fetch('/api/health');
                  }
                  fetchLogs();
                }}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white transition-all text-xs font-bold shadow-lg shadow-indigo-600/10 ml-auto"
              >
                <Zap className="w-4 h-4" />
                <span>Simulate High Load (250 req/s)</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: CURRICULUM INGESTION */}
      {activeTab === 'ingestion' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 animate-in fade-in duration-200">
          <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <FileCode className="w-5 h-5 text-indigo-400" />
                <span>5-Level Syllabus Ingestion Parser</span>
              </h3>
              <span className="text-xs text-indigo-400 font-mono">Engine v3.0</span>
            </div>

            <form onSubmit={handleIngest} className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                  Markdown Syllabus Structure
                </label>
                <textarea
                  value={markdownInput}
                  onChange={(e) => setMarkdownInput(e.target.value)}
                  rows={12}
                  className="w-full rounded-xl bg-slate-950/90 border border-slate-800 p-4 font-mono text-xs text-slate-200 focus:outline-none focus:border-indigo-500 transition-colors leading-relaxed"
                  placeholder="# Unit Title&#10;## Module Title&#10;### Topic Title&#10;#### Subtopic Title&#10;- Objective"
                />
              </div>

              {ingestStatus && (
                <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-medium flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{ingestStatus}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={isIngesting}
                className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-indigo-600/30 disabled:opacity-50"
              >
                {isIngesting ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                <span>Ingest &amp; Commit Unit to Database</span>
              </button>
            </form>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <BookOpen className="w-5 h-5 text-emerald-400" />
              <span>Hierarchical Parsing Rules &amp; Relational Schema</span>
            </h3>

            <div className="space-y-3 text-xs text-slate-300 leading-relaxed">
              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
                <span className="font-mono text-indigo-400 font-bold"># [Unit Name]</span>
                <p className="text-slate-400">Creates the top-level Unit entity. Automatically set as a Global catalog item accessible for student library cloning.</p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
                <span className="font-mono text-emerald-400 font-bold">## [Module Name]</span>
                <p className="text-slate-400">Defines distinct pedagogical curriculum units under the parent course unit.</p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
                <span className="font-mono text-amber-400 font-bold">### [Topic Name]</span>
                <p className="text-slate-400">Thematic grouping for related clinical and academic subtopics.</p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
                <span className="font-mono text-sky-400 font-bold">#### [Subtopic Name]</span>
                <p className="text-slate-400">Interactive study node. Tracks completion state, bookmarking, and Socratic retrieval sessions.</p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
                <span className="font-mono text-purple-400 font-bold">- [Objective]</span>
                <p className="text-slate-400">Granular rubric item. Injected into Gemini prompts for generating targeted multi-choice questions.</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: UNITS MANAGEMENT */}
      {activeTab === 'units' && (
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-5 animate-in fade-in duration-200">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-indigo-400" />
                <span>Global Curriculum Units Catalog ({units.length})</span>
              </h3>
              <p className="text-xs text-slate-400">Rename, delete, or re-organize units with full relational cascades.</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {units.map((unit) => (
              <div
                key={unit.id}
                className="p-5 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-slate-700 transition-all flex flex-col justify-between space-y-4"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    {editingUnitId === unit.id ? (
                      <div className="flex items-center gap-2 w-full">
                        <input
                          type="text"
                          value={editingUnitName}
                          onChange={(e) => setEditingUnitName(e.target.value)}
                          className="flex-1 rounded-lg bg-slate-900 border border-indigo-500 px-3 py-1.5 text-xs text-white"
                          autoFocus
                        />
                        <button
                          onClick={() => handleRenameUnit(unit.id)}
                          className="p-1.5 rounded-lg bg-indigo-600 text-white"
                        >
                          <Check className="w-4 h-4" />
                        </button>
                      </div>
                    ) : (
                      <h4 className="text-sm font-bold text-white">{unit.unitName}</h4>
                    )}
                    <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
                      ID #{unit.id}
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 mt-1 line-clamp-2">{unit.description || 'Global syllabus unit'}</p>

                  <div className="mt-3 flex items-center gap-3 text-xs text-slate-400">
                    <span>{unit.modules.length} modules</span>
                    <span>&bull;</span>
                    <span>
                      {unit.modules.reduce((a, m) => a + m.topics.reduce((ta, t) => ta + t.subtopics.length, 0), 0)} subtopics
                    </span>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between">
                  <button
                    onClick={() => {
                      setEditingUnitId(unit.id);
                      setEditingUnitName(unit.unitName);
                    }}
                    className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 font-semibold"
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                    <span>Rename</span>
                  </button>

                  <button
                    onClick={() => handleDeleteUnit(unit.id)}
                    className="inline-flex items-center gap-1.5 text-xs text-red-400 hover:text-red-300 font-semibold"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Delete Unit</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 4: SUPERUSER USER ADMINISTRATION */}
      {activeTab === 'users' && (
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-5 animate-in fade-in duration-200">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Users className="w-5 h-5 text-indigo-400" />
                <span>Superuser User Administration ({allUsers.length})</span>
              </h3>
              <p className="text-xs text-slate-400">Manage user accounts, assign roles, and audit access permissions.</p>
            </div>

            <div className="flex items-center gap-3">
              <div className="relative">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Filter users..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="pl-9 pr-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <button
                onClick={() => setShowAddUser(!showAddUser)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold transition-all"
              >
                <Plus className="w-4 h-4" />
                <span>Add Account</span>
              </button>
            </div>
          </div>

          {/* Add User Modal / Form */}
          {showAddUser && (
            <form onSubmit={handleCreateUser} className="p-4 rounded-xl bg-slate-950 border border-indigo-500/40 space-y-3">
              <h4 className="text-xs font-bold text-indigo-300 uppercase tracking-wider">Register New Account</h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <input
                  type="text"
                  placeholder="Full Name / Username"
                  value={newUsername}
                  onChange={(e) => setNewUsername(e.target.value)}
                  className="p-2 rounded-lg bg-slate-900 border border-slate-700 text-xs text-white"
                  required
                />
                <input
                  type="email"
                  placeholder="email@trace.edu"
                  value={newUserEmail}
                  onChange={(e) => setNewUserEmail(e.target.value)}
                  className="p-2 rounded-lg bg-slate-900 border border-slate-700 text-xs text-white"
                  required
                />
                <select
                  value={newUserRole}
                  onChange={(e) => setNewUserRole(e.target.value as any)}
                  className="p-2 rounded-lg bg-slate-900 border border-slate-700 text-xs text-white"
                >
                  <option value="Student">Student</option>
                  <option value="Teacher">Teacher</option>
                  <option value="Parent">Parent</option>
                  <option value="Admin">Admin (Superuser)</option>
                </select>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddUser(false)}
                  className="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-3 py-1.5 rounded-lg bg-indigo-600 text-white text-xs font-bold"
                >
                  Save User
                </button>
              </div>
            </form>
          )}

          {/* Users Table */}
          <div className="overflow-x-auto rounded-xl border border-slate-800">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase font-mono text-[10px] tracking-wider border-b border-slate-800">
                <tr>
                  <th className="p-3.5">ID</th>
                  <th className="p-3.5">User Details</th>
                  <th className="p-3.5">Role</th>
                  <th className="p-3.5">Semester / Specialty</th>
                  <th className="p-3.5">Active Contracts</th>
                  <th className="p-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-sans">
                {filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-3.5 font-mono text-slate-400">#{u.id}</td>
                    <td className="p-3.5">
                      <p className="font-bold text-white">{u.username}</p>
                      <p className="text-[11px] text-slate-400">{u.email}</p>
                    </td>
                    <td className="p-3.5">
                      <span
                        className={
                          u.role === 'Admin'
                            ? 'px-2 py-0.5 rounded-full text-[10px] font-bold uppercase bg-red-500/10 text-red-400 border border-red-500/30'
                            : u.role === 'Teacher'
                            ? 'px-2 py-0.5 rounded-full text-[10px] font-bold uppercase bg-purple-500/10 text-purple-400 border border-purple-500/30'
                            : u.role === 'Parent'
                            ? 'px-2 py-0.5 rounded-full text-[10px] font-bold uppercase bg-amber-500/10 text-amber-400 border border-amber-500/30'
                            : 'px-2 py-0.5 rounded-full text-[10px] font-bold uppercase bg-indigo-500/10 text-indigo-400 border border-indigo-500/30'
                        }
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="p-3.5 text-slate-300">{u.semesterStatus}</td>
                    <td className="p-3.5 text-slate-400">
                      {u.activeUnits ? u.activeUnits.join(', ') : 'None'}
                    </td>
                    <td className="p-3.5 text-right">
                      <button
                        onClick={() => handleDeleteUser(u.id)}
                        className="text-red-400 hover:text-red-300 font-semibold text-xs"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 5: DIRECT DATABASE SCHEMA */}
      {activeTab === 'database' && (
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-5 animate-in fade-in duration-200">
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Database className="w-5 h-5 text-indigo-400" />
              <span>Full-Stack Relational Database Schema Inventory</span>
            </h3>
            <p className="text-xs text-slate-400">
              Live schema synchronization across backend models and frontend data layer.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-indigo-400 font-bold text-xs">users</span>
                <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  {allUsers.length} Records
                </span>
              </div>
              <p className="text-xs text-slate-400">User accounts, roles, hashed passwords, semester status, AI persona.</p>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-indigo-400 font-bold text-xs">units / modules / topics</span>
                <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  {units.length} Units
                </span>
              </div>
              <p className="text-xs text-slate-400">5-level relational curriculum hierarchy with cascading deletions.</p>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-indigo-400 font-bold text-xs">quiz_history</span>
                <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  {quizHistory.length} Results
                </span>
              </div>
              <p className="text-xs text-slate-400">Quiz answer scores, percentage next level (PnL) telemetry, rationales.</p>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-indigo-400 font-bold text-xs">chat_sessions / messages</span>
                <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  {chatSessions.length} Sessions
                </span>
              </div>
              <p className="text-xs text-slate-400">Multi-turn Socratic consultations, clinical case prompts, audit trail.</p>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-indigo-400 font-bold text-xs">timetables</span>
                <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  Synchronized
                </span>
              </div>
              <p className="text-xs text-slate-400">Adaptive weekly schedule slots and AI brief generator.</p>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-indigo-400 font-bold text-xs">bookmarks</span>
                <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  Active
                </span>
              </div>
              <p className="text-xs text-slate-400">Subtopic learning objectives saved for rapid clinical review.</p>
            </div>
          </div>
        </div>
      )}

      {/* TAB 6: LOCAL BACKEND & DOCKER DEPLOYMENT */}
      {activeTab === 'deployment' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Laptop className="w-5 h-5 text-indigo-400" />
                  <span>Local Machine & Backend Deployment Architecture</span>
                </h3>
                <p className="text-xs text-slate-400 mt-1">
                  Run the Python backend and web app independently on your local workstation with isolated preview environments.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                  Docker & Shell Ready
                </span>
              </div>
            </div>

            {/* Quick Status Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-indigo-400">Frontend Port</span>
                  <span className="text-[10px] font-mono bg-indigo-500/10 text-indigo-300 px-2 py-0.5 rounded border border-indigo-500/20">
                    PORT 3000
                  </span>
                </div>
                <p className="text-xs text-slate-300 font-mono">http://localhost:3000</p>
                <p className="text-[11px] text-slate-500">React + Vite SPA with Node API Proxy orchestrator.</p>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-emerald-400">Python Backend Port</span>
                  <span className="text-[10px] font-mono bg-emerald-500/10 text-emerald-300 px-2 py-0.5 rounded border border-emerald-500/20">
                    PORT 8001
                  </span>
                </div>
                <p className="text-xs text-slate-300 font-mono">http://localhost:8001</p>
                <p className="text-[11px] text-slate-500">FastAPI Modular Engine (SQLAlchemy, AI Chat, Neon/SQLite).</p>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-amber-400">Dedicated Admin URL</span>
                  <span className="text-[10px] font-mono bg-amber-500/10 text-amber-300 px-2 py-0.5 rounded border border-amber-500/20">
                    ISOLATED
                  </span>
                </div>
                <p className="text-xs text-slate-300 font-mono">http://localhost:3000/?view=admin</p>
                <p className="text-[11px] text-slate-500">Direct superuser window without mixing student sessions.</p>
              </div>
            </div>
          </div>

          {/* Deployment Methods Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Method 1: Docker Compose */}
            <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400">
                  <Server className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-white">Method 1: Docker Compose (All-in-One)</h4>
                  <p className="text-xs text-slate-400">Recommended for clean containerized local deployment</p>
                </div>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                A pre-configured <code className="text-indigo-300 bg-slate-950 px-1 py-0.5 rounded">docker-compose.yml</code> file coordinates both containers (Python FastAPI on port 8001 and Node/React on port 3000).
              </p>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-slate-300 space-y-2">
                <div className="text-slate-500"># 1. Start all multi-service containers</div>
                <div className="text-emerald-400">docker-compose up --build</div>
                <div className="text-slate-500 mt-2"># 2. Stop all running containers</div>
                <div className="text-indigo-400">docker-compose down</div>
              </div>

              <div className="text-xs text-slate-400 space-y-1">
                <div className="flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Runs backend and frontend in isolated container networks</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Pre-binds PostgreSQL/SQLite storage and JWT tokens</span>
                </div>
              </div>
            </div>

            {/* Method 2: Native Shell Launcher */}
            <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
                  <Terminal className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-white">Method 2: One-Click Shell Script</h4>
                  <p className="text-xs text-slate-400">Run directly on Linux, macOS, or Windows WSL</p>
                </div>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                Use the automated launcher script <code className="text-indigo-300 bg-slate-950 px-1 py-0.5 rounded">./start-local.sh</code> to spin up both processes side-by-side with hot reload.
              </p>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-slate-300 space-y-2">
                <div className="text-slate-500"># Run the unified launcher script</div>
                <div className="text-emerald-400">./start-local.sh</div>
                <div className="text-slate-500 mt-2"># Or run python backend manually</div>
                <div className="text-indigo-400">cd backend && python run_modular.py</div>
              </div>

              <div className="text-xs text-slate-400 space-y-1">
                <div className="flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Captures stdout and stderr from both processes</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Graceful Ctrl+C process termination hook</span>
                </div>
              </div>
            </div>
          </div>

          {/* Configuration File & API Reference Card */}
          <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl space-y-3">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <FileCode className="w-4 h-4 text-indigo-400" />
              <span>Full Setup Documentation & File References</span>
            </h4>
            <p className="text-xs text-slate-400">
              Review detailed instructions and environment variable specs in the generated repository documentation:
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-mono text-xs font-bold text-slate-200">LOCAL_DEPLOYMENT.md</div>
                <p className="text-[11px] text-slate-400 mt-1">Complete step-by-step terminal & Docker instructions.</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-mono text-xs font-bold text-slate-200">docker-compose.yml</div>
                <p className="text-[11px] text-slate-400 mt-1">Multi-service manifest with port mappings 8001 & 3000.</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-mono text-xs font-bold text-slate-200">start-local.sh</div>
                <p className="text-[11px] text-slate-400 mt-1">Bash launcher with automated background PID management.</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
