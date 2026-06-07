# Cyber Threat Shield & Hoax Checker

Sistem keamanan siber terintegrasi yang terdiri dari aplikasi Android (Jetpack Compose) dan Backend (NestJS BFF). Aplikasi ini mampu mendeteksi berita hoaks menggunakan AI (Gemini/DeepSeek) serta memindai URL dan file berbahaya menggunakan engine VirusTotal.

## Cara Menjalankan Project

### 1. Persiapan Backend (BFF)
Backend berfungsi sebagai jembatan antara aplikasi Android dan API pihak ketiga (Gemini, VirusTotal, dll).

1. Buka terminal dan masuk ke folder backend:
   ```bash
   cd backend
   ```
2. Instal dependensi:
   ```bash
   npm install
   ```
3. Konfigurasi Environment:
   - Salin file `.env.example` dan paste di folder yg sama, lalu ubah namanya menjadi `.env`.
   - Masukkan API Key Anda (Gemini, Google Fact Check, VirusTotal).
4. Jalankan server:
   ```bash
   npm run start:dev
   ```
   Server akan berjalan di `http://localhost:3000`.

### 2. Persiapan Android App
Aplikasi ini dikembangkan menggunakan Jetpack Compose.

1. Buka project menggunakan **Android Studio**.
2. Pilih Devices, gunakan hp atau emulator
3. Klik tombol **Run** di Android Studio.

---

### Environment Variables (Backend)
- Isi file `.env` td di folder backend untuk mengaktifkan fitur:
- **Hoax Checker**: Memerlukan `GEMINI_API_KEY` atau `DEEPSEEK_API_KEY`.
- **Global Fact Check**: Memerlukan `GOOGLE_FACT_CHECK_API_KEY`.
- **Scam Scanner**: Memerlukan `VIRUSTOTAL_API_KEY`.

---

## 🛡️ Fitur Utama
- **Real-time Hoax Analysis**: Menggunakan Multi-LLM (Gemini 2.5 Flash & DeepSeek V4 Flash) untuk analisis narasi, bias, dan falasi logika.
- **Global Fact Check Integration**: Verifikasi silang klaim berita menggunakan API Google Fact Check secara otomatis.
- **Real-Time Notification Guard**: Memantau notifikasi masuk secara real-time. Sistem otomatis mengklasifikasikan isi notifikasi:
  - **Auto News Scan**: Menganalisis berita viral atau rumor.
  - **Auto Link Scan**: Memindai URL mencurigakan menggunakan VirusTotal.
  - **Smart Filtering**: Mengabaikan notifikasi sistem dan duplikasi untuk menghemat kuota API.
- **Cyber Scam Scanner**: Deteksi link phishing dan malware pada file APK/Dokumen secara manual.
- **OCR Hoax Scanner**: Ekstrak teks dari screenshot berita untuk dianalisis oleh AI.
- **Notification Logs & History**: Menu khusus untuk melihat riwayat notifikasi yang ditangkap dan hasil analisisnya secara mendalam.
