import React from 'react';

export interface RingProgressProps {
  progress: number; // 0 to 100
  color: string;
  label: string;
}

interface ProgressRingsProps {
  unitProgress: number;
  moduleProgress: number;
  topicProgress: number;
  size?: number;
}

export const ProgressRings: React.FC<ProgressRingsProps> = ({
  unitProgress,
  moduleProgress,
  topicProgress,
  size = 140
}) => {
  const strokeWidth = 8;
  const center = size / 2;

  // Concentric radii
  const r1 = center - strokeWidth;
  const r2 = r1 - strokeWidth - 4;
  const r3 = r2 - strokeWidth - 4;

  const circ1 = 2 * Math.PI * r1;
  const circ2 = 2 * Math.PI * r2;
  const circ3 = 2 * Math.PI * r3;

  const offset1 = circ1 - (Math.min(100, Math.max(0, unitProgress)) / 100) * circ1;
  const offset2 = circ2 - (Math.min(100, Math.max(0, moduleProgress)) / 100) * circ2;
  const offset3 = circ3 - (Math.min(100, Math.max(0, topicProgress)) / 100) * circ3;

  return (
    <div className="relative flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="transform -rotate-90">
        {/* Background track rings */}
        <circle
          cx={center}
          cy={center}
          r={r1}
          fill="none"
          stroke="currentColor"
          className="text-slate-800/60"
          strokeWidth={strokeWidth}
        />
        <circle
          cx={center}
          cy={center}
          r={r2}
          fill="none"
          stroke="currentColor"
          className="text-slate-800/60"
          strokeWidth={strokeWidth}
        />
        <circle
          cx={center}
          cy={center}
          r={r3}
          fill="none"
          stroke="currentColor"
          className="text-slate-800/60"
          strokeWidth={strokeWidth}
        />

        {/* Outer Ring - Unit (Indigo) */}
        <circle
          cx={center}
          cy={center}
          r={r1}
          fill="none"
          stroke="#6366f1"
          strokeWidth={strokeWidth}
          strokeDasharray={circ1}
          strokeDashoffset={offset1}
          strokeLinecap="round"
          className="transition-all duration-700 ease-out"
        />

        {/* Middle Ring - Module (Emerald) */}
        <circle
          cx={center}
          cy={center}
          r={r2}
          fill="none"
          stroke="#10b981"
          strokeWidth={strokeWidth}
          strokeDasharray={circ2}
          strokeDashoffset={offset2}
          strokeLinecap="round"
          className="transition-all duration-700 ease-out"
        />

        {/* Inner Ring - Topic (Amber) */}
        <circle
          cx={center}
          cy={center}
          r={r3}
          fill="none"
          stroke="#f59e0b"
          strokeWidth={strokeWidth}
          strokeDasharray={circ3}
          strokeDashoffset={offset3}
          strokeLinecap="round"
          className="transition-all duration-700 ease-out"
        />
      </svg>

      {/* Center percentage summary */}
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
        <span className="text-lg font-black text-white leading-none">
          {Math.round(unitProgress)}%
        </span>
        <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mt-0.5">
          Mastery
        </span>
      </div>
    </div>
  );
};
