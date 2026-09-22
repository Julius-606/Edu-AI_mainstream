import React, { useState } from 'react';
import {
  Calendar,
  Sparkles,
  Clock,
  RefreshCw,
  BookOpen,
  CheckCircle,
  Coffee,
  BrainCircuit,
  Filter
} from 'lucide-react';
import { TimetableSlot, User } from '../types';
import { TraceStore } from '../lib/store';

interface TimetableTabProps {
  user: User;
  onOpenUnitByName: (unitName: string) => void;
  onOpenQuizzes: () => void;
}

export const TimetableTab: React.FC<TimetableTabProps> = ({
  user,
  onOpenUnitByName,
  onOpenQuizzes
}) => {
  const [timetable, setTimetable] = useState<TimetableSlot[]>(TraceStore.getTimetable());
  const [selectedDay, setSelectedDay] = useState<string>('All');
  const [selectedType, setSelectedType] = useState<string>('All');
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [aiBrief, setAiBrief] = useState<string>(
    `AI Timetable Brief for ${user.username}: Structured spaced repetition with prioritized focus on Biochemistry II and General Surgery based on recent assessment accuracy. Cognitive consolidation breaks are strategically positioned to optimize long-term synaptic retention.`
  );

  const days = ['All', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  const types = ['All', 'study', 'assessment', 'revision', 'break'];

  const filteredSlots = timetable.filter((slot) => {
    const matchDay = selectedDay === 'All' || slot.day === selectedDay;
    const matchType = selectedType === 'All' || slot.type === selectedType;
    return matchDay && matchType;
  });

  const handleRegenerate = async () => {
    setIsRegenerating(true);
    setTimeout(() => {
      // Rotate or refresh study plan
      const refreshed = TraceStore.getTimetable();
      setTimetable(refreshed);
      setAiBrief(
        `AI Timetable Brief updated: Intensified retrieval focus on Acute Coronary Syndromes & Acid-Base disorders for the upcoming week based on your latest consultation inquiries.`
      );
      setIsRegenerating(false);
    }, 600);
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'study':
        return 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40';
      case 'assessment':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/40';
      case 'revision':
        return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40';
      case 'break':
        return 'bg-slate-800 text-slate-400 border-slate-700';
      default:
        return 'bg-slate-800 text-slate-300';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'study':
        return <BookOpen className="w-3.5 h-3.5" />;
      case 'assessment':
        return <Sparkles className="w-3.5 h-3.5" />;
      case 'revision':
        return <BrainCircuit className="w-3.5 h-3.5" />;
      case 'break':
        return <Coffee className="w-3.5 h-3.5" />;
      default:
        return <Clock className="w-3.5 h-3.5" />;
    }
  };

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Top Header */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
            Neuro-Optimized Scheduling
          </span>
          <h1 className="text-2xl font-black text-white mt-0.5">Dynamic Weekly Timetable</h1>
          <p className="text-xs text-slate-400">Spaced repetition and cognitive load balancing</p>
        </div>

        <button
          onClick={handleRegenerate}
          disabled={isRegenerating}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 text-xs font-bold transition-colors disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRegenerating ? 'animate-spin' : ''}`} />
          <span>{isRegenerating ? 'Optimizing Schedule...' : 'Regenerate AI Timetable'}</span>
        </button>
      </div>

      {/* AI Brief Banner */}
      <div className="p-5 rounded-3xl bg-slate-900/90 border border-slate-800 flex items-start gap-3.5 shadow-lg">
        <div className="p-2.5 rounded-2xl bg-indigo-600/20 text-indigo-400 shrink-0 mt-0.5 border border-indigo-500/30">
          <Sparkles className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-white mb-1">
            Algorithmic Allocation Rationale
          </h3>
          <p className="text-xs text-slate-300 leading-relaxed">{aiBrief}</p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="space-y-3">
        {/* Days of the Week */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
          {days.map((day) => (
            <button
              key={day}
              onClick={() => setSelectedDay(day)}
              className={`px-3.5 py-1.5 rounded-xl font-semibold whitespace-nowrap transition-colors ${
                selectedDay === day
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-white'
              }`}
            >
              {day}
            </button>
          ))}
        </div>

        {/* Type Filter */}
        <div className="flex items-center gap-2 text-xs">
          <span className="text-slate-500 text-[11px] uppercase font-bold tracking-wider">Type:</span>
          {types.map((type) => (
            <button
              key={type}
              onClick={() => setSelectedType(type)}
              className={`px-3 py-1 rounded-full capitalize text-xs font-medium transition-colors ${
                selectedType === type
                  ? 'bg-slate-700 text-white'
                  : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      {/* Schedule Slots List */}
      <div className="space-y-3">
        {filteredSlots.length > 0 ? (
          filteredSlots.map((slot) => (
            <div
              key={slot.id}
              className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/90 hover:border-slate-700 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm"
            >
              <div className="flex items-start gap-4">
                <div className="w-20 shrink-0 text-left">
                  <span className="text-xs font-bold text-white block">{slot.day}</span>
                  <span className="text-[11px] text-slate-400 font-mono">
                    {slot.startTime} - {slot.endTime}
                  </span>
                </div>

                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border flex items-center gap-1 ${getTypeColor(
                        slot.type
                      )}`}
                    >
                      {getTypeIcon(slot.type)}
                      <span>{slot.type}</span>
                    </span>
                    {slot.unitName && (
                      <span className="text-xs text-indigo-400 font-semibold">{slot.unitName}</span>
                    )}
                  </div>

                  <h3 className="text-sm font-bold text-white">{slot.title}</h3>
                  {slot.notes && (
                    <p className="text-xs text-slate-400 mt-0.5 italic">{slot.notes}</p>
                  )}
                </div>
              </div>

              {/* Action */}
              <div className="shrink-0 flex items-center gap-2">
                {slot.type === 'study' && slot.unitName && (
                  <button
                    onClick={() => onOpenUnitByName(slot.unitName!)}
                    className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors"
                  >
                    Open Unit
                  </button>
                )}
                {slot.type === 'assessment' && (
                  <button
                    onClick={onOpenQuizzes}
                    className="px-4 py-2 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 text-xs font-semibold transition-colors"
                  >
                    Launch Quiz
                  </button>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="p-12 text-center text-slate-500 bg-slate-900/40 rounded-3xl border border-slate-800">
            No schedule slots match this filter.
          </div>
        )}
      </div>
    </div>
  );
};
