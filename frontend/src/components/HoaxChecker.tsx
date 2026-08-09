import React, { useState } from 'react';
import { Cpu, Search, Image as ImageIcon, CheckCircle2, AlertTriangle, XCircle, Sparkles, ExternalLink, RefreshCw, FileText, Scale } from 'lucide-react';
import { checkHoax } from '../services/api';
import { FactCheckResult } from '../types';

export const HoaxChecker: React.FC = () => {
  const [textInput, setTextInput] = useState('');
  const [engine, setEngine] = useState<'gemini' | 'deepseek'>('gemini');
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<FactCheckResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        setError('Ukuran gambar terlalu besar (maksimal 10MB)');
        return;
      }
      const reader = new FileReader();
      reader.onloadend = () => {
        setSelectedImage(reader.result as string);
        setError(null);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!textInput.trim() && !selectedImage) {
      setError('Masukkan teks berita atau unggah tangkapan layar untuk diperiksa.');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await checkHoax({
        text: textInput.trim() || undefined,
        imageUrl: selectedImage || undefined,
        engine,
      });
      setResult(data);
    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.message ||
        'Gagal terhubung ke backend BFF NestJS. Pastikan server NestJS aktif di http://localhost:3000'
      );
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string, score: number) => {
    if (status === 'safe' || score >= 75) {
      return {
        bg: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        icon: CheckCircle2,
        label: 'VERIFIKASI VALID / TEPERCAYA',
      };
    }
    if (status === 'neutral' || (score >= 45 && score < 75)) {
      return {
        bg: 'bg-amber-50 text-amber-700 border-amber-200',
        icon: AlertTriangle,
        label: 'MERAGUKAN / PERLU VERIFIKASI SEIMBANG',
      };
    }
    return {
      bg: 'bg-rose-50 text-rose-700 border-rose-200',
      icon: XCircle,
      label: 'INDIKASI HOAKS / DISINFORMASI TINGGI',
    };
  };

  return (
    <div className="space-y-4 sm:space-y-6">
      
      {/* Input Form Card */}
      <div className="card-clean p-4 sm:p-7 space-y-4 sm:space-y-6">
        
        {/* Card Header & Engine Selector */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 sm:gap-4 pb-3 sm:pb-4 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-slate-900 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
              <Cpu className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-extrabold text-slate-900">AI Hoax Verification</h3>
              <p className="text-[11px] sm:text-xs text-slate-500 font-medium">Analisis narasi hoaks, OCR gambar, & Google Fact Check</p>
            </div>
          </div>

          {/* Engine Selector Pills */}
          <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-full border border-slate-200 text-[11px] sm:text-xs font-semibold w-full sm:w-auto justify-center">
            <button
              type="button"
              onClick={() => setEngine('gemini')}
              className={`flex-1 sm:flex-none px-3.5 sm:px-4 py-1.5 rounded-full transition cursor-pointer text-center ${
                engine === 'gemini'
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Google Gemini
            </button>
            <button
              type="button"
              onClick={() => setEngine('deepseek')}
              className={`flex-1 sm:flex-none px-3.5 sm:px-4 py-1.5 rounded-full transition cursor-pointer text-center ${
                engine === 'deepseek'
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              DeepSeek AI
            </button>
          </div>
        </div>

        <form onSubmit={handleScan} className="space-y-4">
          <div>
            <textarea
              value={textInput}
              onChange={(e) => setTextInput(e.target.value)}
              placeholder="Tempelkan klaim, potongan berita, teks pesan viral WhatsApp, atau judul artikel di sini..."
              rows={4}
              className="w-full px-3.5 sm:px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 placeholder-slate-400 text-xs focus:outline-none focus:border-slate-400 focus:ring-1 focus:ring-slate-400 transition resize-y"
            />
          </div>

          {/* Action Row */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-3 sm:p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80">
            <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
              <label className="flex items-center justify-center gap-2 px-3.5 sm:px-4 py-2 bg-white hover:bg-slate-100 text-slate-700 rounded-full text-xs font-semibold cursor-pointer border border-slate-200 shadow-xs transition w-full sm:w-auto">
                <ImageIcon className="w-4 h-4 text-slate-700" />
                <span>Unggah Tangkapan Layar (OCR)</span>
                <input type="file" accept="image/*" onChange={handleImageChange} className="hidden" />
              </label>

              {selectedImage && (
                <div className="flex items-center justify-between gap-2 text-xs text-slate-800 bg-white px-3 py-1 rounded-full border border-slate-200 shadow-xs w-full sm:w-auto">
                  <div className="flex items-center gap-1.5 truncate">
                    <FileText className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                    <span className="font-semibold truncate">Gambar Terpilih</span>
                  </div>
                  <button type="button" onClick={() => setSelectedImage(null)} className="text-slate-400 hover:text-rose-600 shrink-0">×</button>
                </div>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-2.5 rounded-full bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-md disabled:opacity-50 transition cursor-pointer"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin text-white" />
                  <span>Menganalisis Berita...</span>
                </>
              ) : (
                <>
                  <Search className="w-4 h-4 text-white" />
                  <span>Jalankan Analisis AI</span>
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

      {/* Result Section Card */}
      {result && (
        <div className="card-clean p-4 sm:p-7 space-y-4 sm:space-y-6 animate-fadeIn">
          
          {/* Header Status & Trust Score */}
          {(() => {
            const badge = getStatusBadge(result.status, result.trustScore);
            const BadgeIcon = badge.icon;
            return (
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 sm:pb-6 border-b border-slate-100">
                <div className="flex items-center gap-3">
                  <div className={`p-2.5 sm:p-3 rounded-2xl border ${badge.bg} shrink-0`}>
                    <BadgeIcon className="w-5 h-5 sm:w-6 sm:h-6" />
                  </div>
                  <div>
                    <span className={`inline-block px-2.5 sm:px-3 py-0.5 rounded-full text-[10px] font-extrabold uppercase border mb-1 ${badge.bg}`}>
                      {badge.label}
                    </span>
                    <h3 className="text-lg sm:text-xl font-extrabold text-slate-900 tracking-tight leading-snug">{result.verdictSummary}</h3>
                  </div>
                </div>

                <div className="flex items-center gap-3 bg-slate-50 px-4 py-2 rounded-2xl border border-slate-200 w-full sm:w-auto justify-between sm:justify-start">
                  <div className="text-right sm:text-right">
                    <div className="text-[10px] text-slate-400 font-bold uppercase">Skor Kepercayaan</div>
                    <div className="text-xl sm:text-2xl font-black text-slate-900">
                      {result.trustScore}<span className="text-xs text-slate-400 font-normal">/100</span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })()}

          {/* OCR Extracted Text */}
          {result.ocrExtractedText && (
            <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
                <FileText className="w-4 h-4 text-blue-600 shrink-0" />
                <span>Hasil Ekstraksi OCR Teks Gambar:</span>
              </div>
              <p className="text-xs text-slate-700 font-mono italic leading-relaxed bg-white p-3 rounded-xl border border-slate-200">
                "{result.ocrExtractedText}"
              </p>
            </div>
          )}

          {/* Explanation & Corrected Fact */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-amber-500 shrink-0" />
                Penjelasan & Analisis Konteks
              </h4>
              <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">{result.explanation}</p>
            </div>

            <div className="p-3.5 sm:p-4 rounded-2xl bg-emerald-50/60 border border-emerald-200 space-y-2">
              <h4 className="text-xs font-bold text-emerald-800 uppercase tracking-wider flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                Fakta Sebenarnya (Koreksi)
              </h4>
              <p className="text-xs text-emerald-900 leading-relaxed whitespace-pre-line">{result.correctedFact}</p>
            </div>
          </div>

          {/* Fallacies Detected */}
          {result.fallaciesDetected && result.fallaciesDetected.length > 0 && (
            <div className="p-3.5 sm:p-4 rounded-2xl bg-amber-50/60 border border-amber-200 space-y-2">
              <h4 className="text-xs font-bold text-amber-800 uppercase tracking-wider flex items-center gap-1.5">
                <Scale className="w-4 h-4 text-amber-600 shrink-0" />
                Falasi Logika & Bias Narasi Terdeteksi
              </h4>
              <div className="flex flex-wrap gap-1.5 sm:gap-2">
                {result.fallaciesDetected.map((fal, i) => (
                  <span key={i} className="px-2.5 sm:px-3 py-0.5 sm:py-1 rounded-full text-xs bg-amber-100 text-amber-900 font-semibold border border-amber-200">
                    ⚠️ {fal}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Google Fact Checks */}
          {result.googleFactChecks && result.googleFactChecks.length > 0 && (
            <div className="space-y-3 pt-2">
              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                <Search className="w-4 h-4 text-blue-600 shrink-0" />
                Verifikasi Silang Google Fact Check Tools ({result.googleFactChecks.length})
              </h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {result.googleFactChecks.map((fc, index) => (
                  <div key={index} className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-semibold text-slate-900 line-clamp-2">"{fc.claim}"</span>
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200 whitespace-nowrap shrink-0">
                        {fc.verdict}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-[11px] text-slate-500 pt-2 border-t border-slate-200">
                      <span className="truncate pr-2">Oleh: <strong className="text-slate-800">{fc.publisher || fc.claimant || 'Penerbit Independen'}</strong></span>
                      {fc.url && (
                        <a
                          href={fc.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center gap-1 text-slate-900 font-bold hover:underline shrink-0"
                        >
                          <span>Sumber</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>
      )}

    </div>
  );
};
