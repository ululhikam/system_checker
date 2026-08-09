import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import axios, { AxiosResponse } from 'axios';
import { getGeminiApiKeys, callGeminiApiWithFallback } from '../common/gemini-rotator';


export class FactCheckRequest {
  text?: string;
  imageUrl?: string;
  engine: 'gemini' | 'deepseek';
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
  googleFactChecks: Array<{
    claim: string;
    claimant: string;
    verdict: string;
    reviewDate: string;
    publisher: string;
    url: string;
  }>;
  aiInsights: {
    engineUsed: string;
    contextNarrative: string;
    credibilityAnalysis: string;
    recommendations: string;
  };
  timestamp: string;
}

interface GoogleFactCheckClaim {
  text: string;
  claimant?: string;
  claimReview?: Array<{
    textualRating?: string;
    reviewDate?: string;
    publisher?: {
      name?: string;
    };
    url?: string;
  }>;
}

interface GoogleFactCheckResponse {
  claims?: GoogleFactCheckClaim[];
}

interface GeminiContentPart {
  text?: string;
}

interface GeminiCandidate {
  content?: {
    parts?: GeminiContentPart[];
  };
  groundingMetadata?: {
    groundingChunks?: Array<{
      web?: {
        uri?: string;
        title?: string;
      };
    }>;
  };
}

interface GeminiResponse {
  candidates?: GeminiCandidate[];
}

interface DeepSeekChoice {
  message?: {
    content?: string;
  };
}

interface DeepSeekResponse {
  choices?: DeepSeekChoice[];
}

interface LlmParsedResponse {
  status: 'FAKTUAL' | 'HOAKS' | 'DISINFORMASI' | 'SATIRE' | 'UNVERIFIED';
  confidence_score: number;
  reasoning: string;
  correctedFact?: string;
  source_links: string[];
}

function cleanQuery(query: string): string {
  let cleaned = query
    .replace(/[.,/#!$%^&*;:{}=\-_`~()?"'“”]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  if (cleaned.length > 120) {
    const truncated = cleaned.substring(0, 120);
    const lastSpace = truncated.lastIndexOf(' ');
    if (lastSpace > 50) {
      cleaned = truncated.substring(0, lastSpace);
    } else {
      cleaned = truncated;
    }
  }
  return cleaned;
}

function parseCleanJson(rawText: string): unknown {
  let cleaned = rawText.trim();
  if (cleaned.startsWith('```')) {
    cleaned = cleaned
      .replace(/^```[a-zA-Z]*\n/, '')
      .replace(/\n```$/, '')
      .trim();
  }
  try {
    return JSON.parse(cleaned);
  } catch (e: unknown) {
    const errMessage = e instanceof Error ? e.message : String(e);
    const match = cleaned.match(/\{[\s\S]*\}/);
    if (match) {
      try {
        return JSON.parse(match[0]);
      } catch {
        throw new Error(
          `JSON parsing failed: ${errMessage}. Cleaned text: ${cleaned}`,
        );
      }
    }
    throw e;
  }
}

@Injectable()
export class HoaxService {
  private readonly logger = new Logger(HoaxService.name);

  async checkHoax(payload: FactCheckRequest): Promise<FactCheckResult> {
    let queryText = payload.text || '';

    if (!queryText.trim()) {
      queryText = 'Klaim kosong';
    }

    this.logger.log(
      `Analyzing claim: "${queryText}" with engine ${payload.engine}`,
    );

    // Validate API Keys
    const googleApiKey = process.env.GOOGLE_FACT_CHECK_API_KEY;
    const geminiKeys = getGeminiApiKeys();
    const deepseekKey = process.env.DEEPSEEK_API_KEY;

    const isGoogleKeyMissing =
      !googleApiKey ||
      googleApiKey === 'your_google_fact_check_api' ||
      googleApiKey.trim() === '';
    const isGeminiKeysMissing = geminiKeys.length === 0;
    const isDeepseekKeyMissing =
      !deepseekKey ||
      deepseekKey === 'your_deepseek_api_key' ||
      deepseekKey.trim() === '';

    if (isGoogleKeyMissing) {
      throw new BadRequestException(
        'API_KEY_NOT_CONFIGURED: Google Fact Check API Key belum dikoneksikan di file .env backend!',
      );
    }

    if (payload.engine === 'gemini' && isGeminiKeysMissing) {
      throw new BadRequestException(
        'API_KEY_NOT_CONFIGURED: Gemini API Key belum dikoneksikan di file .env backend! Tambahkan GEMINI_API_KEY atau GEMINI_API_KEYS di .env.',
      );
    }

    if (payload.engine === 'deepseek' && isDeepseekKeyMissing) {
      throw new BadRequestException(
        'API_KEY_NOT_CONFIGURED: DeepSeek API Key belum dikoneksikan di file .env backend!',
      );
    }

    // Try Google Fact Check API
    let googleFactChecks: Array<{
      claim: string;
      claimant: string;
      verdict: string;
      reviewDate: string;
      publisher: string;
      url: string;
    }> = [];

    const fetchFactChecks = async (queryStr: string) => {
      try {
        const response = (await axios.get(
          `https://factchecktools.googleapis.com/v1alpha1/claims:search`,
          {
            params: {
              query: queryStr,
              key: googleApiKey,
              languageCode: 'id',
            },
          },
        )) as unknown as { data: GoogleFactCheckResponse };
        return response.data?.claims || [];
      } catch (error) {
        this.logger.warn(
          `Google Fact Check API query "${queryStr}" failed: ${error instanceof Error ? error.message : error}`,
        );
        return [];
      }
    };

    try {
      // 1. Try original query
      let claims = await fetchFactChecks(queryText);

      // 2. If no claims, try simplified cleaned query to improve matching rate
      const cleaned = cleanQuery(queryText);
      if (claims.length === 0 && cleaned !== queryText && cleaned.length > 5) {
        claims = await fetchFactChecks(cleaned);
      }

      if (claims && claims.length > 0) {
        googleFactChecks = claims.map((c: GoogleFactCheckClaim) => ({
          claim: c.text,
          claimant: c.claimant || 'Tidak diketahui',
          verdict: c.claimReview?.[0]?.textualRating || 'Belum terverifikasi',
          reviewDate: c.claimReview?.[0]?.reviewDate || '',
          publisher: c.claimReview?.[0]?.publisher?.name || 'Fact Checker',
          url: c.claimReview?.[0]?.url || '',
        }));
      }
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : 'Unknown error occurred';
      this.logger.warn(
        `Google Fact Check API failed (graceful fallback): ${message}`,
      );
      googleFactChecks = [];
    }

    let trustScore = 50;
    let status: 'safe' | 'neutral' | 'unsafe' = 'neutral';
    let verdictSummary = '';
    let explanation = '';
    let correctedFact = '';
    let fallaciesDetected: string[] = [];
    let contextNarrative = '';
    let credibilityAnalysis = '';
    let recommendations = '';

    try {
      let parsed: LlmParsedResponse;

      if (payload.engine === 'gemini' && geminiKeys.length > 0) {
        // Step 1: Web grounding search with Gemini (dengan Fallback / Key Rotation)
        const step1Prompt = `Lakukan analisis cek fakta mendalam dan pencarian web terbaru untuk memverifikasi klaim berikut.

Klaim: "${queryText}"
Data Referensi Awal (jika ada): ${JSON.stringify(googleFactChecks)}

Tugas Anda:
1. Cari informasi valid mengenai klaim ini di internet menggunakan pencarian Google.
2. Tentukan klasifikasi status dari klaim ini. Pilih salah satu dari klasifikasi berikut secara tepat:
   - FAKTUAL: Jika klaim benar-benar terjadi, didukung bukti kuat, dan bukan rekayasa.
   - HOAKS: Jika klaim sepenuhnya salah, bohong, atau rekayasa.
   - DISINFORMASI: Jika klaim mengandung informasi yang diputarbalikkan, manipulasi konteks, atau setengah benar demi tujuan tertentu.
   - SATIRE: Jika klaim merupakan humor, parodi, atau sindiran politik/sosial yang tidak untuk dipercaya sebagai kebenaran.
   - UNVERIFIED: Jika tidak ditemukan bukti atau informasi yang cukup di internet untuk membuktikan benar atau salahnya klaim.
3. Tentukan confidence score (tingkat keyakinan analisis Anda) dari skala 0 hingga 100.
4. Berikan penjelasan singkat maksimal 3 kalimat mengenai alasan Anda memilih status tersebut dan jelaskan fakta yang sebenarnya.
`;

        let step1Response: AxiosResponse<GeminiResponse> | null = null;

        try {
          step1Response = await callGeminiApiWithFallback<GeminiResponse>(
            (key) =>
              axios.post(
                `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${key}`,
                {
                  contents: [{ parts: [{ text: step1Prompt }] }],
                  tools: [{ google_search: {} }],
                },
              ),
            this.logger,
            'HoaxService Step 1 (Web Search)',
          );
        } catch (err: unknown) {
          const axiosErr = err as { response?: { status?: number } };
          if (axiosErr.response?.status === 429) {
            this.logger.error('Seluruh Gemini API Key mencapai limit 429 pada Step 1');
            return this.createLimitReachedResult(queryText, payload.engine, googleFactChecks);
          }
          throw err;
        }

        const step1Text =
          step1Response?.data?.candidates?.[0]?.content?.parts?.[0]?.text || '';

        // Extract unique grounding links
        const groundedLinks: string[] = [];
        const groundingChunks =
          step1Response?.data?.candidates?.[0]?.groundingMetadata
            ?.groundingChunks;
        if (groundingChunks && Array.isArray(groundingChunks)) {
          for (const chunk of groundingChunks) {
            if (chunk.web?.uri) {
              const uri = chunk.web.uri;
              if (!groundedLinks.includes(uri)) {
                groundedLinks.push(uri);
              }
              // Enrich googleFactChecks
              const isDuplicate = googleFactChecks.some((fc) => fc.url === uri);
              if (!isDuplicate) {
                googleFactChecks.push({
                  claim: queryText,
                  claimant: 'Sumber Publik / Media Digital',
                  verdict: 'Rujukan Web',
                  reviewDate: new Date().toLocaleDateString('id-ID'),
                  publisher: chunk.web.title || 'Sumber Informasi Web',
                  url: uri,
                });
              }
            }
          }
        }

        // Step 2: Format to strict JSON
        const step2Prompt = `Analisis laporan verifikasi klaim berikut dan konversikan menjadi format JSON murni yang terstruktur sesuai dengan skema yang diberikan.

Teks Laporan Verifikasi:
"${step1Text}"

Skema JSON yang Wajib Dipenuhi:
{
  "status": "FAKTUAL" | "HOAKS" | "DISINFORMASI" | "SATIRE" | "UNVERIFIED",
  "confidence_score": number (skala 0-100),
  "reasoning": "penjelasan singkat padat maksimal 3 kalimat",
  "correctedFact": "koreksi fakta sebenarnya (jika hoaks/disinformasi, jelaskan fakta riilnya secara jelas. Jika faktual, jelaskan mengapa ini benar. Maksimal 2 kalimat)"
}

Aturan Tambahan:
- Kembalikan HANYA JSON murni. Jangan tambahkan markdown code block (\`\`\`json), markdown formatting, atau karakter tambahan lainnya di luar format JSON.
`;

        let step2Response: AxiosResponse<GeminiResponse> | null = null;

        try {
          step2Response = await callGeminiApiWithFallback<GeminiResponse>(
            (key) =>
              axios.post(
                `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${key}`,
                {
                  contents: [{ parts: [{ text: step2Prompt }] }],
                  generationConfig: { responseMimeType: 'application/json' },
                },
              ),
            this.logger,
            'HoaxService Step 2 (JSON Structuring)',
          );
        } catch (err: unknown) {
          const axiosErr = err as { response?: { status?: number } };
          if (axiosErr.response?.status === 429) {
            this.logger.error('Seluruh Gemini API Key mencapai limit 429 pada Step 2');
            return this.createLimitReachedResult(queryText, payload.engine, googleFactChecks);
          }
          throw err;
        }

        const rawText =
          step2Response?.data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
        parsed = parseCleanJson(rawText) as LlmParsedResponse;
        parsed.source_links = groundedLinks;
      } else {
        // DeepSeek integration
        const deepseekPrompt = `Analisis klaim berikut berdasarkan KONTEKS BERITA yang diberikan (jika ada). Jika KONTEKS BERITA kosong, analisis berdasarkan pengetahuan internal Anda secara bijak.

Klaim: "${queryText}"
KONTEKS BERITA: ${JSON.stringify(googleFactChecks)}

ATURAN KETAT:
1. Jika KONTEKS BERITA tidak kosong, analisis wajib mengutamakan KONTEKS BERITA tersebut.
2. Jika KONTEKS BERITA kosong atau tidak mencukupi, Anda BOLEH menggunakan pengetahuan internal Anda untuk memverifikasi apakah klaim ini adalah hoaks yang umum beredar, misinformasi, fakta nyata, atau satire.
3. Jika Anda tidak yakin atau tidak memiliki informasi internal tentang klaim ini, isi status dengan "UNVERIFIED".
4. Ekstrak URL sumber pendukung ke dalam field "source_links" jika ada.
5. Klasifikasikan status menjadi salah satu dari: [FAKTUAL, HOAKS, DISINFORMASI, SATIRE, UNVERIFIED].

FORMAT BALASAN (WAJIB JSON MURNI TANPA EMBEL-EMBEL MARKDOWN):
{
  "status": "<Salah satu dari: FAKTUAL, HOAKS, DISINFORMASI, SATIRE, UNVERIFIED>",
  "confidence_score": <angka 0-100>,
  "reasoning": "<Penjelasan singkat, padat, dan langsung pada intinya maksimal 3 kalimat.>",
  "correctedFact": "<Koreksi fakta yang sebenarnya, maksimal 2 kalimat.>",
  "source_links": ["<url1>", "<url2>"]
}
`;

        let retries = 3;
        let delayMs = 1500;
        let llmResponse: any = null;

        while (retries > 0) {
          try {
            llmResponse = await axios.post(
              // pakai 'https://api.deepseek.com/v1/chat/completions' jika ingin pakai API Key yang dibuat dari deepseek (resmi) langsung
              'https://openrouter.ai/api/v1/chat/completions',
                {
                  model: 'deepseek-v4-flash',
                  messages: [{ role: 'user', content: deepseekPrompt }],
                  response_format: { type: 'json_object' },
                },
                {
                  headers: {
                    Authorization: `Bearer ${deepseekKey}`,
                    'Content-Type': 'application/json',
                  },
                  timeout: 20000,
                },
            );
            break;
          } catch (err: unknown) {
            retries--;
            const axiosErr = err as {
              response?: { status?: number; data?: any };
            };
            if (axiosErr.response?.status === 429 && retries > 0) {
              this.logger.warn(
                `DeepSeek API 429 rate limit hit. Retrying in ${delayMs}ms... (Retries left: ${retries})`,
              );
              await new Promise((resolve) => setTimeout(resolve, delayMs));
              delayMs *= 2;
            } else if (axiosErr.response?.status === 429) {
              this.logger.error('DeepSeek API 429 rate limit hit - all retries exhausted');
              return this.createLimitReachedResult(queryText, payload.engine, googleFactChecks);
            } else {
              throw err;
            }
          }
        }

        const rawText = llmResponse?.data?.choices?.[0]?.message?.content || '';
        parsed = parseCleanJson(rawText) as LlmParsedResponse;
      }

      // Map strict JSON fields to local variables returned in FactCheckResult
      if (parsed.status === 'FAKTUAL') {
        trustScore = parsed.confidence_score;
      } else if (parsed.status === 'UNVERIFIED') {
        trustScore = 50;
      } else if (parsed.status === 'SATIRE') {
        trustScore = 40;
      } else {
        // HOAKS or DISINFORMASI
        trustScore = Math.max(0, 100 - parsed.confidence_score);
      }

      if (parsed.status === 'FAKTUAL') {
        status = 'safe';
      } else if (parsed.status === 'UNVERIFIED' || parsed.status === 'SATIRE') {
        status = 'neutral';
      } else {
        status = 'unsafe';
      }

      verdictSummary = `[${parsed.status}] ${parsed.reasoning}`;
      explanation = parsed.reasoning;
      correctedFact =
        parsed.status === 'HOAKS' || parsed.status === 'DISINFORMASI'
          ? `Fakta: ${parsed.correctedFact || parsed.reasoning}`
          : parsed.correctedFact || parsed.reasoning;

      fallaciesDetected = [parsed.status];

      const linksMarkdown =
        parsed.source_links && parsed.source_links.length > 0
          ? parsed.source_links.map((link) => `• 🌐 ${link}`).join('\n')
          : '• Tidak ada tautan referensi dalam konteks berita.';

      contextNarrative = `Analisis dilakukan dengan membandingkan klaim terhadap data Google Fact Check & penelusuran web terbaru.\n\n**SUMBER REFERENSI DIGITAL:**\n${linksMarkdown}`;
      credibilityAnalysis = `Analisis AI menunjukkan tingkat keyakinan ${parsed.confidence_score}% dengan status verifikasi ${parsed.status}.`;
      recommendations =
        parsed.status === 'HOAKS' || parsed.status === 'DISINFORMASI'
          ? 'Jangan menyebarluaskan informasi ini ke platform media sosial atau grup obrolan keluarga Anda.'
          : 'Tetap lakukan verifikasi silang pada berita sensitif menggunakan portal media resmi.';
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Unknown error occurred';
      this.logger.error(`LLM Call failed: ${message}`);
      throw new BadRequestException(
        `LLM API Call failed (${payload.engine}): ${message}`,
      );
    }

    return {
      trustScore,
      status,
      query: queryText,
      verdictSummary,
      explanation,
      correctedFact,
      fallaciesDetected,
      googleFactChecks,
      aiInsights: {
        engineUsed:
          payload.engine === 'gemini' ? 'Gemini 2.5 Flash' : 'DeepSeek V4 Flash',
        contextNarrative,
        credibilityAnalysis,
        recommendations,
      },
      timestamp: new Date().toISOString(),
    };
  }

  private createLimitReachedResult(query: string, engine: string, factChecks: any[]): FactCheckResult {
    const engineName = engine === 'gemini' ? 'Gemini' : 'DeepSeek';
    const altEngine = engine === 'gemini' ? 'DeepSeek' : 'Gemini';

    return {
      trustScore: 50,
      status: 'neutral',
      query: query,
      verdictSummary: `⚠️ LIMIT HARIAN AI TERCAPAI (ERROR 429)`,
      explanation: `Maaf, kuota harian analisis AI (${engineName}) kami sedang mencapai batas limit (Rate Limit 429). Seluruh proses pemindaian (termasuk Fact Check) dihentikan sementara untuk menjaga stabilitas sistem. Mohon coba lagi beberapa saat lagi.`,
      correctedFact: 'Layanan pemindaian sedang sibuk. Silakan periksa kembali nanti.',
      fallaciesDetected: ['LIMIT_EXCEEDED'],
      googleFactChecks: [],
      aiInsights: {
        engineUsed: engine === 'gemini' ? 'Gemini 2.5 Flash' : 'DeepSeek V4 Flash',
        contextNarrative: 'Analisis dihentikan karena limit harian tercapai.',
        credibilityAnalysis: 'Sistem tidak dapat melakukan kalkulasi saat ini.',
        recommendations: `Gunakan engine AI lain (${altEngine}) sebagai alternatif atau coba lagi dalam beberapa jam.`,
      },
      timestamp: new Date().toISOString(),
    };
  }
}
