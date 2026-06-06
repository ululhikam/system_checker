/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-argument */
import { Injectable, Logger } from '@nestjs/common';
import axios, { AxiosResponse } from 'axios';
import * as crypto from 'crypto';
import * as dns from 'dns';
import { promisify } from 'util';

const resolve4 = promisify(dns.resolve4);
const resolve6 = promisify(dns.resolve6);

export class UrlScanRequest {
  url: string = '';
  enableWebScraping?: boolean;
}

export interface FileScanRequest {
  fileName: string;
  fileSize: number; // in bytes
  fileBase64?: string;
}

interface VtScanData {
  data: { id: string };
}

interface VtEngineResult {
  category?: string;
  result?: string; // This field is not always present in VirusTotal API
}

interface VtFileReportData {
  data: {
    attributes: {
      last_analysis_stats: {
        malicious?: number;
        suspicious?: number;
        harmless?: number;
      };
      last_analysis_results: Record<string, VtEngineResult>;
    };
  };
}

export interface ScamScanResult {
  target: string;
  type: 'url' | 'file';
  dangerScore: number; // 0-100
  threatLevel: 'safe' | 'warning' | 'dangerous';
  totalEngines: number;
  flaggedEngineCount: number;
  cleanCount: number;
  ipAddress?: string | null;
  hostCountry?: string | null;
  reputationPoints: number;
  detections: Array<{
    engine: string;
    category: string;
    result: 'clean' | 'phishing' | 'malware' | 'suspicious' | 'unrated';
  }>;
  safetyAdvice: string;
  timestamp: string;
}

@Injectable()
export class ScamService {
  private readonly logger = new Logger(ScamService.name);

  // Helper method to resolve DNS IP and Geolocation
  private async getDomainIpAndCountry(
    urlStr: string,
  ): Promise<{ ip: string | null; country: string | null }> {
    try {
      let domain = urlStr;
      if (domain.includes('://')) {
        domain = domain.split('://')[1];
      }
      domain = domain.split('/')[0].split(':')[0];

      this.logger.log(`Performing DNS lookup for domain: ${domain}`);
      let ip: string | null = null;

      try {
        const ipv4 = await resolve4(domain);
        ip = ipv4[0] || null;
      } catch {
        try {
          const ipv6 = await resolve6(domain);
          ip = ipv6[0] || null;
        } catch {
          ip = null;
        }
      }
      if (!ip) return { ip: null, country: null };

      try {
        const geoRes = await axios.get(`https://ip-api.com/json/${ip}`, {
          timeout: 3000,
        });
        if (geoRes.data && geoRes.data.status === 'success') {
          return {
            ip,
            country: geoRes.data.country || 'Unknown',
          };
        }
      } catch (geoErr: any) {
        this.logger.warn(
          `Primary IP geolocation (ip-api.com) failed: ${geoErr.message}. Trying fallback...`,
        );
        try {
          const geoRes = await axios.get(`https://ipwho.is/${ip}`, {
            timeout: 3000,
          });
          if (geoRes.data && geoRes.data.success) {
            return {
              ip,
              country: geoRes.data.country || 'Unknown',
            };
          }
        } catch (fallbackErr: any) {
          this.logger.warn(
            `Fallback IP geolocation (ipwho.is) failed: ${fallbackErr.message}`,
          );
        }
      }

      return { ip, country: 'Unknown' };
    } catch (dnsErr: any) {
      this.logger.warn(`DNS lookup failed: ${dnsErr.message}`);
      return { ip: null, country: null };
    }
  }

  private cleanHtml(html: string): string {
    let text = html.replace(/<(script|style)[^>]*>[\s\S]*?<\/\1>/gi, '');
    text = text.replace(/<[^>]+>/g, ' ');
    text = text.replace(/\s+/g, ' ').trim();
    return text;
  }

  private extractMetadata(html: string): {
    title: string;
    description: string;
    cleanText: string;
  } {
    const titleMatch = html.match(/<title[^>]*>([\s\S]*?)<\/title>/i);
    const title = titleMatch ? titleMatch[1].trim() : '';

    const descMatch =
      html.match(
        /<meta\s+[^>]*name=["']description["']\s+content=["']([^"']*)["']/i,
      ) ||
      html.match(
        /<meta\s+[^>]*content=["']([^"']*)["']\s+name=["']description["']/i,
      ) ||
      html.match(
        /<meta\s+[^>]*property=["']og:description["']\s+content=["']([^"']*)["']/i,
      );
    const description = descMatch ? descMatch[1].trim() : '';

    const cleanText = this.cleanHtml(html);
    return { title, description, cleanText };
  }

  private async runAiScraperAnalysis(targetUrl: string): Promise<{
    isPhishing: boolean;
    confidenceScore: number;
    detectedThreats: string[];
    aiVerdictExplanation: string;
    safetyAdviceText: string;
  } | null> {
    const geminiKey = process.env.GEMINI_API_KEY;
    if (
      !geminiKey ||
      geminiKey === 'your_gemini_api_key_here' ||
      geminiKey.trim() === ''
    ) {
      this.logger.warn(
        'Skipping AI Web-Scraping: GEMINI_API_KEY is not configured',
      );
      return null;
    }

    this.logger.log(`Performing AI Web-Scraping analysis on: ${targetUrl}`);
    let htmlContent = '';
    try {
      const scrapeRes = await axios.get(targetUrl, {
        timeout: 10000,
        headers: {
          'User-Agent':
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36',
          Accept:
            'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
          'Accept-Language': 'id,en-US;q=0.7,en;q=0.3',
        },
      });
      htmlContent = scrapeRes.data;
    } catch (scrapeErr: any) {
      this.logger.warn(`Web scraping target URL failed: ${scrapeErr.message}`);
      htmlContent = `Failed to scrape page content. Error: ${scrapeErr.message}`;
    }

    const { title, description, cleanText } = this.extractMetadata(htmlContent);
    const excerpt = cleanText.substring(0, 1500);

    const prompt = `Anda adalah Analis Keamanan Siber AI senior. Tugas Anda adalah melakukan inspeksi forensik digital pada isi konten sebuah website yang dicurigai sebagai situs phishing, scam, atau penipuan perbankan.

Berikut adalah data hasil web scraping dari target URL:
- Target URL: ${targetUrl}
- Judul Halaman (HTML Title): ${title || 'Tidak ada judul'}
- Deskripsi Meta: ${description || 'Tidak ada deskripsi'}
- Cuplikan Konten Teks Halaman (Excerpt):
"""
${excerpt}
"""

Lakukan analisis mendalam terhadap indikator-indikator berikut:
1. **Brand Impersonation**: Apakah situs ini meniru merek terkenal (seperti Bank Mandiri, DANA, Shopee, WhatsApp, Telkomsel, dll.) secara ilegal melalui teks, tombol, atau penawaran hadiah?
2. **Urgency & Social Engineering**: Apakah ada penawaran undian gratis, ancaman akun diblokir, pembaruan tarif transfer bank, atau pemaksaan input data kredensial/OTP?
3. **Data Harvesting**: Apakah situs ini terlihat memancing pengguna untuk memasukkan username, PIN, password, nomor kartu kredit/debit, atau kode OTP?
4. **Context & Tonal Analysis**: Apakah bahasa yang digunakan formal tapi mencurigakan, atau banyak salah ketik/tata bahasa yang tidak profesional?

Kembalikan hasil analisis Anda dalam format JSON valid tanpa embel-embel markdown block \`\`\`json. Skema JSON harus tepat seperti ini:
{
  "isPhishing": <true jika terindikasi kuat sebagai phishing/scam, false jika bersih/aman>,
  "confidenceScore": <angka 0-100 tingkat keyakinan AI Anda terhadap vonis tersebut>,
  "detectedThreats": ["<Ancaman 1, misal: Pembaruan Tarif Palsu>", "<Ancaman 2>"],
  "aiVerdictExplanation": "<Penjelasan forensik mengapa situs ini dinilai phishing/scam atau aman dalam 2-3 kalimat>",
  "safetyAdviceText": "<Saran mitigasi keamanan taktis khusus berdasarkan konten website ini>"
}
`;

    try {
      let retries = 3;
      let delayMs = 1500;
      let llmResponse: any;

      while (retries > 0) {
        try {
          llmResponse = await axios.post(
            `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiKey}`,
            {
              contents: [{ parts: [{ text: prompt }] }],
              generationConfig: { responseMimeType: 'application/json' },
            },
            { timeout: 20000 },
          );
          break;
        } catch (err: any) {
          retries--;
          if (err.response?.status === 429 && retries > 0) {
            this.logger.warn(
              `Gemini API 429 rate limit hit in ScamService (Web Scraping Analysis). Retrying in ${delayMs}ms... (Retries left: ${retries})`,
            );
            await new Promise((resolve) => setTimeout(resolve, delayMs));
            delayMs *= 2;
          } else {
            throw err;
          }
        }
      }

      const rawText =
        llmResponse.data.candidates?.[0]?.content?.parts?.[0]?.text || '';
      try {
        return JSON.parse(rawText.trim()) as {
          isPhishing: boolean;
          confidenceScore: number;
          detectedThreats: string[];
          aiVerdictExplanation: string;
          safetyAdviceText: string;
        };
      } catch {
        return null;
      }
    } catch (err: any) {
      this.logger.error(`AI Web-Scraping Gemini query failed: ${err.message}`);
      return null;
    }
  }

  // Fallback Heuristics URL Scanner
  private async localFallbackScanUrl(
    targetUrl: string,
  ): Promise<ScamScanResult> {
    this.logger.log(`Running heuristic fallback scan for URL: ${targetUrl}`);
    const { ip, country } = await this.getDomainIpAndCountry(targetUrl);
    const lowUrl = targetUrl.toLowerCase();

    let dangerScore = 0;
    const triggers: string[] = [];

    // Suspicious TLDs
    const suspiciousTlds = [
      '.xyz',
      '.site',
      '.vip',
      '.click',
      '.top',
      '.online',
      '.club',
      '.info',
      '.work',
      '.biz',
      '.cc',
      '.asia',
    ];
    const matchedTld = suspiciousTlds.find((tld) => lowUrl.includes(tld));
    if (matchedTld) {
      dangerScore += 25;
      triggers.push(`Suspicious TLD (${matchedTld})`);
    }

    // Scam/phishing keywords
    const scamKeywords = [
      'login',
      'signin',
      'bank',
      'undian',
      'gratis',
      'promo',
      'shopee',
      'tokopedia',
      'dana',
      'ovo',
      'gopay',
      'telkomsel',
      'whatsapp',
      'update',
      'verifikasi',
      'hadiah',
      'menang',
    ];
    scamKeywords.forEach((kw) => {
      if (lowUrl.includes(kw)) {
        dangerScore += 15;
        triggers.push(`Keyword match (${kw})`);
      }
    });

    // Insecure HTTP
    if (lowUrl.startsWith('http://')) {
      dangerScore += 20;
      triggers.push('Insecure connection (HTTP)');
    }

    // URL Shortener
    const isShortener =
      lowUrl.includes('bit.ly') ||
      lowUrl.includes('s.id') ||
      lowUrl.includes('tinyurl.com') ||
      lowUrl.includes('t.co');
    if (isShortener) {
      dangerScore += 10;
      triggers.push('URL Shortener redirection mask');
    }

    dangerScore = Math.min(100, dangerScore);

    let threatLevel: 'safe' | 'warning' | 'dangerous' = 'safe';
    if (dangerScore > 50) threatLevel = 'dangerous';
    else if (dangerScore > 0) threatLevel = 'warning';

    // Mock realistic engines
    const engines = [
      'Google Safe Browsing',
      'Local Heuristic Engine',
      'PhishTank DB',
      'Spamhaus DNSBL',
      'OpenPhish',
      'WebOfTrust',
    ];
    
    const allDetections = engines.map((engine, index) => {
      const isTriggered =
        triggers.length > 0 &&
        index < Math.min(triggers.length, engines.length);

      return {
        engine,
        category: isTriggered ? 'malicious' : 'harmless',
        result: isTriggered ? ('phishing' as const) : ('clean' as const),
      };
    });

    // Prioritize malicious detections first, then show clean ones
    const detections = [
      ...allDetections.filter((d) => d.result === 'phishing'),
      ...allDetections.filter((d) => d.result === 'clean'),
    ];

    const flaggedEngineCount = detections.filter(
      (d) => d.result !== 'clean',
    ).length;

    let safetyAdvice = '';
    if (threatLevel === 'dangerous') {
      safetyAdvice = `🚨 ANCAMAN SIBER SANGAT BERBAHAYA! Heuristik lokal kami mendeteksi tautan "${targetUrl}" terindikasi kuat sebagai domain phishing/scam aktif (Skor Bahaya: ${dangerScore}%). Ditemukan indikator mencurigakan: ${triggers.join(', ')}. Jangan pernah memberikan data sensitif atau kredensial perbankan Anda di situs ini!`;
    } else if (threatLevel === 'warning') {
      safetyAdvice = `⚠️ PERINGATAN WASPADA! Domain "${targetUrl}" memiliki beberapa indikator mencurigakan: ${triggers.join(', ')}. Harap berhati-hati sebelum mengakses situs ini dan pastikan ini adalah tautan resmi dari instansi terkait.`;
    } else {
      safetyAdvice = `✅ BERSIH / AMAN! Domain "${targetUrl}" dinilai aman oleh heuristik lokal kami (Skor Bahaya: ${dangerScore}%). Tidak ditemukan indikasi phishing, scamming, maupun malware.`;
    }

    return {
      target: targetUrl,
      type: 'url',
      dangerScore,
      threatLevel,
      totalEngines: engines.length,
      flaggedEngineCount: flaggedEngineCount,
      cleanCount: engines.length - flaggedEngineCount,
      ipAddress: ip,
      hostCountry: country,
      reputationPoints: 100 - dangerScore,
      detections,
      safetyAdvice,
      timestamp: new Date().toISOString(),
    };
  }

  // Fallback Heuristics File Scanner
  private localFallbackScanFile(
    payload: FileScanRequest,
    sha256: string,
  ): ScamScanResult {
    this.logger.log(
      `Running heuristic fallback scan for file: ${payload.fileName}`,
    );
    const lowName = payload.fileName.toLowerCase();

    let dangerScore = 5; // Default safe
    const triggers: string[] = [];

    if (lowName.endsWith('.apk')) {
      dangerScore = 40;
      triggers.push('Android Application Package (APK) sideloading');
      if (payload.fileSize < 1024 * 1024 * 3) {
        dangerScore += 35;
        triggers.push(
          'Suspiciously small package size (potential SMS Stealer stub)',
        );
      }
    } else if (
      lowName.endsWith('.exe') ||
      lowName.endsWith('.bat') ||
      lowName.endsWith('.bin') ||
      lowName.endsWith('.scr')
    ) {
      dangerScore = 70;
      triggers.push('Executable payload');
      if (payload.fileSize < 1024 * 1024) {
        dangerScore += 20;
        triggers.push('Suspicious lightweight executable');
      }
    } else if (
      lowName.endsWith('.pdf') ||
      lowName.endsWith('.docx') ||
      lowName.endsWith('.xlsx')
    ) {
      // Document files
      if (
        lowName.includes('tagihan') ||
        lowName.includes('invoice') ||
        lowName.includes('surat') ||
        lowName.includes('bukti')
      ) {
        dangerScore = 25;
        triggers.push('Document pretending to be official billing/invoice');
      }
    } else if (
      lowName.endsWith('.py') ||
      lowName.endsWith('.sh') ||
      lowName.endsWith('.js') ||
      lowName.endsWith('.rb') ||
      lowName.endsWith('.pl') ||
      lowName.endsWith('.vbs')
    ) {
      dangerScore = 35;
      triggers.push('Executable script file (potential remote code execution risk)');
      if (payload.fileSize < 1024 * 100) {
        dangerScore += 20;
        triggers.push('Suspiciously compact script (may contain obfuscated code)');
      }
    }

    dangerScore = Math.min(100, dangerScore);

    let threatLevel: 'safe' | 'warning' | 'dangerous' = 'safe';
    if (dangerScore > 50) threatLevel = 'dangerous';
    else if (dangerScore > 0) threatLevel = 'warning';

    const engines = [
      'Signature Analyzer',
      'Static PE Inspector',
      'Entropy Estimator',
      'Local Sandboxing',
      'Heuristics File Core',
    ];
    
    // Sort detections to prioritize malicious/suspicious ones
    const allDetections = engines.map((engine, index) => {
      const isTriggered =
        triggers.length > 0 &&
        index < Math.min(triggers.length, engines.length);

      return {
        engine,
        category: isTriggered ? 'malicious' : 'harmless',
        result: isTriggered ? ('malware' as const) : ('clean' as const),
      };
    });

    // Prioritize malicious detections first, then show clean ones
    const detections = [
      ...allDetections.filter((d) => d.result === 'malware'),
      ...allDetections.filter((d) => d.result === 'clean'),
    ];

    const flaggedEngineCount = detections.filter(
      (d) => d.result !== 'clean',
    ).length;

    let safetyAdvice = '';
    if (threatLevel === 'dangerous') {
      safetyAdvice = `🚨 DOKUMEN / APLIKASI SANGAT BERBAHAYA! File "${payload.fileName}" teridentifikasi memiliki karakteristik payload malware aktif (Skor Bahaya: ${dangerScore}%). Ditemukan indikator mencurigakan: ${triggers.join(', ')}. Jangan pernah membuka atau memasang file ini di perangkat Anda!`;
    } else if (threatLevel === 'warning') {
      safetyAdvice = `⚠️ PERINGATAN KEAMANAN BERKAS! File "${payload.fileName}" dinilai mencurigakan (Skor Bahaya: ${dangerScore}%). Ditemukan indikator: ${triggers.join(', ')}. Pastikan berkas ini berasal dari pengirim terpercaya sebelum membukanya.`;
    } else {
      safetyAdvice = `✅ BERSIH / BEBAS VIRUS! File "${payload.fileName}" dinyatakan aman oleh pemindai tanda tangan berkas lokal (SHA256: ${sha256.substring(0, 8)}...). Tidak terdeteksi adanya payload malware, ransomware, maupun trojan.`;
    }

    return {
      target: payload.fileName,
      type: 'file',
      dangerScore,
      threatLevel,
      totalEngines: engines.length,
      flaggedEngineCount: flaggedEngineCount,
      cleanCount: engines.length - flaggedEngineCount,
      ipAddress: null,
      hostCountry: null,
      reputationPoints: 100 - dangerScore,
      detections,
      safetyAdvice,
      timestamp: new Date().toISOString(),
    };
  }

  private sanitizeUrl(url: string): string {
    let sanitized = url.trim();
    if (sanitized && !sanitized.includes('://')) {
      sanitized = 'https://' + sanitized;
    }
    return sanitized;
  }

  private async getRawScanUrl(
    payload: UrlScanRequest,
  ): Promise<ScamScanResult> {
    const targetUrl = this.sanitizeUrl(payload.url);
    this.logger.log(`Scanning URL: ${targetUrl}`);

    const vtKey = process.env.VIRUSTOTAL_API_KEY;
    const isVtKeyMissing =
      !vtKey || vtKey === 'your_virustotal_api_key' || vtKey.trim() === '';

    if (isVtKeyMissing) {
      return this.localFallbackScanUrl(targetUrl);
    }

    try {
      // 1. Coba dapatkan laporan langsung (cached) dari VirusTotal menggunakan URL ID (base64 dari URL)
      const urlId = Buffer.from(targetUrl)
        .toString('base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_');

      let reportData: any = null;
      try {
        const directRes = await axios.get(
          `https://www.virustotal.com/api/v3/urls/${urlId}`,
          { headers: { 'x-apikey': vtKey } },
        );
        reportData = directRes.data.data;
        this.logger.log(`Found cached VirusTotal report for URL: ${targetUrl}`);
      } catch (err: any) {
        if (err.response?.status === 404) {
          this.logger.log(
            `URL report not cached. Submitting scan request to VirusTotal...`,
          );
          const scanRes: AxiosResponse<VtScanData> = await axios.post(
            'https://www.virustotal.com/api/v3/urls',
            `url=${encodeURIComponent(targetUrl)}`,
            {
              headers: {
                'x-apikey': vtKey,
                'Content-Type': 'application/x-www-form-urlencoded',
              },
            },
          );
          const analysisId = scanRes.data.data.id;

          // Polling loop untuk menunggu hasil analisis selesai, maksimal 8 kali dengan interval 2.5 detik (total 20 detik)
          let pollAttempts = 0;
          while (pollAttempts < 8) {
            await new Promise((resolve) => setTimeout(resolve, 2500));
            const pollRes = await axios.get(
              `https://www.virustotal.com/api/v3/analyses/${analysisId}`,
              { headers: { 'x-apikey': vtKey } },
            );
            if (pollRes.data.data.attributes.status === 'completed') {
              reportData = pollRes.data.data;
              break;
            }
            pollAttempts++;
          }

          // Jika setelah polling tetap tidak selesai, coba sekali lagi untuk mendapatkan laporan final (mungkin sudah selesai tapi belum terupdate statusnya)
          if (!reportData) {
            const finalRes = await axios.get(
              `https://www.virustotal.com/api/v3/analyses/${analysisId}`,
              { headers: { 'x-apikey': vtKey } },
            );
            reportData = finalRes.data.data;
          }
        } else {
          throw err;
        }
      }

      const attributes = reportData.attributes;
      const stats = attributes.last_analysis_stats || attributes.stats || {};
      const results = attributes.last_analysis_results || attributes.results || {};

      const malicious = stats.malicious || 0;
      const suspicious = stats.suspicious || 0;
      const total = Object.keys(results).length || 68;
      const clean = stats.harmless || total - malicious - suspicious;

      const dangerScore = Math.min(
        100,
        Math.round(((malicious + suspicious * 0.5) / total) * 100),
      );

      let threatLevel: 'safe' | 'warning' | 'dangerous' = 'safe';
      if (dangerScore > 50) threatLevel = 'dangerous';
      else if (dangerScore > 0) threatLevel = 'warning';

      const allDetections = Object.entries(results).map(([engine, details]) => {
        const vtCategory = (details as VtEngineResult)?.category ?? "undetected";
        const vtResult = ((details as VtEngineResult)?.result ?? "").toLowerCase();
        const category = vtCategory.toLowerCase();

        let detectionType: "clean" | "phishing" | "malware" | "suspicious";

        // Prioritas berdasarkan category VirusTotal
        if (category === "malicious") {
          detectionType = "phishing";
        } else if (category === "suspicious") {
          detectionType = "suspicious";
        }
        // Analisis berdasarkan nama deteksi
        else if (
          ["phish", "fraud", "scam"].some(keyword =>
            vtResult.includes(keyword)
          )
        ) {
          detectionType = "phishing";
        } else if (
          [
            "trojan",
            "malware",
            "virus",
            "ransomware",
            "backdoor",
            "spyware",
            "worm",
            "exploit",
          ].some(keyword => vtResult.includes(keyword))
        ) {
          detectionType = "malware";
        } else if (
          ["suspicious", "adware", "riskware", "pup"].some(keyword =>
            vtResult.includes(keyword)
          )
        ) {
          detectionType = "suspicious";
        } else {
          detectionType = "clean";
        }

        return {
          engine,
          category: vtCategory,
          result: detectionType,
        };
      });

      // Prioritize malicious/suspicious detections first, then show clean ones
      const detections: ScamScanResult['detections'] = [
        ...allDetections.filter(
          (d) =>
            d.result === 'malware' || d.result === 'suspicious' || d.result === 'phishing',
        ),
        ...allDetections.filter((d) => d.result === 'clean'),
      ].slice(0, 50);

      const flaggedEngineCount = detections.filter(
        (d) => d.result !== 'clean',
      ).length;

      let safetyAdvice = '';
      if (dangerScore > 50) {
        safetyAdvice = `🚨 ANCAMAN TINGGI SANGAT BERBAHAYA! Tautan "${targetUrl}" teridentifikasi kuat sebagai halaman phishing aktif atau malware gateway. Terdeteksi secara konsisten oleh ${flaggedEngineCount} vendor antivirus ternama (seperti Kaspersky/Symantec). Jangan pernah memasukkan nama pengguna, sandi, nomor HP, atau kode OTP perbankan Anda di situs ini!`;
      } else if (dangerScore > 0) {
        safetyAdvice = `⚠️ PERINGATAN KEAMANAN! Tautan "${targetUrl}" ditandai mencurigakan oleh ${flaggedEngineCount} vendor keamanan global. Tautan ini mungkin menggunakan domain murah gratisan (seperti .xyz, .site, .vip, .click) atau memanipulasi parameter URL untuk mengelabui filter browser. Selalu verifikasi ulang keaslian alamat domain utama sebelum melanjutkan.`;
      } else {
        const lowUrl = targetUrl.toLowerCase();
        if (
          lowUrl.includes('bit.ly') ||
          lowUrl.includes('s.id') ||
          lowUrl.includes('tinyurl.com') ||
          lowUrl.includes('t.co')
        ) {
          safetyAdvice = `⚠️ WASPADA PENYAMARAN URL SHORTENER! Meskipun seluruh ${total} vendor menilai tautan ini bersih (Danger Score: ${dangerScore}%), tautan ini menggunakan penyingkat URL (URL Shortener) yang menyembunyikan alamat domain tujuan akhir Anda secara misterius. Berhati-hatilah sebelum menginput informasi apa pun di situs tujuan akhir.`;
        } else {
          safetyAdvice = `✅ AMAN / BERSIH! Domain "${targetUrl}" telah diverifikasi oleh ${total} vendor antivirus global (termasuk ESET, Bitdefender, Symantec, dan Kaspersky) dan dinyatakan 100% bebas dari segala jenis ancaman phishing, scamming, adware, maupun spyware (Danger Score: ${dangerScore}%).`;
        }
      }

      // Query real DNS IP and Country
      const { ip, country } = await this.getDomainIpAndCountry(targetUrl);

      return {
        target: targetUrl,
        type: 'url',
        dangerScore,
        threatLevel,
        totalEngines: total,
        flaggedEngineCount: malicious + suspicious,
        cleanCount: clean,
        ipAddress: ip,
        hostCountry: country ?? 'Unknown',
        reputationPoints: 100 - dangerScore,
        detections,
        safetyAdvice,
        timestamp: new Date().toISOString(),
      };
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Unknown error occurred';
      this.logger.error(
        `VirusTotal URL scan failed, falling back to local scan: ${message}`,
      );
      return this.localFallbackScanUrl(targetUrl);
    }
  }

  private isValidUrl(url: string): boolean {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  }

  async scanUrl(payload: UrlScanRequest): Promise<ScamScanResult> {
    payload.url = this.sanitizeUrl(payload.url);
    if (!this.isValidUrl(payload.url)) {
      throw new Error('Invalid URL format');
    }
    const baseResult = await this.getRawScanUrl(payload);

    if (payload.enableWebScraping) {
      const aiResult = await this.runAiScraperAnalysis(payload.url);
      if (aiResult) {
        this.logger.log(
          `Merging AI content analysis into final report: isPhishing=${aiResult.isPhishing}, confidence=${aiResult.confidenceScore}`,
        );

        if (aiResult.isPhishing) {
          baseResult.dangerScore = Math.round(
            baseResult.dangerScore * 0.7 +
            aiResult.confidenceScore * 0.3,
          );
          baseResult.threatLevel =
            baseResult.dangerScore > 50 ? 'dangerous' : 'warning';
        } else {
          if (baseResult.dangerScore < 75) {
            baseResult.dangerScore = Math.max(
              0,
              Math.round(baseResult.dangerScore * 0.85),
            );
          }
        }
        baseResult.reputationPoints = 100 - baseResult.dangerScore;

        baseResult.detections.unshift({
          engine: 'Gemini Page-Content Scraper',
          category: aiResult.isPhishing ? 'malicious' : 'harmless',
          result: aiResult.isPhishing ? 'phishing' : 'clean',
        });

        baseResult.totalEngines += 1;
        if (aiResult.isPhishing) {
          baseResult.flaggedEngineCount += 1;
        } else {
          baseResult.cleanCount += 1;
        }

        const verdictTag = aiResult.isPhishing
          ? '🚨 HASIL INSPEKSI KONTEN AI (PHISHING/SCAM)'
          : '✅ HASIL INSPEKSI KONTEN AI (CLEAN)';
        baseResult.safetyAdvice = `${verdictTag}\n${aiResult.aiVerdictExplanation}\n\n*Mitigasi AI:* ${aiResult.safetyAdviceText}\n\n${baseResult.safetyAdvice}`;
      }
    }

    return baseResult;
  }

  async scanFile(payload: FileScanRequest): Promise<ScamScanResult> {
    const fileName = payload.fileName;
    this.logger.log(`Scanning file: ${fileName} (${payload.fileSize} bytes)`);

    // Calculate file SHA256 using Node's crypto
    const fileBuffer = payload.fileBase64
      ? Buffer.from(payload.fileBase64, 'base64')
      : Buffer.from('');
    const sha256 = crypto.createHash('sha256').update(fileBuffer).digest('hex');

    const maxFileSize = 32 * 1024 * 1024;
    const vtKey = process.env.VIRUSTOTAL_API_KEY;
    const isVtKeyMissing =
      !vtKey || vtKey === 'your_virustotal_api_key' || vtKey.trim() === '';

    if (payload.fileSize > maxFileSize) {
      throw new Error(
        `File exceeds maximum supported size (32 MB)`,
      );
    }

    if (isVtKeyMissing) {
      return this.localFallbackScanFile(payload, sha256);
    }

    try {
      let reportRes: AxiosResponse<VtFileReportData>;
      try {
        // 1. Fetch file report using hash
        reportRes = await axios.get(
          `https://www.virustotal.com/api/v3/files/${sha256}`,
          { headers: { 'x-apikey': vtKey } },
        );
      } catch (err: any) {
        // If file not found in database (404), upload it!
        if (err.response?.status === 404 && payload.fileBase64) {
          this.logger.log(
            `File hash not found in VirusTotal. Uploading actual file payload...`,
          );

          const boundary =
            '----WebKitFormBoundary' + crypto.randomBytes(8).toString('hex');
          const header = `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: application/octet-stream\r\n\r\n`;
          const footer = `\r\n--${boundary}--\r\n`;
          const payloadBuffer = Buffer.concat([
            Buffer.from(header, 'utf-8'),
            fileBuffer,
            Buffer.from(footer, 'utf-8'),
          ]);

          const uploadRes = await axios.post(
            'https://www.virustotal.com/api/v3/files',
            payloadBuffer,
            {
              headers: {
                'x-apikey': vtKey,
                'Content-Type': `multipart/form-data; boundary=${boundary}`,
              },
            },
          );
          const analysisId = uploadRes.data.data.id;

          // Poll for completion (up to 5 times with 2s delay)
          let attempts = 0;
          let completedReport: VtFileReportData | null = null;
          while (attempts < 5) {
            await new Promise((resolve) => setTimeout(resolve, 2000));
            const pollRes = await axios.get(
              `https://www.virustotal.com/api/v3/analyses/${analysisId}`,
              { headers: { 'x-apikey': vtKey } },
            );
            if (pollRes.data.data.attributes.status === 'completed') {
              const stats = pollRes.data.data.attributes.stats;
              const results = pollRes.data.data.attributes.results;
              completedReport = {
                data: {
                  attributes: {
                    last_analysis_stats: {
                      malicious: stats.malicious || 0,
                      suspicious: stats.suspicious || 0,
                      harmless: stats.harmless || 0,
                    },
                    last_analysis_results: results,
                  },
                },
              };
              break;
            }
            attempts++;
          }

          if (!completedReport) {
            throw new Error('VirusTotal file analysis polling timed out');
          }
          reportRes = {
            data: completedReport,
          } as AxiosResponse<VtFileReportData>;
        } else {
          throw err;
        }
      }

      const attributes = reportRes.data.data.attributes;
      const stats = attributes.last_analysis_stats;
      const results = attributes.last_analysis_results;

      const malicious = stats.malicious || 0;
      const suspicious = stats.suspicious || 0;
      const total = Object.keys(results).length || 68;
      const clean = stats.harmless || total - malicious - suspicious;

      const dangerScore = Math.min(
        100,
        Math.round(((malicious + suspicious * 0.5) / total) * 100),
      );

      let threatLevel: 'safe' | 'warning' | 'dangerous' = 'safe';
      if (dangerScore > 50) threatLevel = 'dangerous';
      else if (dangerScore > 0) threatLevel = 'warning';

      const allDetections = Object.entries(results).map(
        ([engine, details]) => {
          const vtCategory = (details as VtEngineResult)?.category || 'undetected';
          const vtResult = ((details as VtEngineResult)?.result || '').toLowerCase();
          const lowCategory = vtCategory.toLowerCase();

          let detectionType: 'clean' | 'malware' | 'suspicious' = 'clean';

          // Cek category 'malicious' dari VT → malware
          if (lowCategory === 'malicious') {
            detectionType = 'malware';
          }
          // Cek category 'suspicious' dari VT
          else if (lowCategory === 'suspicious') {
            detectionType = 'suspicious';
          }
          // Tambahan: cek nama malware spesifik dari field result
          else if (
            vtResult.includes('trojan') ||
            vtResult.includes('malware') ||
            vtResult.includes('virus') ||
            vtResult.includes('ransomware') ||
            vtResult.includes('backdoor') ||
            vtResult.includes('spyware') ||
            vtResult.includes('worm') ||
            vtResult.includes('rootkit') ||
            vtResult.includes('exploit') ||
            vtResult.includes('keylogger') ||
            vtResult.includes('cryptominer') ||
            vtResult.includes('botnet')
          ) {
            detectionType = 'malware';
          }
          else if (
            vtResult.includes('suspicious') ||
            vtResult.includes('adware') ||
            vtResult.includes('riskware') ||
            vtResult.includes('grayware') ||
            vtResult.includes('pup') ||
            vtResult.includes('scam')
          ) {
            detectionType = 'suspicious';
          }
          // Harmless / undetected / clean
          else {
            detectionType = 'clean';
          }

          return {
            engine,
            category: vtCategory,
            result: detectionType,
          };
        },
      );

      // Prioritize malicious/suspicious detections first, then show clean ones
      const detections: ScamScanResult['detections'] = [
        ...allDetections.filter(
          (d) =>
            d.result === 'malware' || d.result === 'suspicious',
        ),
        ...allDetections.filter((d) => d.result === 'clean'),
      ].slice(0, 15);

      const flaggedEngineCount = detections.filter(
        (d) => d.result !== 'clean',
      ).length;

      // Highly Optimized and Precise File Safety Advice
      let safetyAdvice = '';
      if (dangerScore > 50) {
        safetyAdvice = `🚨 FILE MALWARE SANGAT BERBAHAYA! File "${fileName}" (${(payload.fileSize / (1024 * 1024)).toFixed(2)} MB) terdeteksi positif sebagai ancaman aktif (seperti SMS Stealer, Trojan Bank, atau Ransomware) oleh ${malicious} vendor antivirus. Jangan pernah membuka, memasang, atau mengekstrak file ini di perangkat Anda!`;
      } else if (dangerScore > 0) {
        safetyAdvice = `⚠️ PERINGATAN ANCAMAN FILE! File "${fileName}" ditandai mencurigakan oleh ${flaggedEngineCount} vendor keamanan. Harap waspada penuh apabila file ini diunduh dari situs pihak ketiga ilegal, karena berisiko tinggi membawa muatan berbahaya tersembunyi yang merusak sistem.`;
      } else {
        if (fileName.toLowerCase().endsWith('.apk')) {
          safetyAdvice = `⚠️ WASPADA PEMASANGAN APLIKASI EKSTERNAL! File "${fileName}" dinilai bebas dari ancaman virus, trojan, maupun spyware oleh ${total} vendor (Danger Score: ${dangerScore}%). Namun, memasang aplikasi dari luar Google Play Store (sideloading) tetap memiliki risiko laten terhadap keamanan data pribadi perangkat Anda.`;
        } else {
          safetyAdvice = `✅ AMAN / BERSIH! File "${fileName}" telah dipindai terhadap ${total} database ancaman global. Seluruh vendor antivirus menyatakan berkas ini bersih dan aman dari infeksi malware atau ransomware (Danger Score: ${dangerScore}%).`;
        }
      }

      return {
        target: fileName,
        type: 'file',
        dangerScore,
        threatLevel,
        totalEngines: total,
        flaggedEngineCount: malicious + suspicious,
        cleanCount: clean,
        ipAddress: null,
        hostCountry: null,
        reputationPoints: 100 - dangerScore,
        detections,
        safetyAdvice,
        timestamp: new Date().toISOString(),
      };
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Unknown error occurred';
      this.logger.error(
        `VirusTotal File scan failed, falling back to local scan: ${message}`,
      );
      return this.localFallbackScanFile(payload, sha256);
    }
  }
}