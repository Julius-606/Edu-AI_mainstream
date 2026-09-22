import React from 'react';
import Markdown from 'react-markdown';

interface FormattedTextProps {
  text: string;
  className?: string;
  onLinkClick?: (url: string) => void;
}

export const FormattedText: React.FC<FormattedTextProps> = ({
  text,
  className = '',
  onLinkClick
}) => {
  return (
    <div className={`prose prose-invert prose-indigo max-w-none text-slate-200 ${className}`}>
      <Markdown
        components={{
          h1: ({ children }) => (
            <h1 className="text-xl font-bold text-slate-100 mt-4 mb-2 tracking-tight">
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-lg font-bold text-indigo-300 mt-3 mb-1.5 tracking-tight">
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-base font-semibold text-emerald-400 mt-2 mb-1">
              {children}
            </h3>
          ),
          p: ({ children }) => (
            <p className="mb-2 leading-relaxed text-slate-300 text-sm">{children}</p>
          ),
          ul: ({ children }) => (
            <ul className="list-disc list-outside pl-4 space-y-1 mb-2.5 text-sm text-slate-300">
              {children}
            </ul>
          ),
          ol: ({ children }) => (
            <ol className="list-decimal list-outside pl-4 space-y-1 mb-2.5 text-sm text-slate-300">
              {children}
            </ol>
          ),
          li: ({ children }) => <li className="leading-snug">{children}</li>,
          strong: ({ children }) => (
            <strong className="font-semibold text-white">{children}</strong>
          ),
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-indigo-500 pl-3 my-2 text-slate-400 italic text-xs bg-indigo-950/20 py-1.5 rounded-r">
              {children}
            </blockquote>
          ),
          code: ({ children }) => (
            <code className="px-1.5 py-0.5 rounded bg-slate-800 text-indigo-300 font-mono text-xs border border-slate-700/60">
              {children}
            </code>
          ),
          a: ({ href, children }) => (
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
          )
        }}
      >
        {text}
      </Markdown>
    </div>
  );
};
