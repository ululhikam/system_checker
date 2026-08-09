import { Logger } from '@nestjs/common';
import { AxiosResponse } from 'axios';

/**
 * Mengambil semua Gemini API Key yang tersedia di .env
 * Mendukung format:
 * 1. GEMINI_API_KEYS=key1,key2,key3
 * 2. GEMINI_API_KEY=key1
 * 3. GEMINI_API_KEY_2=key2, GEMINI_API_KEY_3=key3, dst.
 */
export function getGeminiApiKeys(): string[] {
  const keys: string[] = [];

  // 1. Ambil dari GEMINI_API_KEYS (dipisahkan koma atau spasi)
  if (process.env.GEMINI_API_KEYS) {
    const splitKeys = process.env.GEMINI_API_KEYS
      .split(/[,;\s]+/)
      .map((k) => k.trim())
      .filter(Boolean);
    keys.push(...splitKeys);
  }

  // 2. Ambil dari GEMINI_API_KEY, GEMINI_API_KEY_1 s/d GEMINI_API_KEY_10
  const envKeys = [
    process.env.GEMINI_API_KEY,
    process.env.GEMINI_API_KEY_1,
    process.env.GEMINI_API_KEY_2,
    process.env.GEMINI_API_KEY_3,
    process.env.GEMINI_API_KEY_4,
    process.env.GEMINI_API_KEY_5,
    process.env.GEMINI_API_KEY_6,
    process.env.GEMINI_API_KEY_7,
    process.env.GEMINI_API_KEY_8,
    process.env.GEMINI_API_KEY_9,
    process.env.GEMINI_API_KEY_10,
  ];

  for (const k of envKeys) {
    if (
      k &&
      k.trim() !== '' &&
      k !== 'your_gemini_api_key' &&
      !keys.includes(k.trim())
    ) {
      keys.push(k.trim());
    }
  }

  return keys.filter(
    (k) => k && k !== 'your_gemini_api_key' && k.length > 5,
  );
}

/**
 * Mengeksekusi permintaan HTTP ke Gemini API dengan rotasi otomatis (fallback)
 * jika terjadi Rate Limit (Error HTTP 429 / RESOURCE_EXHAUSTED).
 */
export async function callGeminiApiWithFallback<T = any>(
  makeRequest: (apiKey: string) => Promise<AxiosResponse<T>>,
  logger?: Logger,
  label: string = 'Gemini API',
): Promise<AxiosResponse<T>> {
  const keys = getGeminiApiKeys();

  if (keys.length === 0) {
    throw new Error(
      'API_KEY_NOT_CONFIGURED: Tidak ada Gemini API Key yang valid di file .env backend!',
    );
  }

  let lastError: any = null;

  for (let index = 0; index < keys.length; index++) {
    const currentKey = keys[index];
    const maskedKey =
      currentKey.length > 10
        ? `${currentKey.substring(0, 6)}...${currentKey.substring(currentKey.length - 4)}`
        : currentKey;

    let retriesOnCurrentKey = 1;
    let delayMs = 1000;

    while (retriesOnCurrentKey >= 0) {
      try {
        if (logger) {
          logger.log(
            `Menggunakan Gemini Key #${index + 1} (${maskedKey}) untuk ${label}`,
          );
        }
        const response = await makeRequest(currentKey);
        return response;
      } catch (err: any) {
        lastError = err;
        const status = err.response?.status;
        const errorData = err.response?.data;
        const errorString = JSON.stringify(errorData || {});

        const isRateLimit =
          status === 429 ||
          errorString.includes('RESOURCE_EXHAUSTED') ||
          errorString.includes('Quota exceeded') ||
          errorString.includes('RATE_LIMIT_EXHAUSTED');

        const isInvalidKey = status === 400 && errorString.includes('API_KEY_INVALID');

        if (isRateLimit || isInvalidKey) {
          if (retriesOnCurrentKey > 0 && isRateLimit) {
            if (logger) {
              logger.warn(
                `⚡ Gemini Key #${index + 1} (${maskedKey}) terkena Rate Limit 429. Mencoba ulang dalam ${delayMs}ms...`,
              );
            }
            await new Promise((res) => setTimeout(res, delayMs));
            delayMs *= 2;
            retriesOnCurrentKey--;
          } else {
            // Pindah ke API key berikutnya!
            if (logger) {
              logger.warn(
                `🔄 Gemini Key #${index + 1} (${maskedKey}) limit habis. Beralih otomatis ke API Key berikutnya (Key #${index + 2})...`,
              );
            }
            break; // Keluar dari inner retry loop untuk mencoba key berikutnya
          }
        } else {
          // Error lain selain 429 / Auth error
          throw err;
        }
      }
    }
  }

  if (logger) {
    logger.error(
      `❌ Seluruh (${keys.length}) Gemini API Key telah mencapai batas limit harian (Rate Limit 429)!`,
    );
  }
  throw lastError;
}
