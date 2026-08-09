import React from 'react';
import { ShieldCheck, Cpu, Globe } from 'lucide-react';

interface HeaderProps {
  activeTab: 'hoax' | 'url' | 'file' | 'history';
  setActiveTab: (tab: 'hoax' | 'url' | 'file' | 'history') => void;
}

export const Header: React.FC<HeaderProps> = ({ activeTab, setActiveTab }) => {
  return (
    <header className="space-y-4 sm:space-y-6">
      
      {/* Profile Top Greeting Row */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-full bg-slate-900 text-white flex items-center justify-center font-bold text-sm shadow-md ring-2 ring-white shrink-0">
          U
        </div>
        <div>
          <h2 className="text-sm sm:text-base font-bold text-slate-900 tracking-tight">
            Hello, User
          </h2>
          <p className="text-[11px] sm:text-xs text-slate-500 font-medium">Cyber Threat & Hoax Intelligence Hub</p>
        </div>
      </div>

      {/* High-Contrast Pitch Black Hero Card */}
      <div className="card-hero-dark p-5 sm:p-7 relative overflow-hidden">
        <div className="absolute top-0 right-0 -mt-10 -mr-10 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none"></div>

        <div className="flex flex-col md:flex-row md:items-center justify-between gap-5 sm:gap-6 relative z-10">
          
          <div className="space-y-1.5 sm:space-y-2">
            <div className="flex items-center gap-2 text-[11px] sm:text-xs font-semibold text-slate-400 uppercase tracking-wider">
              <ShieldCheck className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-emerald-400 shrink-0" />
              <span>Real-time Security Shield</span>
            </div>
            <h3 className="text-xl sm:text-3xl font-extrabold text-white tracking-tight">
              AI Hoax & Threat Guard
            </h3>
            <p className="text-xs text-slate-300 max-w-lg leading-relaxed">
              Verifikasi narasi berita palsu dengan Multi-LLM AI serta pindai tautan phishing & malware berkas melalui engine VirusTotal.
            </p>
          </div>

          {/* Action Buttons inside Hero Card */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 sm:gap-3 w-full md:w-auto">
            <button
              onClick={() => setActiveTab('hoax')}
              className={`flex items-center justify-center gap-2 px-5 py-2.5 rounded-full font-bold text-xs transition shadow-md cursor-pointer ${
                activeTab === 'hoax'
                  ? 'bg-white text-slate-950 shadow-white/20'
                  : 'bg-slate-800 text-slate-200 hover:bg-slate-700'
              }`}
            >
              <Cpu className="w-4 h-4" />
              <span>AI Hoax Check</span>
            </button>

            <button
              onClick={() => setActiveTab('url')}
              className={`flex items-center justify-center gap-2 px-5 py-2.5 rounded-full font-bold text-xs transition shadow-md cursor-pointer ${
                activeTab === 'url'
                  ? 'bg-white text-slate-950 shadow-white/20'
                  : 'bg-slate-800 text-slate-200 hover:bg-slate-700'
              }`}
            >
              <Globe className="w-4 h-4" />
              <span>Scan Threat Link</span>
            </button>
          </div>

        </div>
      </div>

    </header>
  );
};
