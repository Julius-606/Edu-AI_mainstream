import React, { useState } from 'react';
import { X, ArrowLeft, ArrowRight, RotateCw, ExternalLink, Globe, BookOpen } from 'lucide-react';

interface InAppBrowserProps {
  url: string;
  onClose: () => void;
}

export const InAppBrowser: React.FC<InAppBrowserProps> = ({ url: initialUrl, onClose }) => {
  const [currentUrl, setCurrentUrl] = useState(initialUrl || 'https://en.wikipedia.org/wiki/Medicine');
  const [inputUrl, setInputUrl] = useState(currentUrl);

  const quickLinks = [
    { label: 'NCBI PubMed', url: 'https://pubmed.ncbi.nlm.nih.gov' },
    { label: 'Wikipedia Medicine', url: 'https://en.wikipedia.org/wiki/Portal:Medicine' },
    { label: 'StatPearls', url: 'https://www.ncbi.nlm.nih.gov/books/NBK430685/' },
    { label: 'WHO Guidelines', url: 'https://www.who.int' }
  ];

  const handleNavigate = (e: React.FormEvent) => {
    e.preventDefault();
    let dest = inputUrl.trim();
    if (!dest.startsWith('http://') && !dest.startsWith('https://')) {
      dest = 'https://' + dest;
    }
    setCurrentUrl(dest);
    setInputUrl(dest);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-5xl h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Browser Top Navigation Bar */}
        <div className="bg-slate-950 border-b border-slate-800 px-4 py-3 flex items-center gap-3">
          <div className="flex items-center gap-1 text-slate-400">
            <button
              onClick={() => {}}
              className="p-1.5 rounded-lg hover:bg-slate-800 hover:text-white transition-colors"
              title="Back"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => {}}
              className="p-1.5 rounded-lg hover:bg-slate-800 hover:text-white transition-colors"
              title="Forward"
            >
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={() => {
                const temp = currentUrl;
                setCurrentUrl('');
                setTimeout(() => setCurrentUrl(temp), 50);
              }}
              className="p-1.5 rounded-lg hover:bg-slate-800 hover:text-white transition-colors"
              title="Reload"
            >
              <RotateCw className="w-4 h-4" />
            </button>
          </div>

          {/* Address bar */}
          <form onSubmit={handleNavigate} className="flex-1 flex items-center gap-2 bg-slate-900 border border-slate-700/80 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus-within:border-indigo-500 transition-colors">
            <Globe className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <input
              type="text"
              value={inputUrl}
              onChange={(e) => setInputUrl(e.target.value)}
              className="w-full bg-transparent outline-none text-slate-200 placeholder-slate-500 font-mono text-xs"
              placeholder="Enter URL (https://...)"
            />
          </form>

          {/* External open and close */}
          <div className="flex items-center gap-1.5">
            <a
              href={currentUrl}
              target="_blank"
              rel="noreferrer"
              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
              title="Open in new window"
            >
              <ExternalLink className="w-4 h-4" />
            </a>
            <button
              onClick={onClose}
              className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-red-950/40 rounded-lg transition-colors"
              title="Close browser"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Quick Reference Shortcuts */}
        <div className="bg-slate-950/70 border-b border-slate-800/80 px-4 py-1.5 flex items-center gap-2 overflow-x-auto text-xs text-slate-400">
          <BookOpen className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
          <span className="text-[11px] font-medium text-slate-500 mr-1">Trace Reference:</span>
          {quickLinks.map((item) => (
            <button
              key={item.label}
              onClick={() => {
                setCurrentUrl(item.url);
                setInputUrl(item.url);
              }}
              className="px-2.5 py-0.5 rounded-full bg-slate-800/80 hover:bg-indigo-950 hover:text-indigo-300 transition-colors whitespace-nowrap text-[11px]"
            >
              {item.label}
            </button>
          ))}
        </div>

        {/* Browser Content */}
        <div className="flex-1 bg-slate-950 relative">
          {currentUrl ? (
            <iframe
              src={currentUrl}
              title="Trace In-App Browser"
              className="w-full h-full border-none bg-white"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
            />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-500">
              Loading source...
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
