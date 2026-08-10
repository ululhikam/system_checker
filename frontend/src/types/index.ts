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

export interface DetectionItem {
  engine: string;
  category: string;
  result: 'clean' | 'phishing' | 'malware' | 'suspicious' | 'unrated';
}

export interface ScamScanResult {
  target: string;
  type?: 'url' | 'file';
  dangerScore: number;
  threatLevel: 'safe' | 'warning' | 'dangerous';
  totalEngines: number;
  flaggedEngineCount: number;
  cleanCount: number;
  ipAddress?: string | null;
  hostCountry?: string | null;
  reputationPoints?: number;
  detections?: DetectionItem[];
  safetyAdvice: string;
  timestamp: string;

  // Backward compatibility fields for legacy UI components
  scanType?: 'url' | 'file';
  status?: 'safe' | 'warning' | 'dangerous' | 'suspicious' | 'malicious';
  threatScore?: number;
  summary?: string;
  enginesAnalyzed?: number;
  maliciousCount?: number;
  suspiciousCount?: number;
  harmlessCount?: number;
  categories?: string[];
  details?: Record<string, any>;
}

export interface HistoryItem {
  id: string;
  type: 'hoax' | 'scam';
  title: string;
  score: number;
  status: 'safe' | 'neutral' | 'unsafe' | 'warning' | 'dangerous' | 'suspicious' | 'malicious';
  timestamp: string;
  resultDetails: FactCheckResult | ScamScanResult;
}

