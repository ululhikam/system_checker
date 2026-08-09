import axios from 'axios';
import { FactCheckRequest, FactCheckResult, UrlScanRequest, ScamScanResult, HistoryItem } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const LOCAL_STORAGE_KEY = 'cyber_guard_logs';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 45000, // 45 seconds for AI/VT scans
});

// Save scan result to browser LocalStorage
export const saveLocalHistory = (item: Omit<HistoryItem, 'id' | 'timestamp'>) => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
    const historyList: HistoryItem[] = raw ? JSON.parse(raw) : [];
    
    const newItem: HistoryItem = {
      ...item,
      id: Date.now().toString() + Math.random().toString(36).substring(2, 6),
      timestamp: new Date().toLocaleString('id-ID', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }),
    };

    historyList.unshift(newItem);
    // Keep max 50 recent items
    const trimmed = historyList.slice(0, 50);
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(trimmed));
  } catch (e) {
    console.error('Failed to save log to localStorage:', e);
  }
};

export const checkHoax = async (payload: FactCheckRequest): Promise<FactCheckResult> => {
  const response = await api.post<FactCheckResult>('/check-hoax', payload);
  const result = response.data;
  
  saveLocalHistory({
    type: 'hoax',
    title: payload.text || result.query || 'AI Hoax Verification',
    score: result.trustScore,
    status: result.status,
    resultDetails: result,
  });

  return result;
};

export const scanUrl = async (payload: UrlScanRequest): Promise<ScamScanResult> => {
  const response = await api.post<ScamScanResult>('/scan-url', payload);
  const result = response.data;

  saveLocalHistory({
    type: 'scam',
    title: result.target || payload.url,
    score: result.threatScore,
    status: result.status,
    resultDetails: result,
  });

  return result;
};

export const scanFile = async (file: File): Promise<ScamScanResult> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('fileName', file.name);
  formData.append('fileSize', file.size.toString());

  const response = await api.post<ScamScanResult>('/scan-file', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  const result = response.data;

  saveLocalHistory({
    type: 'scam',
    title: `File: ${file.name}`,
    score: result.threatScore,
    status: result.status,
    resultDetails: result,
  });

  return result;
};

export const fetchHistory = async (): Promise<HistoryItem[]> => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error('Failed to load history from localStorage:', e);
    return [];
  }
};

export const clearHistoryApi = async (): Promise<boolean> => {
  try {
    localStorage.removeItem(LOCAL_STORAGE_KEY);
    return true;
  } catch (e) {
    console.error('Failed to clear localStorage history:', e);
    return false;
  }
};
