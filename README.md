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
2. **Konfigurasi API (`ApiService.kt`)**:
   Penting untuk menyesuaikan `BASE_URL` agar aplikasi bisa terhubung ke backend:
   - File: `app/src/main/java/com/example/checker/data/ApiService.kt`
   - **Emulator**: Gunakan `http://10.0.2.2:3000/api/`
   - **HP Fisik**: Gunakan IP Laptop Anda, contoh: `http://192.168.1.15:3000/api/` (Pastikan satu jaringan Wi-Fi).
   - **Production**: Gunakan URL Hosting Anda, contoh: `https://your-api.vercel.app/api/`
3. Klik tombol **Run** di Android Studio.

---

## 🛠️ Konfigurasi Penting

### API Service (Android)
Pada file `ApiService.kt`, pastikan `BASE_URL` diatur dengan benar:
```kotlin
object NetworkClient {
    // Sesuaikan alamat BASE_URL dengan lokasi server backend Anda, contoh:
    private const val BASE_URL = "http://10.0.2.2:3000/api/" 
}
```

### Environment Variables (Backend)
- Isi file `.env` td di folder backend untuk mengaktifkan fitur:
- **Hoax Checker**: Memerlukan `GEMINI_API_KEY` atau `DEEPSEEK_API_KEY`.
- **Global Fact Check**: Memerlukan `GOOGLE_FACT_CHECK_API_KEY`.
- **Scam Scanner**: Memerlukan `VIRUSTOTAL_API_KEY`.

---

## 🛡️ Fitur Utama
- **Real-time Hoax Analysis**: Menggunakan Multi-LLM (Gemini 2.5 Flash & DeepSeek V4 Flash).
- **OCR Hoax Scanner**: Ambil screenshot berita dan biarkan AI mengekstrak teks serta menganalisisnya.
- **Cyber Scam Scanner**: Deteksi link phishing dan malware pada file APK/Dokumen.
- **Background Protection**: Memantau clipboard secara otomatis untuk melindungi dari link berbahaya.
- **Scan History**: Menyimpan riwayat pemindaian.
