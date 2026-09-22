import React, { useState } from 'react';
import { X, BookOpen, Check, Plus, Trash2 } from 'lucide-react';
import { Unit } from '../types';

interface ManageUnitsModalProps {
  units: Unit[];
  onClose: () => void;
  onToggleActive: (unitId: number, isActive: boolean) => void;
}

export const ManageUnitsModal: React.FC<ManageUnitsModalProps> = ({
  units,
  onClose,
  onToggleActive
}) => {
  const [searchTerm, setSearchTerm] = useState('');

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
              <p className="text-xs text-slate-400">Add or deactivate units in your active semester trail</p>
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
          {filtered.map((unit) => (
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

                <button
                  onClick={() => onToggleActive(unit.id, !unit.isActive)}
                  className={`px-3 py-1.5 rounded-xl font-semibold text-xs transition-colors shrink-0 ${
                    unit.isActive
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 hover:bg-red-950/40 hover:text-red-300 hover:border-red-800'
                      : 'bg-indigo-600 hover:bg-indigo-500 text-white'
                  }`}
                >
                  {unit.isActive ? 'Active (Deactivate)' : 'Enroll Unit'}
                </button>
              </div>
            </div>
          ))}
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
