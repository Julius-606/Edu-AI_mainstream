import React, { useState } from 'react';
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Bookmark as BookmarkIcon,
  Sparkles,
  Send,
  HelpCircle,
  BookOpen,
  ArrowRight,
  RotateCw
} from 'lucide-react';
import { Unit, Subtopic, SubtopicObjective } from '../types';
import { FormattedText } from './FormattedText';
import { TraceStore } from '../lib/store';

interface LearnScreenProps {
  unit: Unit;
  subtopicId?: number;
  onBack: () => void;
  onLaunchQuiz: (unitName: string, topicName?: string) => void;
  onOpenBrowser: (url?: string) => void;
}

export const LearnScreen: React.FC<LearnScreenProps> = ({
  unit,
  subtopicId: initialSubtopicId,
  onBack,
  onLaunchQuiz,
  onOpenBrowser
}) => {
  // Find current subtopic or default to first
  const allSubtopics: { subtopic: Subtopic; topicName: string; moduleName: string }[] = [];
  for (const m of unit.modules) {
    for (const t of m.topics) {
      for (const s of t.subtopics) {
        allSubtopics.push({ subtopic: s, topicName: t.name, moduleName: m.name });
      }
    }
  }

  const initialIndex = initialSubtopicId
    ? allSubtopics.findIndex((item) => item.subtopic.id === initialSubtopicId)
    : 0;

  const [currentIndex, setCurrentIndex] = useState(initialIndex >= 0 ? initialIndex : 0);
  const [currentObjIndex, setCurrentObjIndex] = useState(0);

  const activeItem = allSubtopics[currentIndex] || allSubtopics[0];
  const activeSubtopic = activeItem?.subtopic;
  const objectives = activeSubtopic?.objectives || [];
  const currentObjective: SubtopicObjective | undefined = objectives[currentObjIndex] || objectives[0];

  const [isCompleted, setIsCompleted] = useState(activeSubtopic?.isCompleted ?? false);
  const [bookmarked, setBookmarked] = useState(false);
  const [bookmarkNote, setBookmarkNote] = useState('');
  const [showBookmarkInput, setShowBookmarkInput] = useState(false);

  // Inline AI Query state
  const [aiQuestion, setAiQuestion] = useState('');
  const [aiResponse, setAiResponse] = useState<string | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);

  const handleToggleComplete = () => {
    if (!activeSubtopic) return;
    TraceStore.toggleSubtopicCompleted(activeSubtopic.id);
    setIsCompleted(!isCompleted);
  };

  const handleSaveBookmark = () => {
    if (!activeSubtopic || !currentObjective) return;
    TraceStore.addBookmark({
      userId: TraceStore.getUser().id,
      subtopicId: activeSubtopic.id,
      subtopicName: activeSubtopic.name,
      objectiveDescription: currentObjective.title,
      excerpt: currentObjective.content.slice(0, 160) + '...',
      notes: bookmarkNote.trim() || undefined
    });
    setBookmarked(true);
    setShowBookmarkInput(false);
    setBookmarkNote('');
  };

  const handleAskAi = async (promptOverride?: string) => {
    const question = promptOverride || aiQuestion;
    if (!question.trim()) return;

    setIsAiLoading(true);
    setAiResponse(null);

    try {
      const res = await fetch('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: `In the context of ${unit.unitName} -> ${activeItem.topicName} -> ${activeSubtopic.name}: ${question}`,
          persona: 'Socratic Tutor'
        })
      });
      const data = await res.json();
      setAiResponse(data.text || 'No response received.');
    } catch {
      setAiResponse('AI consultant offline. Please check connection.');
    } finally {
      setIsAiLoading(false);
      setAiQuestion('');
    }
  };

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Top Header & Breadcrumb */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/80 border border-slate-800 rounded-3xl p-5 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
            title="Back to Dashboard"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
              <span>{unit.unitName}</span>
              <span>/</span>
              <span>{activeItem?.moduleName}</span>
            </div>
            <h1 className="text-xl font-bold text-white mt-0.5">{activeSubtopic?.name}</h1>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowBookmarkInput(!showBookmarkInput)}
            className={`p-2.5 rounded-xl border text-xs font-semibold flex items-center gap-1.5 transition-colors ${
              bookmarked
                ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
            }`}
            title="Bookmark note"
          >
            <BookmarkIcon className="w-4 h-4" />
            <span className="hidden sm:inline">{bookmarked ? 'Saved' : 'Bookmark'}</span>
          </button>

          <button
            onClick={handleToggleComplete}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 transition-all ${
              isCompleted
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-600/20'
            }`}
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>{isCompleted ? 'Completed' : 'Mark Complete'}</span>
          </button>
        </div>
      </div>

      {/* Bookmark note input popup */}
      {showBookmarkInput && (
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 space-y-3 animate-in fade-in duration-150">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
            Add High-Yield Study Note to Repository
          </h4>
          <input
            type="text"
            value={bookmarkNote}
            onChange={(e) => setBookmarkNote(e.target.value)}
            placeholder="e.g., Critical mechanism for Friday rounds..."
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white focus:border-indigo-500 outline-none"
          />
          <div className="flex justify-end gap-2">
            <button
              onClick={() => setShowBookmarkInput(false)}
              className="px-3 py-1.5 rounded-lg text-xs text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              onClick={handleSaveBookmark}
              className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold"
            >
              Save to Repository
            </button>
          </div>
        </div>
      )}

      {/* Objectives Navigation Tabs */}
      {objectives.length > 1 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {objectives.map((obj, i) => (
            <button
              key={obj.id}
              onClick={() => setCurrentObjIndex(i)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
                currentObjIndex === i
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              Part {i + 1}: {obj.title}
            </button>
          ))}
        </div>
      )}

      {/* Main Learning Content Panel */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 backdrop-blur-md shadow-xl">
        {currentObjective ? (
          <div>
            <div className="border-b border-slate-800/80 pb-4 mb-6">
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-400 block mb-1">
                Learning Objective {currentObjIndex + 1} of {objectives.length}
              </span>
              <h2 className="text-xl sm:text-2xl font-black text-white">{currentObjective.title}</h2>
              <p className="text-sm text-slate-400 mt-1">{currentObjective.description}</p>
            </div>

            {/* Markdown rendered body */}
            <FormattedText
              text={currentObjective.content}
              onLinkClick={(url) => onOpenBrowser(url)}
            />
          </div>
        ) : (
          <div className="text-center py-12 text-slate-400">
            No objectives configured for this topic.
          </div>
        )}

        {/* Previous / Next Objective Controls */}
        <div className="mt-10 pt-6 border-t border-slate-800 flex items-center justify-between">
          <button
            onClick={() => {
              if (currentObjIndex > 0) {
                setCurrentObjIndex(currentObjIndex - 1);
              } else if (currentIndex > 0) {
                setCurrentIndex(currentIndex - 1);
                setCurrentObjIndex(0);
              }
            }}
            disabled={currentIndex === 0 && currentObjIndex === 0}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 disabled:opacity-40 text-xs font-semibold transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
            <span>Previous Part</span>
          </button>

          <button
            onClick={() => {
              if (currentObjIndex < objectives.length - 1) {
                setCurrentObjIndex(currentObjIndex + 1);
              } else if (currentIndex < allSubtopics.length - 1) {
                setCurrentIndex(currentIndex + 1);
                setCurrentObjIndex(0);
              }
            }}
            disabled={currentIndex === allSubtopics.length - 1 && currentObjIndex === objectives.length - 1}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors"
          >
            <span>Next Part</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Inline Socratic AI Consultant Prompt Box */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-xl space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">
              Socratic Consultant Inquiry
            </h3>
            <p className="text-xs text-slate-400">Ask any clinical question or request deep physiological analogies</p>
          </div>
        </div>

        {/* Quick Inquiry Chips */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs">
          <button
            onClick={() => handleAskAi('Explain this mechanism step-by-step using a clinical analogy.')}
            className="px-3 py-1.5 rounded-full bg-slate-800/90 hover:bg-indigo-950 text-slate-300 hover:text-indigo-300 border border-slate-700/60 transition-colors whitespace-nowrap"
          >
            Explain with analogy
          </button>
          <button
            onClick={() => handleAskAi('What are the most common diagnostic traps or board exam questions on this?')}
            className="px-3 py-1.5 rounded-full bg-slate-800/90 hover:bg-indigo-950 text-slate-300 hover:text-indigo-300 border border-slate-700/60 transition-colors whitespace-nowrap"
          >
            Common exam traps
          </button>
          <button
            onClick={() => handleAskAi('How does this directly alter patient management and pharmacotherapy?')}
            className="px-3 py-1.5 rounded-full bg-slate-800/90 hover:bg-indigo-950 text-slate-300 hover:text-indigo-300 border border-slate-700/60 transition-colors whitespace-nowrap"
          >
            Patient management impact
          </button>
        </div>

        {/* Input bar */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleAskAi();
          }}
          className="flex items-center gap-2 bg-slate-950 border border-slate-800 rounded-2xl p-1.5 focus-within:border-indigo-500 transition-colors"
        >
          <input
            type="text"
            value={aiQuestion}
            onChange={(e) => setAiQuestion(e.target.value)}
            placeholder="Type your inquiry for this subtopic..."
            className="flex-1 bg-transparent px-3 py-1 text-xs text-white placeholder-slate-500 outline-none"
          />
          <button
            type="submit"
            disabled={isAiLoading || !aiQuestion.trim()}
            className="p-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white disabled:opacity-40 transition-colors"
          >
            <Send className="w-3.5 h-3.5" />
          </button>
        </form>

        {/* AI Answer Box */}
        {isAiLoading && (
          <div className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800/80 flex items-center gap-3 text-xs text-indigo-300">
            <RotateCw className="w-4 h-4 animate-spin text-indigo-400" />
            <span>Consulting clinical knowledge engine...</span>
          </div>
        )}

        {aiResponse && (
          <div className="p-5 rounded-2xl bg-slate-950/80 border border-indigo-500/30 text-xs">
            <FormattedText text={aiResponse} onLinkClick={onOpenBrowser} />
          </div>
        )}
      </div>

      {/* CTA: Launch Retrieval Quiz */}
      <div className="bg-gradient-to-r from-indigo-950/40 via-slate-900 to-indigo-950/40 border border-indigo-500/30 rounded-3xl p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h3 className="text-base font-bold text-white">Consolidate with Knowledge Retrieval</h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Test retention and calculate your PnL score on {activeItem?.topicName}.
          </p>
        </div>
        <button
          onClick={() => onLaunchQuiz(unit.unitName, activeItem?.topicName)}
          className="flex items-center justify-center gap-2 px-6 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs tracking-wider uppercase transition-all shadow-lg shadow-indigo-600/30"
        >
          <span>Launch Quiz</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
