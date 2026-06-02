/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import axios from 'axios';

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
  correctedFact: string; // Fakta yang sebenarnya (Koreksi)
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
  trustScore: number;
  verdictSummary: string;
  explanation: string;
  correctedFact: string;
  fallaciesDetected: string[];
  contextNarrative: string;
  credibilityAnalysis: string;
  recommendations: string;
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
    const geminiKey = process.env.GEMINI_API_KEY;
    const deepseekKey = process.env.DEEPSEEK_API_KEY;

    const isGoogleKeyMissing =
      !googleApiKey ||
      googleApiKey === 'your_google_fact_check_api_key_here' ||
      googleApiKey.trim() === '';
    const isGeminiKeyMissing =
      !geminiKey ||
      geminiKey === 'your_gemini_api_key_here' ||
      geminiKey.trim() === '';
    const isDeepseekKeyMissing =
      !deepseekKey ||
      deepseekKey === 'your_deepseek_api_key_here' ||
      deepseekKey.trim() === '';

    if (isGoogleKeyMissing) {
      throw new BadRequestException(
        'API_KEY_NOT_CONFIGURED: Google Fact Check API Key belum dikoneksikan di file .env backend!',
      );
    }

    if (payload.engine === 'gemini' && isGeminiKeyMissing) {
      throw new BadRequestException(
        'API_KEY_NOT_CONFIGURED: Gemini API Key belum dikoneksikan di file .env backend!',
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
    try {
      const response = (await axios.get(
        `https://factchecktools.googleapis.com/v1alpha1/claims:search`,
        {
          params: {
            query: queryText,
            key: googleApiKey,
            languageCode: 'id',
          },
        },
      )) as unknown as { data: GoogleFactCheckResponse };

      if (response.data && response.data.claims) {
        googleFactChecks = response.data.claims.map(
          (c: GoogleFactCheckClaim) => ({
            claim: c.text,
            claimant: c.claimant || 'Tidak diketahui',
            verdict: c.claimReview?.[0]?.textualRating || 'Belum terverifikasi',
            reviewDate: c.claimReview?.[0]?.reviewDate || '',
            publisher: c.claimReview?.[0]?.publisher?.name || 'Fact Checker',
            url: c.claimReview?.[0]?.url || '',
          }),
        );
      }
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : 'Unknown error occurred';
      this.logger.warn(`Google Fact Check API failed (graceful fallback): ${message}`);
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
      const prompt = `Anda adalah mesin AI Keamanan Siber dan Anti-Disinformasi Tercanggih (Shield AI Engine).
Analisis secara mendalam teks klaim/berita berikut untuk mendeteksi kebenaran, potensi hoaks, manipulasi naratif, atau rekayasa sosial.

Klaim yang diinput Pengguna: "${queryText}"
Data Kecocokan Google Fact Check Instan: ${JSON.stringify(googleFactChecks)}

TUGAS UTAMA ANDA:
1. **Analisis Konteks Global**: Lacak, cari, dan evaluasi narasi klaim ini secara global berdasarkan database pengetahuan Anda dari berbagai website, platform media sosial (seperti Instagram, TikTok, Facebook, WhatsApp berantai), dan rilis berita media nasional/global.
2. **Koreksi Fakta Sebenarnya**: Tuliskan ringkasan informasi yang BENAR dan faktual di dalam field "correctedFact" (maksimal 2 kalimat tegas, dimulai dengan emoji ✅, menerangkan fakta sebenarnya yang bertolak belakang dengan hoaks tersebut).
3. **Temukan Sumber & Referensi Nyata**: Di dalam field "contextNarrative", Anda WAJIB menyertakan ringkasan analisis narasi DAN menyertakan daftar bullet-points sumber referensi digital valid (seperti situs turnbackhoax.id, website Kementerian Kominfo, kompas.com, tempo.co, detik.com, akun Instagram resmi yang menyebarkan atau membantah, dll.) yang berkaitan dengan informasi ini. Format daftar sumber referensi dengan jelas dalam bahasa Indonesia menggunakan format Markdown yang rapi!
4. **Analisis Kredibilitas**: Tulis analisis mendalam di field "credibilityAnalysis" mengenai tingkat kepercayaan sumber informasi asli, mengapa narasi ini bisa menyebar, dan kelemahan argumennya.
5. **Logical Fallacy**: Identifikasi kebohongan atau cacat logika berpikir (Logical Fallacy) yang terkandung di dalam teks klaim pada field "fallaciesDetected".

Format jawaban Anda harus berupa JSON valid tanpa embel-embel markdown block \`\`\`json. Skema JSON harus tepat seperti ini:
{
  "trustScore": <angka 0-100, di mana 100 sangat kredibel/aman, dan 0 sangat berbahaya/hoaks>,
  "verdictSummary": "<Kesimpulan status kebenaran dalam 1 kalimat tegas berpola emoji, misal: 🚨 HOAKS TERKONFIRMASI: ... atau ⚠️ MISLEADING: ... atau ✅ AMAN TERVERIFIKASI: ...>",
  "explanation": "<Penjelasan rinci dan analitis terstruktur dalam 2-3 paragraf mengapa berita ini valid atau hoaks>",
  "correctedFact": "<Informasi yang valid dan faktual sebenarnya untuk mengoreksi hoaks ini (Maksimal 2 kalimat)>",
  "fallaciesDetected": ["<Nama cacat logika 1>", "<cacat logika 2>"],
  "contextNarrative": "<Ringkasan evaluasi penyebaran global.\\n\\n**SUMBER REFERENSI VERIFIKASI DIGITAL:**\\n• 🌐 **Situs/Portal Berita**: [Nama Media] (keterangan debunk/rilis)\\n• 📱 **Media Sosial (IG/TikTok/WA)**: [Detail akun/grup yang menyebarkan]\\n• 🛡️ **Pihak Berwenang**: [Kominfo/Mafindo/BMKG dll.]>",
  "credibilityAnalysis": "<Analisis kredibilitas sumber, bukti digital, dan keandalan narasumber dalam 1-2 paragraf>",
  "recommendations": "<Saran mitigasi taktis bagi pengguna untuk menghindari penipuan atau kepanikan akibat klaim ini>"
}`;

      if (payload.engine === 'gemini' && geminiKey) {
        const llmResponse = (await axios.post(
          `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiKey}`,
          {
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { responseMimeType: 'application/json' },
          },
        )) as unknown as { data: GeminiResponse };

        const rawText =
          llmResponse.data.candidates?.[0]?.content?.parts?.[0]?.text || '';
        const parsed = JSON.parse(rawText.trim()) as LlmParsedResponse;
        trustScore = parsed.trustScore;
        verdictSummary = parsed.verdictSummary;
        explanation = parsed.explanation;
        correctedFact = parsed.correctedFact;
        fallaciesDetected = parsed.fallaciesDetected;
        contextNarrative = parsed.contextNarrative;
        credibilityAnalysis = parsed.credibilityAnalysis;
        recommendations = parsed.recommendations;
      } else {
        // DeepSeek integration
        const llmResponse = (await axios.post(
          'https://api.deepseek.com/v1/chat/completions',
          {
            model: 'deepseek-chat',
            messages: [{ role: 'user', content: prompt }],
            response_format: { type: 'json_object' },
          },
          {
            headers: { Authorization: `Bearer ${deepseekKey}` },
          },
        )) as unknown as { data: DeepSeekResponse };

        const rawText = llmResponse.data.choices?.[0]?.message?.content || '';
        const parsed = JSON.parse(rawText.trim()) as LlmParsedResponse;
        trustScore = parsed.trustScore;
        verdictSummary = parsed.verdictSummary;
        explanation = parsed.explanation;
        correctedFact = parsed.correctedFact;
        fallaciesDetected = parsed.fallaciesDetected;
        contextNarrative = parsed.contextNarrative;
        credibilityAnalysis = parsed.credibilityAnalysis;
        recommendations = parsed.recommendations;
      }
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Unknown error occurred';
      this.logger.error(`LLM Call failed: ${message}`);
      throw new BadRequestException(
        `LLM API Call failed (${payload.engine}): ${message}`,
      );
    }

    if (trustScore >= 75) {
      status = 'safe';
    } else if (trustScore >= 40) {
      status = 'neutral';
    } else {
      status = 'unsafe';
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
          payload.engine === 'gemini' ? 'Gemini 2.5 Flash' : 'DeepSeek V3',
        contextNarrative,
        credibilityAnalysis,
        recommendations,
      },
      timestamp: new Date().toISOString(),
    };
  }
}
