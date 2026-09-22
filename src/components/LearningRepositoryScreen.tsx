import React, { useState } from 'react';
import {
  Bookmark as BookmarkIcon,
  Trash2,
  ExternalLink,
  Search,
  ArrowLeft,
  BookOpen
} from 'lucide-react';
import { Bookmark } from '../types';
import { TraceStore } from '../lib/store';

interface LearningRepositoryScreenProps {
  onBack: () => void;
  onOpenSubtopicByName: (subtopicName: string) => void;
}

export const LearningRepositoryScreen: React.FC<LearningRepositoryScreenProps> = ({
  onBack,
  onOpenSubtopicByName
}) => {
  const [bookmarks, setBookmarks] = useState<Bookmark[]>(TraceStore.getBookmarks());
  const [search, setSearch] = useState('');

  const handleDelete = (id: string) => {
    const updated = TraceStore.deleteBookmark(id);
    setBookmarks(updated);
  };

  const filtered = bookmarks.filter(
    (b) =>
      b.subtopicName.toLowerCase().includes(search.toLowerCase()) ||
      b.objectiveDescription.toLowerCase().includes(search.toLowerCase()) ||
      (b.notes && b.notes.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-200">
      {/* Header */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-amber-400">
              Personal Vault
            </span>
            <h1 className="text-2xl font-black text-white mt-0.5">Learning Repository & Bookmarks</h1>
            <p className="text-xs text-slate-400">Saved clinical takeaways, high-yield mechanisms, and notes</p>
          </div>
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search saved bookmarks..."
            className="w-full bg-slate-950 border border-slate-800 rounded-2xl pl-10 pr-4 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-indigo-500"
          />
        </div>
      </div>

      {/* Bookmarks List */}
      <div className="space-y-3">
        {filtered.length > 0 ? (
          filtered.map((b) => (
            <div
              key={b.id}
              className="p-5 rounded-3xl bg-slate-900/80 border border-slate-800/90 hover:border-slate-700 transition-all shadow-md flex flex-col sm:flex-row sm:items-start justify-between gap-4"
            >
              <div className="space-y-2 flex-1">
                <div className="flex items-center gap-2">
                  <BookmarkIcon className="w-4 h-4 text-amber-400 shrink-0" />
                  <h3 className="text-sm font-bold text-white">{b.subtopicName}</h3>
                </div>

                <p className="text-xs font-semibold text-indigo-300">{b.objectiveDescription}</p>

                <p className="text-xs text-slate-300 italic bg-slate-950/60 p-3 rounded-xl border border-slate-800/80 leading-relaxed">
                  "{b.excerpt}"
                </p>

                {b.notes && (
                  <p className="text-xs text-amber-200/90 font-medium">
                    <strong>My Note:</strong> {b.notes}
                  </p>
                )}

                <span className="text-[10px] text-slate-500 block">
                  Saved on {new Date(b.timestamp).toLocaleDateString()}
                </span>
              </div>

              {/* Actions */}
              <div className="flex sm:flex-col items-center justify-end gap-2 shrink-0">
                <button
                  onClick={() => onOpenSubtopicByName(b.subtopicName)}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                  <span>Open Topic</span>
                </button>
                <button
                  onClick={() => handleDelete(b.id)}
                  className="p-2 rounded-xl bg-slate-800 hover:bg-red-950/40 text-slate-400 hover:text-red-400 transition-colors"
                  title="Remove bookmark"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="p-12 text-center text-slate-500 bg-slate-900/40 rounded-3xl border border-slate-800">
            No bookmarks found. Use the bookmark icon on any learning subtopic to save high-yield notes.
          </div>
        )}
      </div>
    </div>
  );
};
