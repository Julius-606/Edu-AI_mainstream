import React from 'react';
import { X, Archive, CheckCircle2, RotateCcw, Calendar, Award } from 'lucide-react';
import { Unit, QuizHistoryItem } from '../types';

interface ArchivesModalProps {
  units: Unit[];
  quizHistory: QuizHistoryItem[];
  onClose: () => void;
  onRestoreUnit: (unitId: number) => void;
}

export const ArchivesModal: React.FC<ArchivesModalProps> = ({
  units,
  quizHistory,
  onClose,
  onRestoreUnit
}) => {
  const inactiveOrArchived = units.filter((u) => !u.isActive);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-slate-800 text-slate-300">
              <Archive className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Archives & Learning Records</h3>
              <p className="text-xs text-slate-400">Past terms, completed units, and historical assessment results</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Archived Units */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
              Archived & Inactive Units ({inactiveOrArchived.length})
            </h4>
            {inactiveOrArchived.length === 0 ? (
              <p className="text-xs text-slate-500 bg-slate-950/40 border border-slate-800/80 rounded-xl p-4 text-center">
                All units are currently active in your live curriculum.
              </p>
            ) : (
              <div className="space-y-2.5">
                {inactiveOrArchived.map((unit) => (
                  <div
                    key={unit.id}
                    className="flex items-center justify-between p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 hover:border-slate-700 transition-colors"
                  >
                    <div>
                      <h5 className="text-sm font-semibold text-white">{unit.unitName}</h5>
                      <p className="text-xs text-slate-400">{unit.category} • {unit.modules.length} Modules</p>
                    </div>
                    <button
                      onClick={() => onRestoreUnit(unit.id)}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 hover:bg-indigo-600/40 text-xs font-medium transition-colors"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>Re-activate</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Historical Assessment Trail */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3 flex items-center justify-between">
              <span>Historical Quiz Trail ({quizHistory.length})</span>
              <span className="text-[11px] font-normal text-slate-500">PnL Metric Scores</span>
            </h4>
            {quizHistory.length === 0 ? (
              <p className="text-xs text-slate-500 bg-slate-950/40 border border-slate-800/80 rounded-xl p-4 text-center">
                No archived quiz sessions yet.
              </p>
            ) : (
              <div className="space-y-2">
                {quizHistory.map((item) => (
                  <div
                    key={item.id}
                    className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold text-xs ${
                          item.pnlScore >= 80
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                            : item.pnlScore >= 60
                            ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                            : 'bg-red-500/10 text-red-400 border border-red-500/30'
                        }`}
                      >
                        {Math.round(item.pnlScore)}%
                      </div>
                      <div>
                        <p className="text-xs font-semibold text-white">{item.unitName}</p>
                        <p className="text-[11px] text-slate-400">{item.topicName || 'Integrative Quiz'}</p>
                      </div>
                    </div>
                    <div className="text-right text-[11px] text-slate-500">
                      <span>{item.score} / {item.total} correct</span>
                      <p>{new Date(item.timestamp).toLocaleDateString()}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="p-4 border-t border-slate-800 bg-slate-950/60 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
