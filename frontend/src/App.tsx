import { useState } from 'react';
import { Header } from './components/Header';
import { HoaxChecker } from './components/HoaxChecker';
import { UrlScanner } from './components/UrlScanner';
import { FileScanner } from './components/FileScanner';
import { HistoryLog } from './components/HistoryLog';
import { Cpu, Globe, FileCode2, History, Shield, Lock, Terminal } from 'lucide-react';

export function App() {
  const [activeTab, setActiveTab] = useState<'hoax' | 'url' | 'file' | 'history'>('hoax');

  const dockNavItems = [
    { id: 'hoax', label: 'AI Hoax', icon: Cpu },
    { id: 'url', label: 'Threat Link', icon: Globe },
    { id: 'file', label: 'File Scan', icon: FileCode2 },
    { id: 'history', label: 'Logs', icon: History },
  ] as const;

  return (
    <div className="min-h-screen bg-[#f4f5f7] text-slate-900 flex flex-col relative pb-28 selection:bg-slate-900 selection:text-white">
      
      {/* Top Header Banner */}
      <div className="bg-white border-b border-slate-200/80 pt-4 sm:pt-6 pb-6 sm:pb-8 shadow-xs">
        <div className="max-w-6xl mx-auto px-3 sm:px-6 lg:px-8">
          <Header
            activeTab={activeTab}
            setActiveTab={setActiveTab}
          />
        </div>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-3 sm:px-6 lg:px-8 pt-5 sm:pt-8 space-y-6 sm:space-y-8">
        
        {/* Dynamic Tab Content */}
        <div className="transition-all duration-300">
          {activeTab === 'hoax' && <HoaxChecker />}
          {activeTab === 'url' && <UrlScanner />}
          {activeTab === 'file' && <FileScanner />}
          {activeTab === 'history' && <HistoryLog />}
        </div>

      </main>

      {/* Floating Bottom Dock Navigation */}
      <div className="fixed bottom-4 sm:bottom-6 left-1/2 -translate-x-1/2 z-50 max-w-[calc(100vw-1.5rem)] px-1">
        <nav className="floating-dock px-2 sm:px-3 py-1.5 sm:py-2 rounded-full flex items-center justify-center gap-1 sm:gap-1.5 shadow-2xl overflow-x-auto no-scrollbar">
          {dockNavItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center justify-center gap-1.5 sm:gap-2 px-3 sm:px-4 py-2 sm:py-2.5 rounded-full text-xs font-extrabold transition-all duration-200 cursor-pointer whitespace-nowrap ${
                  isActive
                    ? 'bg-slate-900 text-white shadow-md shadow-slate-900/20'
                    : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-white' : 'text-slate-500'}`} />
                <span className={`tracking-tight ${isActive ? 'inline-block' : 'hidden sm:inline-block'}`}>
                  {item.label}
                </span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Footer */}
      <footer className="mt-12 sm:mt-16 py-6 border-t border-slate-200 text-xs text-slate-500 text-center">
        <div className="max-w-6xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Shield className="w-4 h-4 text-slate-900" />
            <span className="font-extrabold text-slate-900">Cyber Guard & Hoax Checker</span>
            <span>•</span>
            <span>Local Log Storage</span>
          </div>

          <div className="flex items-center gap-4 text-slate-400">
            <span className="flex items-center gap-1">
              <Lock className="w-3.5 h-3.5 text-slate-700" /> Safe & Verified
            </span>
            <span className="flex items-center gap-1">
              <Terminal className="w-3.5 h-3.5 text-slate-700" /> React + Vite
            </span>
          </div>
        </div>
      </footer>

    </div>
  );
}

export default App;
