import React from 'react';

export const DynamicBackground: React.FC = () => {
  return (
    <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10 bg-slate-950">
      {/* Luminous atmospheric glows */}
      <div 
        className="absolute -top-40 -left-40 w-[600px] h-[600px] rounded-full bg-indigo-600/10 blur-[140px]" 
      />
      <div 
        className="absolute top-1/3 -right-40 w-[550px] h-[550px] rounded-full bg-emerald-600/10 blur-[140px]" 
      />
      <div 
        className="absolute -bottom-40 left-1/3 w-[650px] h-[650px] rounded-full bg-blue-600/10 blur-[150px]" 
      />
      {/* Subtle high-tech grid overlay */}
      <div 
        className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b08_1px,transparent_1px),linear-gradient(to_bottom,#1e293b08_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)]" 
      />
    </div>
  );
};
