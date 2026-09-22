import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  CheckCircle2,
  XCircle,
  Clock,
  RotateCcw,
  ArrowRight,
  ChevronRight,
  HelpCircle,
  Award,
  BookOpen
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { Unit, QuizQuestion, QuizHistoryItem } from '../types';
import { TraceStore } from '../lib/store';
import { FormattedText } from './FormattedText';

interface QuizInterfaceProps {
  units: Unit[];
  initialUnitName?: string;
  initialTopicName?: string;
  onBack: () => void;
}

export const QuizInterface: React.FC<QuizInterfaceProps> = ({
  units,
  initialUnitName,
  initialTopicName,
  onBack
}) => {
  const [selectedUnit, setSelectedUnit] = useState<string>(
    initialUnitName || (units[0] ? units[0].unitName : 'Biochemistry II')
  );
  const [selectedTopic, setSelectedTopic] = useState<string>(initialTopicName || '');
  const [isGenerating, setIsGenerating] = useState(false);

  // Quiz active state
  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [userAnswers, setUserAnswers] = useState<Record<number, number>>({});
  const [revealed, setRevealed] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [timerActive, setTimerActive] = useState(false);

  // Load questions on mount or unit selection
  useEffect(() => {
    loadQuestions(selectedUnit, selectedTopic);
  }, [selectedUnit, selectedTopic]);

  // Timer effect
  useEffect(() => {
    let interval: any = null;
    if (timerActive && !isCompleted) {
      interval = setInterval(() => setSeconds((s) => s + 1), 1000);
    }
    return () => clearInterval(interval);
  }, [timerActive, isCompleted]);

  const loadQuestions = (unitName: string, topicName?: string) => {
    const list = TraceStore.getQuizQuestions(unitName, topicName);
    setQuestions(list);
    setCurrentIndex(0);
    setSelectedOption(null);
    setUserAnswers({});
    setRevealed(false);
    setIsCompleted(false);
    setSeconds(0);
    setTimerActive(list.length > 0);
  };

  const handleGenerateAiQuiz = async () => {
    setIsGenerating(true);
    try {
      const res = await fetch('/api/quiz/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          unitName: selectedUnit,
          topicName: selectedTopic,
          studentLevel: TraceStore.getUser().semesterStatus
        })
      });
      const data = await res.json();
      if (data.questions && data.questions.length > 0) {
        setQuestions(data.questions);
        setCurrentIndex(0);
        setSelectedOption(null);
        setUserAnswers({});
        setRevealed(false);
        setIsCompleted(false);
        setSeconds(0);
        setTimerActive(true);
      } else {
        // Fallback to bank
        loadQuestions(selectedUnit, selectedTopic);
      }
    } catch {
      loadQuestions(selectedUnit, selectedTopic);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSelectOption = (idx: number) => {
    if (revealed) return;
    setSelectedOption(idx);
  };

  const handleConfirmAnswer = () => {
    if (selectedOption === null) return;
    setUserAnswers((prev) => ({ ...prev, [currentIndex]: selectedOption }));
    setRevealed(true);
  };

  const handleNextQuestion = () => {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex(currentIndex + 1);
      setSelectedOption(null);
      setRevealed(false);
    } else {
      // Complete quiz!
      completeQuiz();
    }
  };

  const completeQuiz = () => {
    setTimerActive(false);
    setIsCompleted(true);

    let correctCount = 0;
    questions.forEach((q, i) => {
      if (userAnswers[i] === q.correctIndex) {
        correctCount += 1;
      }
    });

    const pnlScore = questions.length > 0 ? Math.round((correctCount / questions.length) * 100) : 0;

    // Record in history
    TraceStore.recordQuizResult({
      userId: TraceStore.getUser().id,
      unitName: selectedUnit,
      topicName: selectedTopic || undefined,
      score: correctCount,
      total: questions.length,
      pnlScore,
      timestamp: Date.now(),
      questions,
      userAnswers
    });

    // Fire celebratory confetti if passing (≥ 70%)
    if (pnlScore >= 70) {
      try {
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 }
        });
      } catch (e) {
        console.log(e);
      }
    }
  };

  const currentQ = questions[currentIndex];
  const answeredCount = Object.keys(userAnswers).length;
  const correctSoFar = Object.entries(userAnswers).filter(
    ([idx, ans]) => questions[Number(idx)]?.correctIndex === ans
  ).length;

  const pnlPercent = questions.length > 0 ? Math.round((correctSoFar / questions.length) * 100) : 0;

  const formatTime = (totalSec: number) => {
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Top Header & Unit Selector */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-5 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-amber-400">
            Knowledge Retrieval Engine
          </span>
          <h1 className="text-2xl font-black text-white mt-0.5">High-Yield Clinical Assessment</h1>
          <p className="text-xs text-slate-400">PnL scoring with deep clinical rationales</p>
        </div>

        {/* Unit & AI Quiz Selector Controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          <select
            value={selectedUnit}
            onChange={(e) => setSelectedUnit(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white outline-none focus:border-indigo-500 font-medium"
          >
            {units.map((u) => (
              <option key={u.id} value={u.unitName}>
                {u.unitName}
              </option>
            ))}
          </select>

          <button
            onClick={handleGenerateAiQuiz}
            disabled={isGenerating}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/40 hover:bg-amber-500/30 text-xs font-bold transition-colors disabled:opacity-50"
          >
            <Sparkles className={`w-3.5 h-3.5 ${isGenerating ? 'animate-spin' : ''}`} />
            <span>{isGenerating ? 'Generating with Gemini...' : 'Generate New AI Quiz'}</span>
          </button>
        </div>
      </div>

      {/* Main Quiz Flow */}
      {!isCompleted ? (
        questions.length > 0 && currentQ ? (
          <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 backdrop-blur-md shadow-xl space-y-6">
            {/* Question Progress & Timer Header */}
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <span className="px-3 py-1 rounded-full bg-indigo-600/20 text-indigo-300 text-xs font-bold border border-indigo-500/30">
                  Question {currentIndex + 1} of {questions.length}
                </span>
                <span className="text-xs text-slate-400 font-medium hidden sm:inline">
                  {currentQ.topicName || selectedUnit}
                </span>
              </div>

              <div className="flex items-center gap-4 text-xs font-semibold text-slate-400">
                <div className="flex items-center gap-1.5 bg-slate-950 px-3 py-1 rounded-xl border border-slate-800">
                  <Clock className="w-3.5 h-3.5 text-indigo-400" />
                  <span>{formatTime(seconds)}</span>
                </div>
              </div>
            </div>

            {/* Question Stem */}
            <div>
              <h2 className="text-base sm:text-lg font-bold text-white leading-relaxed">
                {currentQ.question}
              </h2>
            </div>

            {/* Answer Options */}
            <div className="space-y-3">
              {currentQ.options.map((option, idx) => {
                const isSelected = selectedOption === idx;
                const isCorrect = currentQ.correctIndex === idx;

                let btnStyle = 'bg-slate-950/60 border-slate-800 text-slate-200 hover:border-slate-700';

                if (revealed) {
                  if (isCorrect) {
                    btnStyle = 'bg-emerald-950/40 border-emerald-500 text-emerald-200 font-semibold';
                  } else if (isSelected && !isCorrect) {
                    btnStyle = 'bg-red-950/40 border-red-500 text-red-200';
                  }
                } else if (isSelected) {
                  btnStyle = 'bg-indigo-950/40 border-indigo-500 text-white shadow-sm';
                }

                return (
                  <button
                    key={idx}
                    onClick={() => handleSelectOption(idx)}
                    disabled={revealed}
                    className={`w-full p-4 rounded-2xl border text-left flex items-center justify-between gap-3 transition-all ${btnStyle}`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-7 h-7 rounded-xl bg-slate-800/80 flex items-center justify-center text-xs font-bold text-slate-300 shrink-0">
                        {String.fromCharCode(65 + idx)}
                      </span>
                      <span className="text-sm">{option}</span>
                    </div>

                    {revealed && isCorrect && (
                      <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                    )}
                    {revealed && isSelected && !isCorrect && (
                      <XCircle className="w-5 h-5 text-red-400 shrink-0" />
                    )}
                  </button>
                );
              })}
            </div>

            {/* Immediate Clinical Rationale Disclosure */}
            {revealed && (
              <div className="p-5 rounded-2xl bg-slate-950/90 border border-slate-800 space-y-2 animate-in fade-in duration-200">
                <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-emerald-400">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Clinical Rationale & Analysis</span>
                </div>
                <div className="text-xs text-slate-300 leading-relaxed">
                  <FormattedText text={currentQ.explanation} />
                </div>
              </div>
            )}

            {/* Confirm / Next Controls */}
            <div className="pt-4 border-t border-slate-800 flex items-center justify-between">
              <button
                onClick={onBack}
                className="text-xs font-semibold text-slate-400 hover:text-white"
              >
                Exit Assessment
              </button>

              {!revealed ? (
                <button
                  onClick={handleConfirmAnswer}
                  disabled={selectedOption === null}
                  className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs transition-colors disabled:opacity-40"
                >
                  Confirm Answer
                </button>
              ) : (
                <button
                  onClick={handleNextQuestion}
                  className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs transition-colors shadow-lg shadow-indigo-600/30"
                >
                  <span>{currentIndex < questions.length - 1 ? 'Next Question' : 'Complete & View Score'}</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-12 text-center text-slate-400 space-y-4">
            <BookOpen className="w-12 h-12 mx-auto text-slate-600" />
            <h3 className="text-base font-bold text-white">No questions available for this selection</h3>
            <p className="text-xs max-w-sm mx-auto text-slate-500">
              Click below to prompt Gemini to generate a brand-new high-yield assessment.
            </p>
            <button
              onClick={handleGenerateAiQuiz}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs"
            >
              Generate AI Assessment
            </button>
          </div>
        )
      ) : (
        /* Post-Quiz Results Card */
        <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-8 backdrop-blur-md shadow-2xl space-y-6 text-center animate-in zoom-in-95 duration-200">
          <div className="inline-flex p-4 rounded-3xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/40">
            <Award className="w-10 h-10" />
          </div>

          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Assessment Report Card
            </span>
            <h2 className="text-2xl font-black text-white mt-1">
              {pnlPercent >= 80 ? 'Exceptional Mastery!' : pnlPercent >= 60 ? 'Satisfactory Performance' : 'Remediation Advised'}
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Completed {questions.length} questions in {formatTime(seconds)}
            </p>
          </div>

          <div className="max-w-xs mx-auto p-5 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-around">
            <div>
              <span className="text-xs text-slate-400 block font-medium">PnL Metric</span>
              <span
                className={`text-2xl font-black ${
                  pnlPercent >= 80 ? 'text-emerald-400' : pnlPercent >= 60 ? 'text-amber-400' : 'text-red-400'
                }`}
              >
                {pnlPercent}%
              </span>
            </div>
            <div className="h-8 w-px bg-slate-800" />
            <div>
              <span className="text-xs text-slate-400 block font-medium">Score</span>
              <span className="text-2xl font-black text-white">
                {correctSoFar} / {questions.length}
              </span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <button
              onClick={() => loadQuestions(selectedUnit, selectedTopic)}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Retake Quiz</span>
            </button>
            <button
              onClick={handleGenerateAiQuiz}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold transition-colors shadow-lg shadow-indigo-600/30"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>Generate New Set</span>
            </button>
            <button
              onClick={onBack}
              className="px-5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 hover:text-white text-xs font-semibold"
            >
              Return to Dashboard
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
