import React from 'react';
import { Cpu, Globe, ShieldCheck, Activity } from 'lucide-react';

export const StatOverview: React.FC = () => {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      
      <div className="card-clean p-3.5 sm:p-4 flex items-center justify-between hover:shadow-md transition">
        <div className="flex items-center gap-2.5 sm:gap-3.5 min-w-0">
          <div className="w-9 h-9 sm:w-11 sm:h-11 rounded-xl sm:rounded-2xl bg-slate-900 text-white flex items-center justify-center shrink-0 shadow-sm">
            <Cpu className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-[10px] sm:text-[11px] font-semibold text-slate-400 uppercase tracking-wider truncate">Multi-LLM AI</div>
            <div className="text-xs sm:text-sm font-extrabold text-slate-900 truncate">Gemini & DeepSeek</div>
          </div>
        </div>
        <span className="hidden sm:inline-block px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-blue-50 text-blue-700 border border-blue-100 shrink-0">
          ACTIVE
        </span>
      </div>

      <div className="card-clean p-3.5 sm:p-4 flex items-center justify-between hover:shadow-md transition">
        <div className="flex items-center gap-2.5 sm:gap-3.5 min-w-0">
          <div className="w-9 h-9 sm:w-11 sm:h-11 rounded-xl sm:rounded-2xl bg-slate-900 text-white flex items-center justify-center shrink-0 shadow-sm">
            <Globe className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-[10px] sm:text-[11px] font-semibold text-slate-400 uppercase tracking-wider truncate">VirusTotal</div>
            <div className="text-xs sm:text-sm font-extrabold text-slate-900 truncate">70+ Cyber Engines</div>
          </div>
        </div>
        <span className="hidden sm:inline-block px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-100 shrink-0">
          READY
        </span>
      </div>

      <div className="card-clean p-3.5 sm:p-4 flex items-center justify-between hover:shadow-md transition">
        <div className="flex items-center gap-2.5 sm:gap-3.5 min-w-0">
          <div className="w-9 h-9 sm:w-11 sm:h-11 rounded-xl sm:rounded-2xl bg-slate-900 text-white flex items-center justify-center shrink-0 shadow-sm">
            <ShieldCheck className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-[10px] sm:text-[11px] font-semibold text-slate-400 uppercase tracking-wider truncate">Google Fact Check</div>
            <div className="text-xs sm:text-sm font-extrabold text-slate-900 truncate">Global Claim API</div>
          </div>
        </div>
        <span className="hidden sm:inline-block px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-purple-50 text-purple-700 border border-purple-100 shrink-0">
          SYNCED
        </span>
      </div>

      <div className="card-clean p-3.5 sm:p-4 flex items-center justify-between hover:shadow-md transition">
        <div className="flex items-center gap-2.5 sm:gap-3.5 min-w-0">
          <div className="w-9 h-9 sm:w-11 sm:h-11 rounded-xl sm:rounded-2xl bg-slate-900 text-white flex items-center justify-center shrink-0 shadow-sm">
            <Activity className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-[10px] sm:text-[11px] font-semibold text-slate-400 uppercase tracking-wider truncate">Notification Guard</div>
            <div className="text-xs sm:text-sm font-extrabold text-slate-900 truncate">Auto Threat Log</div>
          </div>
        </div>
        <span className="hidden sm:inline-block px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-amber-50 text-amber-700 border border-amber-100 shrink-0">
          LIVE
        </span>
      </div>

    </div>
  );
};
