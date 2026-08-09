import React, { useState } from 'react';
import { Globe, Search, ShieldCheck, AlertOctagon, RefreshCw, XCircle, Tag, Network, CheckCircle2 } from 'lucide-react';
import { scanUrl } from '../services/api';
import { ScamScanResult } from '../types';

export const UrlScanner: React.FC = () => {
  const [urlInput, setUrlInput] = useState('');
  const [enableWebScraping, setEnableWebScraping] = useState(true);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ScamScanResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    let formattedUrl = urlInput.trim();
    if (!formattedUrl) {
      setError('Masukkan alamat URL tautan (link) yang ingin Anda periksa.');
      return;
    }

    if (!/^https?:\/\//i.test(formattedUrl)) {
      formattedUrl = 'https://' + formattedUrl;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await scanUrl({
        url: formattedUrl,
        enableWebScraping,
      });
      setResult(data);
    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.message ||
        'Gagal melakukan pemindaian URL. Pastikan backend VirusTotal API dikonfigurasi di file .env.'
      );
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string, score: number) => {
    if (status === 'safe' || score === 0) {
      return {
        bg: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        icon: ShieldCheck,
        label: 'TAUTAN AMAN & BERSIH',
      };
    }
    if (status === 'suspicious' || (score > 0 && score < 3)) {
      return {
        bg: 'bg-amber-50 text-amber-700 border-amber-200',
        icon: AlertOctagon,
        label: 'TAUTAN MENCURIGAKAN',
      };
    }
    return {
      bg: 'bg-rose-50 text-rose-700 border-rose-200',
      icon: XCircle,
      label: 'TAUTAN DANGER / MALICIOUS / PHISHING',
    };
  };

  return (
    <div className="space-y-4 sm:space-y-6">
      
      {/* Input Card */}
      <div className="card-clean p-4 sm:p-7 space-y-4 sm:space-y-6">
        
        <div className="flex items-center gap-3 pb-3 sm:pb-4 border-b border-slate-100">
          <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-slate-900 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
            <Globe className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-extrabold text-slate-900">VirusTotal URL Threat Scanner</h3>
            <p className="text-[11px] sm:text-xs text-slate-500 font-medium">Deteksi malware link, situs web phishing, & DNS blacklist</p>
          </div>
        </div>

        <form onSubmit={handleScan} className="space-y-4">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 sm:pl-4 flex items-center pointer-events-none text-slate-400">
              <Globe className="w-4 h-4" />
            </div>
            <input
              type="text"
              value={urlInput}
              onChange={(e) => setUrlInput(e.target.value)}
              placeholder="Contoh: https://danamasuk-bantuan.online atau bit.ly/promo-hadiah"
              className="w-full pl-10 sm:pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 placeholder-slate-400 text-xs focus:outline-none focus:border-slate-400 focus:ring-1 focus:ring-slate-400 transition"
            />
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-3 sm:p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80">
            <label className="flex items-center gap-2.5 text-xs text-slate-700 font-medium cursor-pointer">
              <input
                type="checkbox"
                checked={enableWebScraping}
                onChange={(e) => setEnableWebScraping(e.target.checked)}
                className="w-4 h-4 rounded border-slate-300 text-slate-900 focus:ring-slate-900 shrink-0"
              />
              <span>Aktifkan Deep Web Scraping & Inspection</span>
            </label>

            <button
              type="submit"
              disabled={loading}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-2.5 rounded-full bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-md disabled:opacity-50 transition cursor-pointer"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin text-white" />
                  <span>Memindai URL...</span>
                </>
              ) : (
                <>
                  <Search className="w-4 h-4 text-white" />
                  <span>Pindai URL Sekarang</span>
                </>
              )}
            </button>
          </div>
        </form>

        {error && (
          <div className="p-3.5 sm:p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2.5">
            <XCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span className="font-semibold leading-relaxed">{error}</span>
          </div>
        )}
      </div>

      {/* Result Card */}
      {result && (
        <div className="card-clean p-4 sm:p-7 space-y-4 sm:space-y-6 animate-fadeIn">
          
          {(() => {
            const badge = getStatusBadge(result.status, result.threatScore);
            const BadgeIcon = badge.icon;
            return (
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 sm:pb-6 border-b border-slate-100">
                <div className="flex items-center gap-3">
                  <div className={`p-2.5 sm:p-3 rounded-2xl border ${badge.bg} shrink-0`}>
                    <BadgeIcon className="w-5 h-5 sm:w-6 sm:h-6" />
                  </div>
                  <div className="min-w-0">
                    <span className={`inline-block px-2.5 sm:px-3 py-0.5 rounded-full text-[10px] font-extrabold uppercase border mb-1 ${badge.bg}`}>
                      {badge.label}
                    </span>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900 font-mono break-all leading-snug">{result.target}</h3>
                  </div>
                </div>

                <div className="flex items-center gap-3 bg-slate-50 px-4 py-2 rounded-2xl border border-slate-200 w-full sm:w-auto justify-between sm:justify-start">
                  <div className="text-right">
                    <div className="text-[10px] text-slate-400 font-bold uppercase">Deteksi Threat</div>
                    <div className="text-lg sm:text-xl font-black text-rose-600">
                      {result.maliciousCount} <span className="text-xs text-slate-400 font-normal">/ {result.enginesAnalyzed || 90} Engine</span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })()}

          {/* Engine Breakdown Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
            <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <div className="text-[10px] text-slate-400 font-bold uppercase">Malicious / Phishing</div>
                <div className="text-xl sm:text-2xl font-black text-rose-600">{result.maliciousCount}</div>
              </div>
              <XCircle className="w-7 h-7 sm:w-8 sm:h-8 text-rose-400/40" />
            </div>

            <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <div className="text-[10px] text-slate-400 font-bold uppercase">Suspicious / Risky</div>
                <div className="text-xl sm:text-2xl font-black text-amber-600">{result.suspiciousCount}</div>
              </div>
              <AlertOctagon className="w-7 h-7 sm:w-8 sm:h-8 text-amber-400/40" />
            </div>

            <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <div className="text-[10px] text-slate-400 font-bold uppercase">Harmless / Clean</div>
                <div className="text-xl sm:text-2xl font-black text-emerald-600">{result.harmlessCount}</div>
              </div>
              <CheckCircle2 className="w-7 h-7 sm:w-8 sm:h-8 text-emerald-400/40" />
            </div>
          </div>

          {/* Summary */}
          <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 sm:space-y-2">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Ringkasan Hasil Pemindaian</h4>
            <p className="text-xs text-slate-700 leading-relaxed">{result.summary}</p>
          </div>

          {/* IP Resolution & Category */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 sm:gap-4">
            {result.ipAddress && (
              <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center gap-3">
                <Network className="w-5 h-5 text-slate-700 shrink-0" />
                <div>
                  <div className="text-[10px] text-slate-400 font-bold uppercase">Resolved IP Address:</div>
                  <div className="text-xs font-mono text-slate-900 font-bold">{result.ipAddress}</div>
                </div>
              </div>
            )}

            {result.categories && result.categories.length > 0 && (
              <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center gap-3">
                <Tag className="w-5 h-5 text-slate-700 shrink-0" />
                <div>
                  <div className="text-[10px] text-slate-400 font-bold uppercase mb-1">Kategori Domain:</div>
                  <div className="flex flex-wrap gap-1.5">
                    {result.categories.map((cat, i) => (
                      <span key={i} className="px-2.5 py-0.5 rounded-full text-[10px] bg-white text-slate-700 font-semibold border border-slate-200">
                        {cat}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>

        </div>
      )}

    </div>
  );
};
