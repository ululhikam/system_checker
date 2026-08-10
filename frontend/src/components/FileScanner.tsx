import React, { useState } from 'react';
import { FileCode2, UploadCloud, RefreshCw, XCircle, FileCheck, ShieldAlert, CheckCircle2, AlertTriangle } from 'lucide-react';
import { scanFile } from '../services/api';
import { ScamScanResult } from '../types';

export const FileScanner: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ScamScanResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFileChange = (file: File | null) => {
    if (file) {
      if (file.size > 32 * 1024 * 1024) {
        setError('Ukuran berkas melebihi batas maksimal (32MB).');
        return;
      }
      setSelectedFile(file);
      setError(null);
      setResult(null);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileChange(e.dataTransfer.files[0]);
    }
  };

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setError('Pilih berkas (APK, Executable, PDF, Doc, dll) untuk dipindai.');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await scanFile(selectedFile);
      setResult(data);
    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.message ||
        'Gagal memindai berkas. Pastikan backend BFF NestJS dapat menerima pengunggahan berkas.'
      );
    } finally {
      setLoading(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="space-y-4 sm:space-y-6">
      
      {/* File Upload Card */}
      <div className="card-clean p-4 sm:p-7 space-y-4 sm:space-y-6">
        
        <div className="flex items-center gap-3 pb-3 sm:pb-4 border-b border-slate-100">
          <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-slate-900 text-white flex items-center justify-center font-bold shadow-sm shrink-0">
            <FileCode2 className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-extrabold text-slate-900">Malware & APK Virus Scanner</h3>
            <p className="text-[11px] sm:text-xs text-slate-500 font-medium">Unggah APK, PDF, Executable untuk deteksi virus & payload</p>
          </div>
        </div>

        <form onSubmit={handleScan} className="space-y-4">
          
          <div
            onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            className={`relative flex flex-col items-center justify-center p-6 sm:p-8 rounded-2xl border-2 border-dashed transition duration-200 cursor-pointer ${
              isDragging
                ? 'border-slate-900 bg-slate-100'
                : selectedFile
                ? 'border-slate-400 bg-slate-50'
                : 'border-slate-200 hover:border-slate-300 bg-slate-50'
            }`}
          >
            <input
              type="file"
              onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
              className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
            />

            {selectedFile ? (
              <div className="flex flex-col items-center text-center space-y-2 max-w-full">
                <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-full bg-slate-900 text-white flex items-center justify-center shadow-sm shrink-0">
                  <FileCheck className="w-5 h-5 sm:w-6 sm:h-6" />
                </div>
                <div className="max-w-full">
                  <h4 className="text-xs sm:text-sm font-bold text-slate-900 truncate px-2">{selectedFile.name}</h4>
                  <p className="text-[11px] text-slate-500 font-mono">{formatFileSize(selectedFile.size)} • {selectedFile.type || 'Binary File'}</p>
                </div>
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); setSelectedFile(null); }}
                  className="text-xs text-rose-600 font-bold hover:underline pt-1"
                >
                  Ganti Berkas
                </button>
              </div>
            ) : (
              <div className="flex flex-col items-center text-center space-y-2.5 sm:space-y-3">
                <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center shrink-0">
                  <UploadCloud className="w-5 h-5 sm:w-6 sm:h-6" />
                </div>
                <div>
                  <h4 className="text-xs sm:text-sm font-bold text-slate-900">Tarik & Lepas Berkas di Sini</h4>
                  <p className="text-[11px] sm:text-xs text-slate-500 mt-0.5">atau klik untuk menjelajah dari perangkat Anda</p>
                </div>
                <span className="text-[10px] sm:text-[11px] font-semibold text-slate-600 bg-white px-3 py-1 rounded-full border border-slate-200 shadow-xs">
                  APK, EXE, PDF, ZIP, DOCX (Maksimal 32MB)
                </span>
              </div>
            )}
          </div>

          <div className="flex items-center justify-end">
            <button
              type="submit"
              disabled={loading || !selectedFile}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-2.5 rounded-full bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-md disabled:opacity-50 transition cursor-pointer"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin text-white" />
                  <span>Memindai Malware Berkas...</span>
                </>
              ) : (
                <>
                  <FileCode2 className="w-4 h-4 text-white" />
                  <span>Analisis Berkas Ini</span>
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
            const level = result.threatLevel || result.status || 'safe';
            const score = result.dangerScore ?? result.threatScore ?? 0;
            const isSafe = level === 'safe';
            const isWarning = level === 'warning' || level === 'suspicious';

            return (
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 sm:pb-6 border-b border-slate-100">
                <div className="flex items-center gap-3">
                  <div className={`p-2.5 sm:p-3 rounded-2xl border ${
                    isSafe
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : isWarning
                      ? 'bg-amber-50 text-amber-700 border-amber-200'
                      : 'bg-rose-50 text-rose-700 border-rose-200'
                  } shrink-0`}>
                    {isSafe ? (
                      <CheckCircle2 className="w-5 h-5 sm:w-6 sm:h-6" />
                    ) : isWarning ? (
                      <AlertTriangle className="w-5 h-5 sm:w-6 sm:h-6" />
                    ) : (
                      <ShieldAlert className="w-5 h-5 sm:w-6 sm:h-6" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <span className={`inline-block px-2.5 sm:px-3 py-0.5 rounded-full text-[10px] font-extrabold uppercase border mb-1 ${
                      isSafe
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : isWarning
                        ? 'bg-amber-50 text-amber-700 border-amber-200'
                        : 'bg-rose-50 text-rose-700 border-rose-200'
                    }`}>
                      BERKAS {isSafe ? 'BERSIH' : isWarning ? 'PERLU WASPADA' : 'MALWARE TERDETEKSI'}
                    </span>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900 font-mono truncate">{result.target}</h3>
                  </div>
                </div>

                <div className="flex items-center gap-3 bg-slate-50 px-4 py-2 rounded-2xl border border-slate-200 w-full sm:w-auto justify-between sm:justify-start">
                  <div className="text-right">
                    <div className="text-[10px] text-slate-400 font-bold uppercase">Danger Score</div>
                    <div className="text-lg sm:text-xl font-black text-rose-600">{score} / 100</div>
                  </div>
                </div>
              </div>
            );
          })()}

          <div className="p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 sm:space-y-2">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Hasil & Analisis Keamanan Berkas</h4>
            <p className="text-xs text-slate-700 leading-relaxed">{result.safetyAdvice || result.summary}</p>
          </div>

        </div>
      )}

    </div>
  );
};
