import { Injectable, Logger } from '@nestjs/common';
import * as fs from 'fs';
import * as path from 'path';

export interface HistoryItem {
  id: string;
  type: 'hoax' | 'scam';
  title: string;
  score: number; // trustScore for hoax, dangerScore for scam
  status: string; // 'safe' | 'warning' | 'dangerous' | 'neutral'
  timestamp: string;
  resultDetails: any;
}

@Injectable()
export class HistoryService {
  private readonly logger = new Logger(HistoryService.name);
  private readonly filePath = path.join(process.cwd(), 'history_cache.json');
  private historyList: HistoryItem[] = [];

  constructor() {
    this.loadHistoryFromFile();
  }

  private loadHistoryFromFile() {
    try {
      if (fs.existsSync(this.filePath)) {
        const fileContent = fs.readFileSync(this.filePath, 'utf-8');
        this.historyList = JSON.parse(fileContent);
        this.logger.log(
          `Loaded ${this.historyList.length} history items from cache file.`,
        );
      } else {
        // Initialize with default attractive demo history items
        this.historyList = this.getDemoHistory();
        this.saveHistoryToFile();
      }
    } catch (err) {
      this.logger.error(`Failed to load history cache: ${err.message}`);
      this.historyList = this.getDemoHistory();
    }
  }

  private saveHistoryToFile() {
    try {
      fs.writeFileSync(
        this.filePath,
        JSON.stringify(this.historyList, null, 2),
        'utf-8',
      );
    } catch (err) {
      this.logger.error(`Failed to save history cache: ${err.message}`);
    }
  }

  async addHistory(
    type: 'hoax' | 'scam',
    title: string,
    score: number,
    status: string,
    resultDetails: any,
  ): Promise<HistoryItem> {
    const newItem: HistoryItem = {
      id: Math.random().toString(36).substr(2, 9),
      type,
      title,
      score,
      status,
      timestamp: new Date().toISOString(),
      resultDetails,
    };

    // Prepend to list (most recent first)
    this.historyList.unshift(newItem);

    // Limit to 100 items to prevent bloat
    if (this.historyList.length > 100) {
      this.historyList.pop();
    }

    this.saveHistoryToFile();
    this.logger.log(
      `Added new history item for ${type}: "${title.substring(0, 30)}..."`,
    );

    // Sync to Supabase in the background if keys are present
    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_KEY;
    if (supabaseUrl && supabaseKey) {
      this.logger.log('Supabase detected! Syncing history to Supabase...');
      // Simple REST client upload to Supabase to keep codebase dependency-free
      axios
        .post(`${supabaseUrl}/rest/v1/checker_history`, newItem, {
          headers: {
            apikey: supabaseKey,
            Authorization: `Bearer ${supabaseKey}`,
            'Content-Type': 'application/json',
            Prefer: 'return=representation',
          },
        })
        .catch((err) =>
          this.logger.error(`Supabase sync error: ${err.message}`),
        );
    }

    return newItem;
  }

  async getHistory(): Promise<HistoryItem[]> {
    return this.historyList;
  }

  async clearHistory(): Promise<boolean> {
    this.historyList = [];
    this.saveHistoryToFile();
    return true;
  }

  private getDemoHistory(): HistoryItem[] {
    return [
      {
        id: 'demo-1',
        type: 'hoax',
        title: 'Pemenang Undian Shopee 150 Juta di WA',
        score: 8,
        status: 'unsafe',
        timestamp: new Date(Date.now() - 3600000 * 2).toISOString(), // 2 hours ago
        resultDetails: {
          verdictSummary: '🚨 Terkonfirmasi HOAX Penipuan Finansial (Scam)',
          explanation:
            'Tautan undian yang dikirim via WhatsApp bukan situs resmi Shopee melainkan modus phising.',
          aiInsights: {
            engineUsed: 'Gemini 2.5 Flash',
            contextNarrative:
              'Penipuan phishing Shopee menggunakan bit.ly sangat merugikan masyarakat.',
            credibilityAnalysis: 'Situs bit.ly tidak memiliki legitimasi.',
            recommendations: 'Abaikan pesan. Jangan klik tautan.',
          },
        },
      },
      {
        id: 'demo-2',
        type: 'scam',
        title: 'bit.ly/shopee-hadiah-2026',
        score: 88,
        status: 'dangerous',
        timestamp: new Date(Date.now() - 3600000 * 2.1).toISOString(),
        resultDetails: {
          dangerScore: 88,
          threatLevel: 'dangerous',
          maliciousCount: 14,
          totalEngines: 72,
          ipAddress: '194.26.137.45',
          hostCountry: 'Ukraine',
          safetyAdvice: '🚨 Bahaya! Domain phishing terdeteksi aktif.',
        },
      },
      {
        id: 'demo-3',
        type: 'hoax',
        title: 'Gempa Megathrust Hancurkan Jakarta Besok',
        score: 35,
        status: 'warning',
        timestamp: new Date(Date.now() - 3600000 * 24).toISOString(), // 1 day ago
        resultDetails: {
          verdictSummary: '⚠️ Informasi Menyesatkan (Misleading Content)',
          explanation:
            'Potensi gempa nyata, namun ramalan hari dan waktu terjadinya adalah hoax.',
          aiInsights: {
            engineUsed: 'DeepSeek V3',
            contextNarrative:
              'Kecemasan megathrust dieksploitasi untuk kepanikan publik.',
            credibilityAnalysis: 'Bantahan resmi telah dikeluarkan oleh BMKG.',
            recommendations: 'Ikuti info resmi dari BMKG saja.',
          },
        },
      },
      {
        id: 'demo-4',
        type: 'scam',
        title: 'undangan_pernikahan.apk',
        score: 95,
        status: 'dangerous',
        timestamp: new Date(Date.now() - 3600000 * 48).toISOString(), // 2 days ago
        resultDetails: {
          dangerScore: 95,
          threatLevel: 'dangerous',
          maliciousCount: 18,
          totalEngines: 68,
          safetyAdvice:
            '🚨 Malware Keras! APK ini dirancang untuk mencuri SMS OTP perbankan.',
        },
      },
    ];
  }
}

// Inline placeholder for axios to prevent compilation error if axios isn't global
const axios = require('axios');
