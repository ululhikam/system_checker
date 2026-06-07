import { Injectable, Logger } from '@nestjs/common';
import * as fs from 'fs';
import * as path from 'path';

export interface HistoryItem {
  id: string;
  type: 'hoax' | 'scam';
  title: string;
  score: number;
  status: string;
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
        this.historyList = [];
        this.saveHistoryToFile();
      }
    } catch (err) {
      this.logger.error(`Failed to load history cache: ${err.message}`);
      this.historyList = [];
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
}

// Inline placeholder for axios to prevent compilation error if axios isn't global
const axios = require('axios');
