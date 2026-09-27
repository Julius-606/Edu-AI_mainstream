import React, { useState } from 'react';
import Markdown from 'react-markdown';
import { Play, ExternalLink, Globe, Video, Copy, Check, Tv, Compass } from 'lucide-react';

export interface TextHighlightItem {
  id: string;
  text: string;
  color: string; // e.g. '#FEF08A' (yellow), '#86EFAC' (green), '#7DD3FC' (cyan), '#D8B4FE' (purple), '#FDA4AF' (coral)
  note?: string;
}

interface FormattedTextProps {
  text: string;
  className?: string;
  onLinkClick?: (url: string) => void;
  highlights?: TextHighlightItem[];
}

function extractYouTubeId(url: string): string | null {
  const match = url.match(/(?:youtube\.com\/(?:watch\?v=|embed\/|shorts\/)|youtu\.be\/)([\w-]{11})/i);
  return match ? match[1] : null;
}

function extractDomain(url: string): string {
  try {
    const parsed = new URL(url);
    return parsed.hostname.replace(/^www\./, '');
  } catch {
    return 'web link';
  }
}

// Automatically wraps raw/bare URLs with Markdown link syntax so they render as rich preview cards
function linkifyRawUrls(content: string): string {
  if (!content) return '';
  // Avoid re-wrapping URLs that are already part of markdown links [text](url) or HTML tags
  return content.replace(
    /(?<![\]\(=][\s]*)(https?:\/\/[^\s<>)"]+)/gi,
    (match) => `[${match}](${match})`
  );
}

const YouTubePreviewCard: React.FC<{ url: string; title?: React.ReactNode; onLinkClick?: (url: string) => void }> = ({
  url,
  title,
  onLinkClick
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [copied, setCopied] = useState(false);
  const videoId = extractYouTubeId(url);

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard?.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!videoId) {
    return (
      <a
        href={url}
        target="_blank"
        rel="noreferrer"
        onClick={(e) => {
          if (onLinkClick) {
            e.preventDefault();
            onLinkClick(url);
          }
        }}
        className="inline-flex items-center gap-1.5 text-red-400 hover:text-red-300 font-semibold underline"
      >
        <Video className="w-4 h-4 text-red-400 shrink-0" />
        <span>{title || url}</span>
      </a>
    );
  }

  const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
  const displayTitle = typeof title === 'string' && title !== url ? title : 'Clinical Lecture & Visual Breakdown';

  return (
    <div className="my-3.5 not-prose w-full rounded-2xl overflow-hidden border border-red-900/50 bg-slate-900/95 shadow-2xl transition-all hover:border-red-500/70">
      {/* Video Display Area */}
      {isPlaying ? (
        <div className="relative aspect-video w-full bg-black">
          <iframe
            src={`https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&rel=0`}
            title="YouTube video player"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            className="w-full h-full border-0"
          />
        </div>
      ) : (
        <div
          onClick={() => setIsPlaying(true)}
          className="relative aspect-video w-full bg-slate-950 cursor-pointer group overflow-hidden"
        >
          <img
            src={thumbnailUrl}
            alt="YouTube Video Preview"
            className="w-full h-full object-cover opacity-85 group-hover:opacity-100 group-hover:scale-105 transition-all duration-300"
            onError={(e) => {
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/40 to-transparent" />
          
          {/* Centered Play Button */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="w-16 h-16 rounded-2xl bg-red-600/90 text-white flex items-center justify-center shadow-2xl group-hover:bg-red-500 group-hover:scale-110 transition-all border border-red-400/40">
              <Play className="w-8 h-8 fill-white translate-x-0.5" />
            </div>
          </div>

          {/* Top Header Labels */}
          <div className="absolute top-3 left-3 flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-black/80 backdrop-blur-md border border-white/10 text-[10px] font-bold tracking-wider uppercase text-red-300 shadow">
            <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
            <Tv className="w-3.5 h-3.5" />
            <span>Interactive Lecture</span>
          </div>

          <div className="absolute bottom-3 right-3 flex items-center gap-2">
            <span className="px-2 py-0.5 rounded bg-black/80 text-[10px] font-mono text-slate-300 border border-white/10">
              YouTube 1080p
            </span>
          </div>
        </div>
      )}

      {/* Info & Action Controls */}
      <div className="p-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-900 border-t border-slate-800/80">
        <div className="flex-1 min-w-0">
          <h4 className="text-xs sm:text-sm font-bold text-white truncate">
            {displayTitle}
          </h4>
          <p className="text-[11px] text-slate-400 truncate mt-0.5 flex items-center gap-1.5">
            <span className="text-red-400 font-semibold flex items-center gap-1">
              <Video className="w-3 h-3" /> youtube.com
            </span>
            <span>•</span>
            <span className="truncate opacity-80">{url}</span>
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={handleCopy}
            className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors text-xs flex items-center gap-1"
            title="Copy Video Link"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>

          {onLinkClick && (
            <button
              onClick={() => onLinkClick(url)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold transition-all"
              title="Open in in-app reader"
            >
              <Compass className="w-3.5 h-3.5 text-indigo-400" />
              <span>In-App View</span>
            </button>
          )}

          <button
            onClick={() => window.open(url, '_blank', 'noopener,noreferrer')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-600/20 hover:bg-red-600/30 text-red-300 border border-red-500/40 text-xs font-bold transition-all shadow-sm"
          >
            <span>Watch External</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};

const WebReferenceCard: React.FC<{ url: string; title?: React.ReactNode; onLinkClick?: (url: string) => void }> = ({
  url,
  title,
  onLinkClick
}) => {
  const [copied, setCopied] = useState(false);
  const domain = extractDomain(url);
  const displayTitle = typeof title === 'string' && title !== url ? title : domain;

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard?.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="my-2.5 not-prose w-full rounded-xl border border-slate-800 bg-slate-900/90 p-3.5 hover:border-indigo-500/50 hover:bg-slate-900 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 group shadow-md">
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-10 h-10 rounded-xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
          <Globe className="w-5 h-5" />
        </div>
        <div className="min-w-0 flex-1">
          <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-400 block truncate">
            {domain}
          </span>
          <p className="text-xs sm:text-sm font-bold text-white group-hover:text-indigo-200 transition-colors truncate">
            {displayTitle}
          </p>
          <p className="text-[10px] text-slate-400 truncate opacity-80 mt-0.5">
            {url}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0 self-end sm:self-auto">
        <button
          onClick={handleCopy}
          className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors text-xs flex items-center gap-1"
          title="Copy Link"
        >
          {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
        </button>

        {onLinkClick && (
          <button
            onClick={() => onLinkClick(url)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold transition-all"
          >
            <Compass className="w-3.5 h-3.5 text-indigo-400" />
            <span>Reader</span>
          </button>
        )}

        <button
          onClick={() => window.open(url, '_blank', 'noopener,noreferrer')}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600/20 text-indigo-300 hover:bg-indigo-600 hover:text-white border border-indigo-500/30 text-xs font-semibold transition-all shadow-sm"
        >
          <span>Open Web</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};

// Render highlighted text spans if highlights are present
function highlightContent(content: React.ReactNode, highlights: TextHighlightItem[]): React.ReactNode {
  if (!highlights || highlights.length === 0 || typeof content !== 'string') {
    return content;
  }

  // Create regex from highlighted phrases
  const escaped = highlights
    .map((h) => h.text.trim())
    .filter(Boolean)
    .sort((a, b) => b.length - a.length)
    .map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));

  if (escaped.length === 0) return content;

  const regex = new RegExp(`(${escaped.join('|')})`, 'gi');
  const parts = content.split(regex);

  return parts.map((part, i) => {
    const matchedHl = highlights.find((h) => h.text.trim().toLowerCase() === part.toLowerCase());
    if (matchedHl) {
      return (
        <mark
          key={i}
          className="rounded px-1 py-0.5 font-semibold text-white shadow-sm"
          style={{ backgroundColor: `${matchedHl.color}55`, borderBottom: `2px solid ${matchedHl.color}` }}
          title={matchedHl.note || 'Study Highlight'}
        >
          {part}
        </mark>
      );
    }
    return part;
  });
}

export const FormattedText: React.FC<FormattedTextProps> = ({
  text,
  className = '',
  onLinkClick,
  highlights = []
}) => {
  const processedText = linkifyRawUrls(text);

  return (
    <div className={`prose prose-invert prose-indigo max-w-none w-full text-slate-200 ${className}`}>
      <Markdown
        components={{
          h1: ({ children }) => (
            <h1 className="text-xl sm:text-2xl font-bold text-slate-100 mt-4 mb-2 tracking-tight">
              {highlightContent(children, highlights)}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-lg sm:text-xl font-bold text-indigo-300 mt-3 mb-1.5 tracking-tight">
              {highlightContent(children, highlights)}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-base sm:text-lg font-semibold text-emerald-400 mt-2 mb-1">
              {highlightContent(children, highlights)}
            </h3>
          ),
          p: ({ children }) => (
            <p className="mb-2 leading-relaxed text-slate-300 text-xs sm:text-sm">
              {highlightContent(children, highlights)}
            </p>
          ),
          ul: ({ children }) => (
            <ul className="list-disc list-outside pl-4 space-y-1 mb-2.5 text-xs sm:text-sm text-slate-300">
              {children}
            </ul>
          ),
          ol: ({ children }) => (
            <ol className="list-decimal list-outside pl-4 space-y-1 mb-2.5 text-xs sm:text-sm text-slate-300">
              {children}
            </ol>
          ),
          li: ({ children }) => (
            <li className="leading-snug">
              {highlightContent(children, highlights)}
            </li>
          ),
          strong: ({ children }) => (
            <strong className="font-bold text-white">
              {highlightContent(children, highlights)}
            </strong>
          ),
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-indigo-500 pl-3 my-2 text-slate-400 italic text-xs bg-indigo-950/20 py-1.5 rounded-r">
              {highlightContent(children, highlights)}
            </blockquote>
          ),
          code: ({ children }) => (
            <code className="px-1.5 py-0.5 rounded bg-slate-800 text-indigo-300 font-mono text-xs border border-slate-700/60">
              {children}
            </code>
          ),
          a: ({ href, children }) => {
            if (!href) return <span>{children}</span>;

            const isYouTube = href.includes('youtube.com') || href.includes('youtu.be');
            if (isYouTube) {
              return <YouTubePreviewCard url={href} title={children} onLinkClick={onLinkClick} />;
            }

            if (href.startsWith('http://') || href.startsWith('https://')) {
              return <WebReferenceCard url={href} title={children} onLinkClick={onLinkClick} />;
            }

            return (
              <a
                href={href}
                onClick={(e) => {
                  if (onLinkClick && href) {
                    e.preventDefault();
                    onLinkClick(href);
                  }
                }}
                target="_blank"
                rel="noreferrer"
                className="text-indigo-400 underline decoration-indigo-500/50 hover:text-indigo-300 transition-colors"
              >
                {children}
              </a>
            );
          }
        }}
      >
        {processedText}
      </Markdown>
    </div>
  );
};
