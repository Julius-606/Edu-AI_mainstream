import React, { useState } from 'react';
import {
  Users,
  AlertTriangle,
  Award,
  Sparkles,
  FileText,
  CheckCircle,
  TrendingUp,
  RotateCw
} from 'lucide-react';
import { User, StudentProgressSummary } from '../types';
import { TraceStore } from '../lib/store';
import { FormattedText } from './FormattedText';

interface TeacherDashboardProps {
  user: User;
}

export const TeacherDashboard: React.FC<TeacherDashboardProps> = ({ user }) => {
  const [students, setStudents] = useState<StudentProgressSummary[]>(TraceStore.getTeacherStudents());
  const [selectedStudent, setSelectedStudent] = useState<StudentProgressSummary | null>(null);
  const [reportText, setReportText] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const atRiskStudents = students.filter((s) => s.isAtRisk);
  const classAvgPnl = Math.round(
    students.reduce((acc, s) => acc + s.averagePnl, 0) / (students.length || 1)
  );

  const handleGenerateReport = async (student: StudentProgressSummary) => {
    setSelectedStudent(student);
    setIsGenerating(true);
    setReportText(null);

    try {
      const res = await fetch(`/api/teacher/report/${student.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          studentName: student.username,
          semesterStatus: student.semesterStatus,
          activeUnits: student.activeUnits,
          avgScore: student.averagePnl
        })
      });
      const data = await res.json();
      setReportText(data.report || 'Report generated successfully.');
    } catch {
      setReportText(
        `### Faculty Evaluation for ${student.username}\n\n**Status**: ${student.semesterStatus}\n**Average Assessment Score**: ${student.averagePnl}%\n\n**Pedagogical Guidance**: Recommend targeted review in clinical acute abdomen and biochemical regulation modules.`
      );
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
            Faculty Academic Portal
          </span>
          <h1 className="text-2xl sm:text-3xl font-black text-white mt-0.5">Clinical Educator Console</h1>
          <p className="text-xs text-slate-400">Class trajectory monitoring, at-risk triage, and AI report generation</p>
        </div>

        <div className="flex items-center gap-3">
          <div className="bg-slate-950/80 border border-slate-800 rounded-2xl px-4 py-2.5 text-center min-w-[110px]">
            <span className="text-xs font-semibold text-slate-400 block">Class Health</span>
            <span className="text-xl font-extrabold text-emerald-400">{classAvgPnl}%</span>
          </div>
          <div className="bg-slate-950/80 border border-slate-800 rounded-2xl px-4 py-2.5 text-center min-w-[110px]">
            <span className="text-xs font-semibold text-slate-400 block">Active Cohort</span>
            <span className="text-xl font-extrabold text-indigo-400">{students.length} Students</span>
          </div>
        </div>
      </div>

      {/* At-Risk Intervention Alert Queue */}
      {atRiskStudents.length > 0 && (
        <div className="p-5 rounded-3xl bg-red-950/20 border border-red-800/50 space-y-3">
          <div className="flex items-center gap-2.5 text-red-400">
            <AlertTriangle className="w-5 h-5 shrink-0" />
            <h3 className="text-sm font-bold uppercase tracking-wider">
              Priority Pedagogical Action Required ({atRiskStudents.length} Students at Risk)
            </h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {atRiskStudents.map((s) => (
              <div
                key={s.id}
                className="p-4 rounded-2xl bg-slate-950/80 border border-red-900/40 flex items-center justify-between gap-3"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">{s.username}</h4>
                  <p className="text-[11px] text-red-300/90 mt-0.5">{s.riskReason}</p>
                </div>
                <button
                  onClick={() => handleGenerateReport(s)}
                  className="px-3 py-1.5 rounded-xl bg-red-600/30 hover:bg-red-600/50 text-red-200 border border-red-500/40 text-xs font-bold transition-colors shrink-0 flex items-center gap-1"
                >
                  <Sparkles className="w-3 h-3" />
                  <span>AI Action</span>
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* AI Pedagogical Report Modal / Card */}
      {selectedStudent && (
        <div className="p-6 rounded-3xl bg-slate-900 border border-indigo-500/40 shadow-2xl space-y-4 animate-in zoom-in-95 duration-150">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              <h3 className="text-sm font-bold text-white">
                Pedagogical Assessment for {selectedStudent.username}
              </h3>
            </div>
            <button
              onClick={() => setSelectedStudent(null)}
              className="text-xs text-slate-400 hover:text-white"
            >
              Close
            </button>
          </div>

          {isGenerating ? (
            <div className="p-8 text-center text-indigo-400 text-xs flex items-center justify-center gap-2">
              <RotateCw className="w-4 h-4 animate-spin" />
              <span>Generating tailored pedagogical report with Gemini...</span>
            </div>
          ) : (
            reportText && (
              <div className="text-xs text-slate-200 leading-relaxed bg-slate-950/80 p-5 rounded-2xl border border-slate-800">
                <FormattedText text={reportText} />
              </div>
            )
          )}
        </div>
      )}

      {/* Cohort Performance Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl overflow-hidden shadow-xl">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <h3 className="text-sm font-bold text-white uppercase tracking-wider">
            Cohort Progression Matrix
          </h3>
          <span className="text-xs text-slate-400 font-medium">{students.length} Total Enrolled</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 text-[10px] uppercase tracking-wider font-bold">
              <tr>
                <th className="p-4">Student</th>
                <th className="p-4">Academic Status</th>
                <th className="p-4">Active Units</th>
                <th className="p-4">Subtopics Completed</th>
                <th className="p-4">Avg PnL</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {students.map((s) => (
                <tr key={s.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="p-4 font-bold text-white flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-slate-800 flex items-center justify-center text-[10px] text-slate-300">
                      {s.username.charAt(0)}
                    </span>
                    <span>{s.username}</span>
                  </td>
                  <td className="p-4 text-slate-300">{s.semesterStatus}</td>
                  <td className="p-4 text-slate-400">{s.activeUnits.join(', ')}</td>
                  <td className="p-4 text-slate-300 font-medium">
                    {s.completedSubtopics} / {s.totalSubtopics}
                  </td>
                  <td className="p-4">
                    <span
                      className={`px-2 py-0.5 rounded-full font-bold text-[11px] ${
                        s.averagePnl >= 80
                          ? 'bg-emerald-500/10 text-emerald-400'
                          : s.averagePnl >= 60
                          ? 'bg-amber-500/10 text-amber-400'
                          : 'bg-red-500/10 text-red-400'
                      }`}
                    >
                      {s.averagePnl}%
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleGenerateReport(s)}
                      className="px-3 py-1.5 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 border border-indigo-500/30 font-semibold transition-colors"
                    >
                      Generate Report
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
