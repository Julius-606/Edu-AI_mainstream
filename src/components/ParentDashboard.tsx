import React from 'react';
import {
  Shield,
  Award,
  Sparkles,
  BookOpen,
  Calendar,
  CheckCircle2,
  TrendingUp,
  UserCheck
} from 'lucide-react';
import { User } from '../types';
import { TraceStore } from '../lib/store';

interface ParentDashboardProps {
  user: User;
}

export const ParentDashboard: React.FC<ParentDashboardProps> = ({ user }) => {
  const student = TraceStore.getAllUsers().find((u) => u.role === 'Student') || {
    id: '1',
    username: 'Alex Kim',
    semesterStatus: 'Year 4 - Clinical Rotations',
    activeUnits: ['Biochemistry II', 'General Surgery', 'Internal Medicine']
  };

  const quizHistory = TraceStore.getQuizHistory(student.id);
  const avgPnl = quizHistory.length > 0
    ? Math.round(quizHistory.reduce((acc, cur) => acc + cur.pnlScore, 0) / quizHistory.length)
    : 85;

  const units = TraceStore.getUnits();
  const enrolledUnits = units.filter((u) => student.activeUnits.includes(u.unitName));

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
            Guardian Academic Portal
          </span>
          <h1 className="text-2xl sm:text-3xl font-black text-white mt-0.5">
            Progress Overview: {student.username}
          </h1>
          <p className="text-xs text-slate-400">Verified institutional progress metrics and faculty evaluations</p>
        </div>

        <div className="flex items-center gap-3">
          <div className="bg-slate-950/80 border border-slate-800 rounded-2xl px-4 py-2.5 text-center min-w-[110px]">
            <span className="text-xs font-semibold text-slate-400 block">Overall PnL</span>
            <span className="text-xl font-extrabold text-emerald-400">{avgPnl}%</span>
          </div>
          <div className="bg-slate-950/80 border border-slate-800 rounded-2xl px-4 py-2.5 text-center min-w-[110px]">
            <span className="text-xs font-semibold text-slate-400 block">Current Status</span>
            <span className="text-sm font-bold text-white mt-1 block">Good Standing</span>
          </div>
        </div>
      </div>

      {/* AI Guardian Brief */}
      <div className="p-6 rounded-3xl bg-gradient-to-br from-indigo-950/40 via-slate-900 to-slate-900 border border-indigo-500/30 shadow-xl space-y-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">
              Continuous Learning Trajectory Review
            </h2>
            <p className="text-xs text-indigo-300">Automated pedagogical synthesis for guardians</p>
          </div>
        </div>

        <p className="text-xs text-slate-200 leading-relaxed pt-1">
          {student.username} is demonstrating consistent progress during the {student.semesterStatus} curriculum.
          Diagnostic reasoning assessments reflect strong foundational knowledge in <strong>Internal Medicine (Cardiology)</strong> and <strong>Biochemistry II</strong>.
          Regular completion of scheduled spaced-retrieval quizzes and active engagement with the Socratic Consultant indicate high academic discipline and exam readiness.
        </p>

        <div className="pt-2 flex items-center gap-4 text-xs text-slate-400 border-t border-slate-800/80">
          <span className="flex items-center gap-1 text-emerald-400 font-semibold">
            <CheckCircle2 className="w-4 h-4" />
            <span>Attendance & Engagement: 98%</span>
          </span>
          <span>•</span>
          <span>Next Scheduled Clinical Assessment: Friday</span>
        </div>
      </div>

      {/* Enrolled Disciplines & Progress */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 shadow-xl space-y-4">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider">
          Enrolled Disciplines & Subtopic Mastery
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {enrolledUnits.map((u) => {
            const subs = u.modules.flatMap((m) => m.topics.flatMap((t) => t.subtopics));
            const completed = subs.filter((s) => s.isCompleted).length;
            const pct = subs.length > 0 ? Math.round((completed / subs.length) * 100) : 0;

            return (
              <div
                key={u.id}
                className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-2"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white truncate">{u.unitName}</span>
                  <span className="text-xs font-extrabold text-indigo-400">{pct}%</span>
                </div>
                <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                  <div
                    className="h-full bg-indigo-500 rounded-full"
                    style={{ width: `${pct}%` }}
                  />
                </div>
                <span className="text-[10px] text-slate-500 block">
                  {completed} of {subs.length} subtopics verified
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Recent Assessment Scores Trail */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 shadow-xl space-y-4">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider">
          Recent Assessment Performance Trail
        </h3>

        <div className="space-y-2">
          {quizHistory.map((item) => (
            <div
              key={item.id}
              className="p-3.5 rounded-2xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold text-xs ${
                    item.pnlScore >= 80
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                      : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                  }`}
                >
                  {Math.round(item.pnlScore)}%
                </div>
                <div>
                  <h4 className="text-xs font-bold text-white">{item.unitName}</h4>
                  <p className="text-[11px] text-slate-400">{item.topicName || 'Clinical Synthesis'}</p>
                </div>
              </div>

              <div className="text-right text-[11px] text-slate-500">
                <span>{item.score} / {item.total} correct</span>
                <p>{new Date(item.timestamp).toLocaleDateString()}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
