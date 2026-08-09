export interface FactCheckRequest {
  text?: string;
  imageUrl?: string;
  engine?: 'gemini' | 'deepseek';
}

export interface GoogleFactCheck {
  claim: string;
  claimant: string;
  verdict: string;
  reviewDate: string;
  publisher: string;
  url: string;
}

export interface AiInsights {
  engineUsed: string;
  contextNarrative: string;
  credibilityAnalysis: string;
  recommendations: string;
}

export interface FactCheckResult {
  trustScore: number;
  status: 'safe' | 'neutral' | 'unsafe';
  query: string;
  ocrExtractedText?: string;
  verdictSummary: string;
  explanation: string;
  correctedFact: string;
  fallaciesDetected: string[];
  googleFactChecks: GoogleFactCheck[];
  aiInsights: AiInsights;
  timestamp: string;
}

export interface UrlScanRequest {
  url: string;
  enableWebScraping?: boolean;
}

export interface VtEngineStats {
  malicious?: number;
  suspicious?: number;
  harmless?: number;
  undetected?: number;
}

export interface ScamScanResult {
  target: string;
  scanType: 'url' | 'file';
  status: 'safe' | 'suspicious' | 'malicious';
  threatScore: number;
  summary: string;
  enginesAnalyzed: number;
  maliciousCount: number;
  suspiciousCount: number;
  harmlessCount: number;
  categories: string[];
  ipAddress?: string;
  details?: Record<string, any>;
  timestamp: string;
}

export interface HistoryItem {
  id: string;
  type: 'hoax' | 'scam';
  title: string;
  score: number;
  status: 'safe' | 'neutral' | 'unsafe' | 'suspicious' | 'malicious';
  timestamp: string;
  resultDetails: FactCheckResult | ScamScanResult;
}
