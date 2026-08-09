import React, { useEffect, useState } from 'react';
import { History, Search, Trash2, Cpu, Globe, RefreshCw, ChevronRight, X, ShieldCheck, AlertTriangle, XCircle } from 'lucide-react';
import { fetchHistory, clearHistoryApi } from '../services/api';
import { HistoryItem } from '../types';

interface HistoryLogProps {
  externalQuery?: string;
}

export const HistoryLog: React.FC<HistoryLogProps> = ({ externalQuery = '' }) => {
  const [historyItems, setHistoryItems] = useState<HistoryItem[]>([]);
  const [filterType, setFilterType] = useState<'all' | 'hoax' | 'scam'>('all');
  const [searchQuery, setSearchQuery] = useState(externalQuery);
  const [loading, setLoading] = useState(true);
  const [clearing, setClearing] = useState(false);
  const [selectedDetail, setSelectedDetail] = useState<HistoryItem | null>(null);

  const loadHistory = async () => {
    setLoading(true);
    try {
      const data = await fetchHistory();
      setHistoryItems(data);
    } catch (err) {
      console.error('Error loading history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHistory();
  }, []);

  useEffect(() => {
    if (externalQuery) {
      setSearchQuery(externalQuery);
    }
  }, [externalQuery]);

  const handleClear = async () => {
    if (!window.confirm('Apakah Anda yakin ingin menghapus seluruh riwayat pemindaian?')) return;
    setClearing(true);
    try {
      await clearHistoryApi();
      setHistoryItems([]);
    } catch (err) {
      console.error('Error clearing history:', err);
    } finally {
      setClearing(false);
    }
  };

  const filteredItems = historyItems.filter((item) => {
    const matchesType = filterType === 'all' || item.type === filterType;
    const matchesSearch = item.title.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesType && matchesSearch;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'safe':
        return { color: 'text-emerald-700 bg-emerald-50 border-emerald-200', icon: ShieldCheck };
      case 'neutral':
      case 'suspicious':
        return { color: 'text-amber-700 bg-amber-50 border-amber-200', icon: AlertTriangle };
      case 'unsafe':
      case 'malicious':
        return { color: 'text-rose-700 bg-rose-50 border-rose-200', icon: XCircle };
      default:
        return { color: 'text-slate-700 bg-slate-100 border-slate-200', icon: ShieldCheck };
    }
  };

  return (
    <div className="space-y-4 sm:space-y-6">
      
      {/* Header & Filter Controls Card */}
      <div className="card-clean p-4 sm:p-7 space-y-4 sm:space-y-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 sm:gap-4 pb-3 sm:pb-4 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-slate-900 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
              <History className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-extrabold text-slate-900">Live Threat & Hoax Activity Logs</h3>
              <p className="text-[11px] sm:text-xs text-slate-500 font-medium">Catatan riwayat verifikasi hoaks dan ancaman siber</p>
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            <button
              onClick={loadHistory}
              disabled={loading}
              className="p-2 sm:p-2.5 rounded-full bg-slate-100 border border-slate-200 text-slate-700 hover:bg-slate-200 transition cursor-pointer"
              title="Muat Ulang"
            >
              <RefreshCw className={`w-3.5 h-3.5 sm:w-4 sm:h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            
            <button
              onClick={handleClear}
              disabled={clearing || historyItems.length === 0}
              className="flex items-center gap-1.5 px-3.5 py-1.5 sm:px-4 sm:py-2 rounded-full bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 text-xs font-bold disabled:opacity-40 transition cursor-pointer"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Hapus Riwayat</span>
            </button>
          </div>
        </div>

        {/* Filter and Search Bar */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          
          <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-full border border-slate-200 text-[11px] sm:text-xs font-semibold overflow-x-auto no-scrollbar">
            <button
              onClick={() => setFilterType('all')}
              className={`px-3.5 py-1.5 rounded-full transition cursor-pointer whitespace-nowrap ${
                filterType === 'all' ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Semua ({historyItems.length})
            </button>
            <button
              onClick={() => setFilterType('hoax')}
              className={`px-3.5 py-1.5 rounded-full transition cursor-pointer whitespace-nowrap ${
                filterType === 'hoax' ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              AI Hoax ({historyItems.filter(i => i.type === 'hoax').length})
            </button>
            <button
              onClick={() => setFilterType('scam')}
              className={`px-3.5 py-1.5 rounded-full transition cursor-pointer whitespace-nowrap ${
                filterType === 'scam' ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Cyber Threat ({historyItems.filter(i => i.type === 'scam').length})
            </button>
          </div>

          <div className="relative w-full sm:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Cari kata kunci..."
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-full text-slate-900 text-xs focus:outline-none focus:border-slate-400"
            />
          </div>

        </div>
      </div>

      {/* History Item List */}
      <div className="space-y-2.5 sm:space-y-3">
        {loading ? (
          <div className="card-clean p-8 sm:p-12 text-center text-slate-500 text-xs sm:text-sm space-y-2">
            <RefreshCw className="w-5 h-5 sm:w-6 sm:h-6 animate-spin mx-auto text-slate-900" />
            <p className="font-semibold">Mengambil log riwayat dari server...</p>
          </div>
        ) : filteredItems.length === 0 ? (
          <div className="card-clean p-8 sm:p-12 text-center text-slate-500 text-xs sm:text-sm space-y-2">
            <History className="w-7 h-7 sm:w-8 sm:h-8 mx-auto text-slate-400" />
            <p className="font-bold text-slate-900">Belum Ada Riwayat Pemindaian</p>
            <p className="text-xs text-slate-500">Jalankan berita atau pemindaian URL/berkas untuk mencatat log.</p>
          </div>
        ) : (
          filteredItems.map((item) => {
            const badge = getStatusBadge(item.status);
            const StatusIcon = badge.icon;
            return (
              <div
                key={item.id}
                onClick={() => setSelectedDetail(item)}
                className="card-clean p-3.5 sm:p-4 hover:shadow-md transition cursor-pointer flex items-center justify-between gap-3 sm:gap-4 group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className={`p-2 sm:p-2.5 rounded-xl sm:rounded-2xl border ${badge.color} shrink-0`}>
                    <StatusIcon className="w-4 h-4 sm:w-5 sm:h-5" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5 sm:gap-2 mb-0.5 sm:mb-1">
                      <span className={`px-2 sm:px-2.5 py-0.5 rounded-full text-[9px] sm:text-[10px] font-extrabold uppercase ${
                        item.type === 'hoax' ? 'bg-blue-50 text-blue-700 border border-blue-200' : 'bg-purple-50 text-purple-700 border border-purple-200'
                      }`}>
                        {item.type === 'hoax' ? 'HOAX' : 'THREAT'}
                      </span>
                      <span className="text-[10px] sm:text-[11px] text-slate-400 font-mono truncate">{item.timestamp}</span>
                    </div>
                    <h4 className="text-xs sm:text-sm font-bold text-slate-900 truncate group-hover:text-blue-600 transition">
                      {item.title}
                    </h4>
                  </div>
                </div>

                <div className="flex items-center gap-2 sm:gap-3 shrink-0">
                  <div className="text-right hidden sm:block">
                    <div className="text-[10px] text-slate-400 font-bold uppercase">Skor</div>
                    <div className="text-xs sm:text-sm font-black text-slate-900">{item.score}/100</div>
                  </div>
                  <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-slate-100 group-hover:bg-slate-900 group-hover:text-white text-slate-600 flex items-center justify-center transition">
                    <ChevronRight className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Item Detail Modal */}
      {selectedDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-sm animate-fadeIn">
          <div className="card-clean w-full max-w-2xl max-h-[90vh] shadow-2xl flex flex-col overflow-hidden">
            
            <div className="flex items-center justify-between p-4 sm:p-5 border-b border-slate-100">
              <div className="flex items-center gap-2 min-w-0 pr-2">
                {selectedDetail.type === 'hoax' ? <Cpu className="w-4 h-4 sm:w-5 sm:h-5 text-slate-900 shrink-0" /> : <Globe className="w-4 h-4 sm:w-5 sm:h-5 text-slate-900 shrink-0" />}
                <h3 className="text-xs sm:text-base font-extrabold text-slate-900 truncate">Detail Log: {selectedDetail.title}</h3>
              </div>
              <button
                onClick={() => setSelectedDetail(null)}
                className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 flex items-center justify-center transition cursor-pointer shrink-0"
              >
                <X className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              </button>
            </div>

            <div className="p-4 sm:p-6 overflow-y-auto space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-3 p-3 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200">
                <div>
                  <span className="text-slate-400 font-bold uppercase text-[10px]">Tipe Pemeriksaan:</span>
                  <p className="text-slate-900 font-extrabold uppercase mt-0.5 text-xs">{selectedDetail.type}</p>
                </div>
                <div>
                  <span className="text-slate-400 font-bold uppercase text-[10px]">Status / Skor:</span>
                  <p className="text-slate-900 font-extrabold mt-0.5 text-xs">{selectedDetail.status.toUpperCase()} ({selectedDetail.score}/100)</p>
                </div>
              </div>

              <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
                <span className="text-slate-400 font-bold uppercase text-[10px]">Data Hasil Rinci (JSON):</span>
                <pre className="p-3 sm:p-4 rounded-xl bg-slate-950 text-emerald-400 font-mono text-[10px] sm:text-[11px] overflow-x-auto whitespace-pre-wrap max-h-64 sm:max-h-72 border border-slate-800">
                  {JSON.stringify(selectedDetail.resultDetails, null, 2)}
                </pre>
              </div>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};
