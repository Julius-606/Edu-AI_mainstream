import React, { useState } from 'react';
import { X, BookOpen, Check, Plus, Trash2, ChevronDown, AlertTriangle } from 'lucide-react';
import { Unit } from '../types';

interface ManageUnitsModalProps {
  units: Unit[];
  onClose: () => void;
  onToggleActive: (unitId: number, isActive: boolean) => void;
  onDeleteUnit?: (unitId: number) => void;
}

export const ManageUnitsModal: React.FC<ManageUnitsModalProps> = ({
  units,
  onClose,
  onToggleActive,
  onDeleteUnit
}) => {
  const [searchTerm = '', setSearchTerm] = useState('');
  const [expandedDeleteId, setExpandedDeleteId] = useState<number | null>(null);

  const filtered = units.filter(
    (u) =>
      u.unitName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.category.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-600/20 text-indigo-400">
              <BookOpen className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Manage Curriculum Units</h3>
              <p className="text-xs text-slate-400">Add, deactivate, or safely delete units in your syllabus trail</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 border-b border-slate-800/80 bg-slate-950/40">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search clinical units or disciplines..."
            className="w-full bg-slate-900 border border-slate-700/80 rounded-xl px-3.5 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-2.5">
          {filtered.map((unit) => {
            const isDeleteOpen = expandedDeleteId === unit.id;

            return (
              <div
                key={unit.id}
                className={`p-4 rounded-xl border transition-all ${
                  unit.isActive
                    ? 'bg-indigo-950/20 border-indigo-500/40 shadow-sm'
                    : 'bg-slate-950/40 border-slate-800 text-slate-400'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-white">{unit.unitName}</h4>
                      <span className="text-[10px] font-semibold uppercase px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
                        {unit.category}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                      {unit.description}
                    </p>
                    <p className="text-[11px] text-indigo-300 font-medium mt-1.5">
                      {unit.modules.length} Modules • {unit.modules.flatMap(m => m.topics).length} Topics
                    </p>
                  </div>

                  <div className="flex flex-col gap-2 shrink-0 items-end">
                    <button
                      onClick={() => onToggleActive(unit.id, !unit.isActive)}
                      className={`px-3 py-1.5 rounded-xl font-semibold text-xs transition-colors ${
                        unit.isActive
                          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 hover:bg-amber-950/40 hover:text-amber-300 hover:border-amber-800'
                          : 'bg-indigo-600 hover:bg-indigo-500 text-white'
                      }`}
                    >
                      {unit.isActive ? 'Active (Deactivate)' : 'Enroll Unit'}
                    </button>

                    {onDeleteUnit && (
                      <button
                        onClick={() => setExpandedDeleteId(isDeleteOpen ? null : unit.id)}
                        className={`flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] font-medium transition-colors ${
                          isDeleteOpen
                            ? 'text-red-300 bg-red-950/40 border border-red-800/60'
                            : 'text-slate-400 hover:text-red-300 hover:bg-slate-800/60'
                        }`}
                        title="Toggle Delete Option"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>Delete Options</span>
                        <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${isDeleteOpen ? 'rotate-180 text-red-400' : ''}`} />
                      </button>
                    )}
                  </div>
                </div>

                {/* Collapsible Delete Section */}
                {isDeleteOpen && onDeleteUnit && (
                  <div className="mt-3 pt-3 border-t border-red-900/30 bg-red-950/20 rounded-xl p-3 animate-in fade-in slide-in-from-top-1 duration-200">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex items-start gap-2">
                        <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                        <div>
                          <p className="text-[11px] font-bold text-red-300">Danger Zone: Permanent Unit Deletion</p>
                          <p className="text-[10px] text-red-400/80 leading-snug">
                            This will permanently erase {unit.unitName}, its modules, topics, and all student learning progress.
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-end sm:self-auto">
                        <button
                          onClick={() => setExpandedDeleteId(null)}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
                        >
                          Cancel
                        </button>
                        <button
                          onClick={() => {
                            if (confirm(`Are you sure you want to completely delete ${unit.unitName}? This will permanently remove its syllabus contents.`)) {
                              onDeleteUnit(unit.id);
                              setExpandedDeleteId(null);
                            }
                          }}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-600 hover:bg-red-500 text-white font-bold text-xs shadow-md shadow-red-900/40 shrink-0 transition-all"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Delete Unit</span>
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="p-4 border-t border-slate-800 bg-slate-950/60 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
