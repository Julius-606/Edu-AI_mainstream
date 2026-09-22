import React, { useState } from 'react';
import {
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  CheckCircle2,
  Circle,
  BookOpen,
  ArrowRight,
  Layers
} from 'lucide-react';
import { Unit, Module, Topic, Subtopic } from '../types';

interface UnitOutlineScreenProps {
  unit: Unit;
  onBack: () => void;
  onOpenSubtopic: (unitId: number, subtopicId: number) => void;
  onLaunchQuiz: (unitName: string, topicName?: string) => void;
}

export const UnitOutlineScreen: React.FC<UnitOutlineScreenProps> = ({
  unit,
  onBack,
  onOpenSubtopic,
  onLaunchQuiz
}) => {
  const [expandedModules, setExpandedModules] = useState<Record<number, boolean>>({
    [unit.modules[0]?.id || 0]: true
  });

  const toggleModule = (id: number) => {
    setExpandedModules((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const allSubtopics = unit.modules.flatMap((m) => m.topics.flatMap((t) => t.subtopics));
  const completedCount = allSubtopics.filter((s) => s.isCompleted).length;
  const progress = allSubtopics.length > 0 ? Math.round((completedCount / allSubtopics.length) * 100) : 0;

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Unit Header Card */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 backdrop-blur-md shadow-xl">
        <div className="flex items-center gap-3 mb-4">
          <button
            onClick={onBack}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <span className="text-xs font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-indigo-600/20 text-indigo-300 border border-indigo-500/30">
            {unit.category}
          </span>
        </div>

        <h1 className="text-2xl sm:text-3xl font-black text-white">{unit.unitName}</h1>
        <p className="text-sm text-slate-400 mt-2 max-w-3xl leading-relaxed">{unit.description}</p>

        {/* Progress & Quick Stats */}
        <div className="mt-6 pt-6 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4 text-xs text-slate-400">
            <span>
              <strong className="text-white">{unit.modules.length}</strong> Modules
            </span>
            <span>•</span>
            <span>
              <strong className="text-white">{unit.modules.flatMap((m) => m.topics).length}</strong> Topics
            </span>
            <span>•</span>
            <span>
              <strong className="text-white">{completedCount}</strong> of {allSubtopics.length} Subtopics Completed
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => onLaunchQuiz(unit.unitName)}
              className="px-4 py-2 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/40 text-xs font-bold hover:bg-amber-500/30 transition-colors"
            >
              Unit Assessment
            </button>
            {allSubtopics[0] && (
              <button
                onClick={() => onOpenSubtopic(unit.id, allSubtopics[0].id)}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-md shadow-indigo-600/20 transition-colors"
              >
                <span>Start Learning</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="mt-4 w-full h-2 rounded-full bg-slate-800 overflow-hidden">
          <div
            className="h-full bg-indigo-500 rounded-full transition-all duration-500"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* Modules & Topics Tree */}
      <div className="space-y-4">
        {unit.modules.map((module) => {
          const isExpanded = expandedModules[module.id] ?? false;
          const modSubtopics = module.topics.flatMap((t) => t.subtopics);
          const modCompleted = modSubtopics.filter((s) => s.isCompleted).length;

          return (
            <div
              key={module.id}
              className="bg-slate-900/80 border border-slate-800 rounded-3xl overflow-hidden shadow-lg"
            >
              {/* Module Header Bar */}
              <div
                onClick={() => toggleModule(module.id)}
                className="p-5 flex items-center justify-between cursor-pointer hover:bg-slate-800/40 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-indigo-400">
                    <Layers className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-white">{module.name}</h3>
                    {module.description && (
                      <p className="text-xs text-slate-400 mt-0.5">{module.description}</p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-400 font-medium">
                    {modCompleted}/{modSubtopics.length} Done
                  </span>
                  {isExpanded ? (
                    <ChevronDown className="w-5 h-5 text-slate-400" />
                  ) : (
                    <ChevronRight className="w-5 h-5 text-slate-400" />
                  )}
                </div>
              </div>

              {/* Module Topics & Subtopics Body */}
              {isExpanded && (
                <div className="px-5 pb-5 pt-1 border-t border-slate-800/80 space-y-4">
                  {module.topics.map((topic) => (
                    <div
                      key={topic.id}
                      className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800/80 space-y-3"
                    >
                      <div className="flex items-center justify-between">
                        <h4 className="text-sm font-bold text-indigo-300">{topic.name}</h4>
                        <button
                          onClick={() => onLaunchQuiz(unit.unitName, topic.name)}
                          className="text-[11px] font-semibold text-amber-400 hover:underline"
                        >
                          Quiz on this topic
                        </button>
                      </div>

                      {/* Subtopics List */}
                      <div className="space-y-1.5 pl-2">
                        {topic.subtopics.map((sub) => (
                          <div
                            key={sub.id}
                            onClick={() => onOpenSubtopic(unit.id, sub.id)}
                            className="p-3 rounded-xl bg-slate-900/90 border border-slate-800/80 hover:border-indigo-500/50 hover:bg-slate-800/80 flex items-center justify-between cursor-pointer transition-all group"
                          >
                            <div className="flex items-center gap-3">
                              {sub.isCompleted ? (
                                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                              ) : (
                                <Circle className="w-4 h-4 text-slate-500 shrink-0" />
                              )}
                              <span className="text-xs font-semibold text-slate-200 group-hover:text-white">
                                {sub.name}
                              </span>
                            </div>

                            <div className="flex items-center gap-2">
                              <span className="text-[10px] text-slate-500">
                                {sub.objectives.length} Objectives
                              </span>
                              <ArrowRight className="w-3.5 h-3.5 text-slate-500 group-hover:text-indigo-400 transition-colors" />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
