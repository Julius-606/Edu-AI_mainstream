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
  Bell
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
        activeUnits: ['Biochemistry II', 'General Surgery', 'Internal Medicine']
      };
      TraceStore.setUser(matched);
    }
    if (matched) {
      setUser(matched);
      TraceStore.setUser(matched);
      setCurrentTab('dashboard');
    }
  };

  const handleSnapshotRestored = () => {
    setUnits(TraceStore.getUnits());
    setUser(TraceStore.getUser());
  };

  const activeUnit = units.find((u) => u.id === activeUnitId) || units[0];

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

  return (
    <div className="min-h-screen text-slate-100 bg-slate-950 font-sans selection:bg-indigo-500/30 selection:text-indigo-200 relative">
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
      />

      {/* Top Application Header Bar */}
      <header className="sticky top-0 z-40 bg-slate-950/80 backdrop-blur-xl border-b border-slate-800/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
          {/* Left: Drawer Trigger + Brand Logo */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsDrawerOpen(true)}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
              title="Open Navigation Menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            <div
              onClick={() => setCurrentTab('dashboard')}
              className="flex items-center gap-2.5 cursor-pointer select-none"
            >
              <div className="w-8 h-8 rounded-xl bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-indigo-400 font-black text-sm shadow-sm">
                T
              </div>
              <span className="text-base font-extrabold text-white tracking-tight hidden sm:inline">
                Trace Learning System
              </span>
            </div>
          </div>

          {/* Center: Quick Role & Perspective Bar */}
          <div className="hidden md:flex items-center gap-1 bg-slate-900/90 border border-slate-800 rounded-2xl p-1 text-xs">
            <button
              onClick={() => setCurrentTab('dashboard')}
              className={`px-3 py-1.5 rounded-xl font-semibold transition-all ${
                currentTab === 'dashboard'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Dashboard
            </button>
            <button
              onClick={() => setCurrentTab('consultations')}
              className={`px-3 py-1.5 rounded-xl font-semibold transition-all ${
                currentTab === 'consultations'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Consultations
            </button>
            <button
              onClick={() => setCurrentTab('quizzes')}
              className={`px-3 py-1.5 rounded-xl font-semibold transition-all ${
                currentTab === 'quizzes'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Quizzes
            </button>
            <button
              onClick={() => setCurrentTab('timetable')}
              className={`px-3 py-1.5 rounded-xl font-semibold transition-all ${
                currentTab === 'timetable'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Timetable
            </button>
            <button
              onClick={() => setCurrentTab('library')}
              className={`px-3 py-1.5 rounded-xl font-semibold transition-all ${
                currentTab === 'library'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Library
            </button>
          </div>

          {/* Right: Quick Tools + Profile Badge */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setBrowserUrl('https://en.wikipedia.org/wiki/Medicine')}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              title="Trace Reference Browser"
            >
              <Globe className="w-4 h-4" />
            </button>

            <button
              onClick={() => setCurrentTab('repository')}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-amber-300 hover:bg-slate-800 transition-colors"
              title="Saved Bookmarks & Vault"
            >
              <Bookmark className="w-4 h-4" />
            </button>

            <button
              onClick={() => setIsSyncOpen(true)}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-emerald-300 hover:bg-slate-800 transition-colors"
              title="Settings & Sync"
            >
              <Settings className="w-4 h-4" />
            </button>

            <button
              onClick={() => setIsAccountOpen(true)}
              className="flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-colors"
            >
              <div className="w-6 h-6 rounded-lg bg-indigo-600/30 text-indigo-300 flex items-center justify-center font-bold text-xs">
                {user.username.charAt(0)}
              </div>
              <span className="text-xs font-semibold text-slate-200 hidden sm:inline">
                {user.username.split(' ')[0]}
              </span>
              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-full uppercase tracking-wider bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                {user.role}
              </span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Screen Router Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 pt-6">
        {user.role === 'Admin' ? (
          <AdminDashboard currentUser={user} onNavigateToTab={(t) => setCurrentTab(t as any)} onRefreshData={() => setUnits(TraceStore.getUnits())} />
        ) : user.role === 'Teacher' ? (
          <TeacherDashboard user={user} />
        ) : user.role === 'Parent' ? (
          <ParentDashboard user={user} />
        ) : (
          /* Student Role Screens */
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
        )}
      </main>

      {/* In-App Browser Modal */}
      {browserUrl && (
        <InAppBrowser url={browserUrl} onClose={() => setBrowserUrl(null)} />
      )}

      {/* Account Settings Modal */}
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

      {/* Archives Modal */}
      {isArchivesOpen && (
        <ArchivesModal
          units={units}
          quizHistory={TraceStore.getQuizHistory(user.id)}
          onClose={() => setIsArchivesOpen(false)}
          onRestoreUnit={handleRestoreArchivedUnit}
        />
      )}

      {/* Manage Units Modal */}
      {isManageUnitsOpen && (
        <ManageUnitsModal
          units={units}
          onClose={() => setIsManageUnitsOpen(false)}
          onToggleActive={handleToggleActiveUnit}
        />
      )}

      {/* Settings & Sync Modal */}
      {isSyncOpen && (
        <SyncModal
          onClose={() => setIsSyncOpen(false)}
          onSnapshotRestored={handleSnapshotRestored}
        />
      )}
    </div>
  );
};
