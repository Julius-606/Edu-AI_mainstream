import React, { useState, useEffect } from 'react';
import {
  Menu,
  Sparkles,
  BookOpen,
  MessageSquare,
  Calendar,
  Globe,
  Settings,
  Archive,
  User as UserIcon,
  Bookmark,
  Users,
  Search,
  Bell,
  ShieldAlert,
  Smartphone,
  Tablet,
  Wifi,
  Battery,
  Home,
  CheckCircle2,
  RefreshCw
} from 'lucide-react';
import { User, Unit, UserRole } from './types';
import { TraceStore } from './lib/store';
import { DynamicBackground } from './components/DynamicBackground';
import { NavigationDrawer } from './components/NavigationDrawer';
import { LoginScreen } from './components/LoginScreen';
import { StudentDashboard } from './components/StudentDashboard';
import { LearnScreen } from './components/LearnScreen';
import { QuizInterface } from './components/QuizInterface';
import { ChatInterface } from './components/ChatInterface';
import { TimetableTab } from './components/TimetableTab';
import { LibraryScreen } from './components/LibraryScreen';
import { UnitOutlineScreen } from './components/UnitOutlineScreen';
import { LearningRepositoryScreen } from './components/LearningRepositoryScreen';
import { ConnectTab } from './components/ConnectTab';
import { AdminDashboard } from './components/AdminDashboard';
import { TeacherDashboard } from './components/TeacherDashboard';
import { ParentDashboard } from './components/ParentDashboard';
import { InAppBrowser } from './components/InAppBrowser';
import { AccountModal } from './components/AccountModal';
import { ArchivesModal } from './components/ArchivesModal';
import { ManageUnitsModal } from './components/ManageUnitsModal';
import { SyncModal } from './components/SyncModal';

export const App: React.FC = () => {
  const [user, setUser] = useState<User>(TraceStore.getUser());
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [units, setUnits] = useState<Unit[]>(TraceStore.getUnits());

  // Preview Mode: 'mobile' (Android phone mockup preview) or 'responsive' (full-width)
  const [previewMode, setPreviewMode] = useState<'mobile' | 'responsive'>('mobile');

  // Screen Routing
  const [currentTab, setCurrentTab] = useState<string>('dashboard');
  const [activeUnitId, setActiveUnitId] = useState<number>(1);
  const [activeSubtopicId, setActiveSubtopicId] = useState<number | undefined>(undefined);
  const [quizUnitName, setQuizUnitName] = useState<string>('');
  const [quizTopicName, setQuizTopicName] = useState<string>('');

  // Modals
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isAccountOpen, setIsAccountOpen] = useState(false);
  const [isArchivesOpen, setIsArchivesOpen] = useState(false);
  const [isManageUnitsOpen, setIsManageUnitsOpen] = useState(false);
  const [isSyncOpen, setIsSyncOpen] = useState(false);
  const [browserUrl, setBrowserUrl] = useState<string | null>(null);

  const timetable = TraceStore.getTimetable();

  const handleSelectUnitToLearn = (unitId: number, subtopicId?: number) => {
    setActiveUnitId(unitId);
    setActiveSubtopicId(subtopicId);
    setCurrentTab('learn');
  };

  const handleLaunchQuiz = (unitName: string, topicName?: string) => {
    setQuizUnitName(unitName);
    setQuizTopicName(topicName || '');
    setCurrentTab('quizzes');
  };

  const handleToggleActiveUnit = (unitId: number, isActive: boolean) => {
    const updated = TraceStore.toggleUnitActive(unitId, isActive);
    setUnits(updated);
  };

  const handleRestoreArchivedUnit = (unitId: number) => {
    const updated = TraceStore.toggleUnitActive(unitId, true);
    setUnits(updated);
  };

  const handleChangeRole = (role: UserRole) => {
    const all = TraceStore.getAllUsers();
    let matched = all.find((u) => u.role === role);
    if (!matched && role === 'Admin') {
      matched = {
        id: '4',
        username: 'Admin Root',
        email: 'admin@trace.edu',
        role: 'Admin',
        difficulty: 'Superuser',
        semesterStatus: 'System Administration & Oversight',
        aiPersona: 'System Architect & Lead Consultant',
        sensoryMode: 'Standard',
        activeUnits: ['Biochemistry II', 'General Surgery', 'Internal Medicine', 'Pathology & Diagnostics']
      };
      TraceStore.setUser(matched);
    }
    if (matched) {
      setUser(matched);
      TraceStore.setUser(matched);
      if (role === 'Admin') {
        setCurrentTab('admin');
      } else {
        setCurrentTab('dashboard');
      }
    }
  };

  const handleSnapshotRestored = () => {
    setUnits(TraceStore.getUnits());
    setUser(TraceStore.getUser());
  };

  const activeUnit = units.find((u) => u.id === activeUnitId) || units[0];

  const adminUser: User = {
    id: '4',
    username: 'Admin Root',
    email: 'admin@trace.edu',
    role: 'Admin',
    difficulty: 'Superuser',
    semesterStatus: 'System Administration & Oversight',
    aiPersona: 'System Architect & Lead Consultant',
    sensoryMode: 'Standard',
    activeUnits: ['Biochemistry II', 'General Surgery', 'Internal Medicine', 'Pathology & Diagnostics']
  };

  // Main screen routing element
  const renderMainScreen = () => {
    if (currentTab === 'admin' || user.role === 'Admin') {
      return (
        <AdminDashboard
          currentUser={user.role === 'Admin' ? user : adminUser}
          onNavigateToTab={(t) => setCurrentTab(t as any)}
          onRefreshData={() => setUnits(TraceStore.getUnits())}
        />
      );
    }

    if (user.role === 'Teacher') {
      return <TeacherDashboard user={user} />;
    }

    if (user.role === 'Parent') {
      return <ParentDashboard user={user} />;
    }

    // Student Role
    return (
      <>
        {currentTab === 'dashboard' && (
          <StudentDashboard
            user={user}
            units={units}
            timetable={timetable}
            onOpenUnit={(unitId) => handleSelectUnitToLearn(unitId)}
            onOpenConsultation={() => setCurrentTab('consultations')}
            onOpenQuizzes={() => setCurrentTab('quizzes')}
            onOpenTimetable={() => setCurrentTab('timetable')}
            onOpenLibrary={() => setCurrentTab('library')}
          />
        )}

        {currentTab === 'learn' && activeUnit && (
          <LearnScreen
            unit={activeUnit}
            subtopicId={activeSubtopicId}
            onBack={() => setCurrentTab('dashboard')}
            onLaunchQuiz={(uName, tName) => handleLaunchQuiz(uName, tName)}
            onOpenBrowser={(url) => setBrowserUrl(url ?? null)}
          />
        )}

        {currentTab === 'quizzes' && (
          <QuizInterface
            units={units}
            initialUnitName={quizUnitName}
            initialTopicName={quizTopicName}
            onBack={() => setCurrentTab('dashboard')}
          />
        )}

        {currentTab === 'consultations' && (
          <ChatInterface user={user} onOpenBrowser={(url) => setBrowserUrl(url ?? null)} />
        )}

        {currentTab === 'timetable' && (
          <TimetableTab
            user={user}
            onOpenUnitByName={(name) => {
              const found = units.find((u) => u.unitName.toLowerCase() === name.toLowerCase());
              if (found) handleSelectUnitToLearn(found.id);
            }}
            onOpenQuizzes={() => setCurrentTab('quizzes')}
          />
        )}

        {currentTab === 'library' && (
          <LibraryScreen
            units={units}
            onToggleEnroll={handleToggleActiveUnit}
            onSelectUnit={(unitId) => {
              setActiveUnitId(unitId);
              setCurrentTab('unit-outline');
            }}
            onBack={() => setCurrentTab('dashboard')}
          />
        )}

        {currentTab === 'unit-outline' && activeUnit && (
          <UnitOutlineScreen
            unit={activeUnit}
            onBack={() => setCurrentTab('library')}
            onOpenSubtopic={(uId, sId) => handleSelectUnitToLearn(uId, sId)}
            onLaunchQuiz={(uName, tName) => handleLaunchQuiz(uName, tName)}
          />
        )}

        {currentTab === 'repository' && (
          <LearningRepositoryScreen
            onBack={() => setCurrentTab('dashboard')}
            onOpenSubtopicByName={(subName) => {
              for (const u of units) {
                for (const m of u.modules) {
                  for (const t of m.topics) {
                    for (const s of t.subtopics) {
                      if (s.name.toLowerCase() === subName.toLowerCase()) {
                        handleSelectUnitToLearn(u.id, s.id);
                        return;
                      }
                    }
                  }
                }
              }
              setCurrentTab('dashboard');
            }}
          />
        )}

        {currentTab === 'connect' && <ConnectTab user={user} />}
      </>
    );
  };

  // App Content (Navigation, Header, Screen Content, Bottom Nav)
  const appContent = (
    <div className="flex flex-col h-full text-slate-100 bg-slate-950 font-sans selection:bg-indigo-500/30 selection:text-indigo-200">
      <DynamicBackground />

      {/* Navigation Drawer */}
      <NavigationDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        user={user}
        onSelectTab={(tab) => setCurrentTab(tab)}
        onOpenAccount={() => setIsAccountOpen(true)}
        onOpenArchives={() => setIsArchivesOpen(true)}
        onOpenManageUnits={() => setIsManageUnitsOpen(true)}
        onOpenSync={() => setIsSyncOpen(true)}
        onOpenBrowser={(url) => setBrowserUrl(url || 'https://en.wikipedia.org/wiki/Medicine')}
        onChangeRole={handleChangeRole}
        onLogout={() => setIsLoggedIn(false)}
        onOpenAdminConsole={() => {
          handleChangeRole('Admin');
          setCurrentTab('admin');
        }}
      />

      {/* App Top Bar */}
      <header className="sticky top-0 z-40 bg-slate-950/90 backdrop-blur-xl border-b border-slate-800/80 px-4 h-14 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2.5">
          <button
            onClick={() => setIsDrawerOpen(true)}
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
            title="Navigation Menu"
          >
            <Menu className="w-5 h-5" />
          </button>

          <div
            onClick={() => {
              if (user.role === 'Admin') {
                setCurrentTab('admin');
              } else {
                setCurrentTab('dashboard');
              }
            }}
            className="flex items-center gap-2 cursor-pointer select-none"
          >
            <div className="w-7 h-7 rounded-lg bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-indigo-400 font-black text-xs shadow-sm">
              T
            </div>
            <span className="text-sm font-bold text-white tracking-tight">Trace</span>
            <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-md bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              {user.role}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          {/* Direct Admin Console Switcher */}
          <button
            onClick={() => {
              if (currentTab === 'admin' || user.role === 'Admin') {
                handleChangeRole('Student');
                setCurrentTab('dashboard');
              } else {
                handleChangeRole('Admin');
                setCurrentTab('admin');
              }
            }}
            className={`px-2 py-1 rounded-lg border text-xs font-bold transition-all flex items-center gap-1.5 ${
              currentTab === 'admin' || user.role === 'Admin'
                ? 'bg-red-500/20 text-red-300 border-red-500/40'
                : 'bg-slate-900 text-slate-400 hover:text-red-300 border-slate-800'
            }`}
            title="Toggle Admin Superuser Console"
          >
            <ShieldAlert className="w-3.5 h-3.5 text-red-400" />
            <span className="hidden sm:inline">Admin</span>
          </button>

          <button
            onClick={() => setIsSyncOpen(true)}
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-emerald-300"
            title="Settings & Sync"
          >
            <Settings className="w-4 h-4" />
          </button>

          <button
            onClick={() => setIsAccountOpen(true)}
            className="w-7 h-7 rounded-lg bg-indigo-600/30 border border-indigo-500/40 text-indigo-200 flex items-center justify-center font-bold text-xs"
            title={user.username}
          >
            {user.username.charAt(0)}
          </button>
        </div>
      </header>

      {/* Main Scrollable View Area */}
      <main className="flex-1 overflow-y-auto px-3.5 py-4 pb-20">
        {renderMainScreen()}
      </main>

      {/* Android Mobile Navigation Bar */}
      <nav className="fixed bottom-0 inset-x-0 z-40 bg-slate-950/95 backdrop-blur-xl border-t border-slate-800/90 py-1.5 px-3 flex items-center justify-around shadow-2xl">
        <button
          onClick={() => setCurrentTab('dashboard')}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-all ${
            currentTab === 'dashboard'
              ? 'text-indigo-400 font-bold'
              : 'text-slate-400 hover:text-slate-200 font-medium'
          }`}
        >
          <Home className="w-4 h-4" />
          <span className="text-[10px]">Home</span>
        </button>

        <button
          onClick={() => setCurrentTab('learn')}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-all ${
            currentTab === 'learn'
              ? 'text-indigo-400 font-bold'
              : 'text-slate-400 hover:text-slate-200 font-medium'
          }`}
        >
          <BookOpen className="w-4 h-4" />
          <span className="text-[10px]">Learn</span>
        </button>

        <button
          onClick={() => setCurrentTab('quizzes')}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-all ${
            currentTab === 'quizzes'
              ? 'text-indigo-400 font-bold'
              : 'text-slate-400 hover:text-slate-200 font-medium'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          <span className="text-[10px]">Quizzes</span>
        </button>

        <button
          onClick={() => setCurrentTab('consultations')}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-all ${
            currentTab === 'consultations'
              ? 'text-indigo-400 font-bold'
              : 'text-slate-400 hover:text-slate-200 font-medium'
          }`}
        >
          <MessageSquare className="w-4 h-4" />
          <span className="text-[10px]">Consult</span>
        </button>

        <button
          onClick={() => setCurrentTab('timetable')}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-all ${
            currentTab === 'timetable'
              ? 'text-indigo-400 font-bold'
              : 'text-slate-400 hover:text-slate-200 font-medium'
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span className="text-[10px]">Schedule</span>
        </button>

        <button
          onClick={() => {
            handleChangeRole('Admin');
            setCurrentTab('admin');
          }}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-all ${
            currentTab === 'admin' || user.role === 'Admin'
              ? 'text-red-400 font-bold'
              : 'text-slate-400 hover:text-red-300 font-medium'
          }`}
        >
          <ShieldAlert className="w-4 h-4 text-red-400" />
          <span className="text-[10px]">Admin</span>
        </button>
      </nav>

      {/* Modals */}
      {browserUrl && <InAppBrowser url={browserUrl} onClose={() => setBrowserUrl(null)} />}
      {isAccountOpen && (
        <AccountModal
          user={user}
          onClose={() => setIsAccountOpen(false)}
          onSave={(updated) => {
            setUser(updated);
            TraceStore.setUser(updated);
          }}
        />
      )}
      {isArchivesOpen && (
        <ArchivesModal
          units={units}
          quizHistory={TraceStore.getQuizHistory(user.id)}
          onClose={() => setIsArchivesOpen(false)}
          onRestoreUnit={handleRestoreArchivedUnit}
        />
      )}
      {isManageUnitsOpen && (
        <ManageUnitsModal
          units={units}
          onClose={() => setIsManageUnitsOpen(false)}
          onToggleActive={handleToggleActiveUnit}
        />
      )}
      {isSyncOpen && (
        <SyncModal
          onClose={() => setIsSyncOpen(false)}
          onSnapshotRestored={handleSnapshotRestored}
        />
      )}
    </div>
  );

  if (!isLoggedIn) {
    return (
      <div className="relative min-h-screen text-slate-100 bg-slate-950 font-sans">
        <DynamicBackground />
        <LoginScreen
          onLoginSuccess={(u) => {
            setUser(u);
            TraceStore.setUser(u);
            setIsLoggedIn(true);
            setCurrentTab('dashboard');
          }}
        />
      </div>
    );
  }

  // If in 'mobile' preview mode, wrap the Android App in an interactive phone frame
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center py-2 sm:py-6 px-1 sm:px-4 font-sans selection:bg-indigo-500/30 selection:text-indigo-200">
      {/* Top Preview Control Bar */}
      <div className="w-full max-w-xl mb-3 flex items-center justify-between gap-2 px-2">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-xs font-bold text-slate-300">
            Android App Preview (Trace OS)
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setPreviewMode(previewMode === 'mobile' ? 'responsive' : 'mobile')}
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-semibold text-slate-300 transition-colors"
          >
            {previewMode === 'mobile' ? (
              <>
                <Tablet className="w-3.5 h-3.5 text-indigo-400" />
                <span>Expanded</span>
              </>
            ) : (
              <>
                <Smartphone className="w-3.5 h-3.5 text-indigo-400" />
                <span>Phone View</span>
              </>
            )}
          </button>
        </div>
      </div>

      {previewMode === 'mobile' ? (
        /* Authentic Android Smartphone Device Frame */
        <div className="relative w-full max-w-[420px] h-[860px] bg-slate-950 rounded-[44px] border-[8px] border-slate-800 shadow-[0_25px_70px_rgba(0,0,0,0.85)] flex flex-col overflow-hidden ring-1 ring-slate-700/50">
          {/* Android Status Bar */}
          <div className="h-8 bg-slate-950/95 border-b border-slate-800/50 px-6 flex items-center justify-between text-[11px] font-semibold text-slate-400 select-none z-50 shrink-0">
            <span>9:41</span>
            
            {/* Center Camera Punch Hole */}
            <div className="w-3.5 h-3.5 rounded-full bg-black border border-slate-800 shadow-inner" />

            <div className="flex items-center gap-2">
              <span className="text-[10px] font-bold text-slate-400">5G</span>
              <Wifi className="w-3.5 h-3.5 text-slate-400" />
              <div className="flex items-center gap-1">
                <span className="text-[10px]">98%</span>
                <Battery className="w-3.5 h-3.5 text-emerald-400" />
              </div>
            </div>
          </div>

          {/* Phone Screen Body */}
          <div className="flex-1 relative overflow-hidden flex flex-col">
            {appContent}
          </div>

          {/* Android Navigation Pill Bar */}
          <div className="h-4 bg-slate-950 flex items-center justify-center shrink-0 z-50">
            <div className="w-28 h-1 bg-slate-600/70 rounded-full" />
          </div>
        </div>
      ) : (
        /* Full Expanded View */
        <div className="w-full max-w-7xl flex-1 bg-slate-950 rounded-2xl border border-slate-800 overflow-hidden shadow-2xl">
          {appContent}
        </div>
      )}
    </div>
  );
};
