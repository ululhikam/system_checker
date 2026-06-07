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
Aplikasi ini dikembangkan menggunakan Jetpack Compose di Android Studio.

1. Buka project menggunakan **Android Studio**.
2. Pastikan file Gradle sinkron dengan sukses.
3. Pilih perangkat (emulator atau HP fisik).
4. Klik tombol **Run** (ikon play hijau) di Android Studio.

---

## 🔌 Cara Menyambungkan Aplikasi Android dengan Backend (BFF)

Untuk menghubungkan aplikasi Android ke backend, Anda perlu menyesuaikan konfigurasi `BASE_URL` pada file [ApiService.kt](file:///c:/Users/ASUS/AndroidStudioProjects/checker/app/src/main/java/com/example/checker/data/ApiService.kt). Berikut adalah 2 skenario yang bisa Anda gunakan:

### Skenario A: Menggunakan Emulator Android (Lokal) - *Default*
Gunakan opsi ini jika Anda menjalankan backend NestJS secara lokal di komputer Anda (`npm run start:dev`) dan menguji aplikasi menggunakan **Emulator bawaan Android Studio**.
1. Jalankan backend NestJS di laptop Anda (`npm run start:dev`).
2. Buka [ApiService.kt](file:///c:/Users/ASUS/AndroidStudioProjects/checker/app/src/main/java/com/example/checker/data/ApiService.kt).
3. Pastikan baris berikut aktif (tidak dikomentari):
   ```kotlin
   private const val BASE_URL = "http://10.0.2.2:3000/api/"
   ```
   *Catatan: IP `10.0.2.2` adalah IP khusus bagi Emulator Android untuk mengakses localhost komputer host/laptop.*

### Skenario B: Menggunakan HP Fisik (Lokal)
Gunakan opsi ini jika Anda menjalankan backend NestJS di laptop Anda dan menguji aplikasi menggunakan **HP Android fisik** yang terhubung via kabel data / debugging nirkabel.
1. Hubungkan HP Android dan Laptop ke jaringan **Wi-Fi yang sama** (atau hotspot dari HP yang sama).
2. Cari tahu IP lokal laptop Anda:
   - Di Windows: Buka Command Prompt, ketik `ipconfig`, cari bagian IPv4 Address (contoh: `192.168.1.100`).
   - Di Mac/Linux: Buka Terminal, ketik `ifconfig` atau `ip route`.
3. Buka [ApiService.kt](file:///c:/Users/ASUS/AndroidStudioProjects/checker/app/src/main/java/com/example/checker/data/ApiService.kt).
4. Komentari pilihan URL emulator, lalu aktifkan dan ubah alamat IP berikut sesuai IP lokal komputer Anda:
   ```kotlin
   private const val BASE_URL = "http://192.168.1.100:3000/api/"
   ```

> [!NOTE]
> **Cleartext Traffic**: Aplikasi ini sudah dikonfigurasi dengan `android:usesCleartextTraffic="true"` di file `AndroidManifest.xml` sehingga Anda dapat melakukan pengujian menggunakan protokol HTTP (`http://`) lokal dengan lancar tanpa terblokir sistem keamanan Android.

---

### Environment Variables (Backend)
- Isi file `.env` di folder backend untuk mengaktifkan fitur:
- **Hoax Checker**: Memerlukan `GEMINI_API_KEY` atau `DEEPSEEK_API_KEY`.
- **Global Fact Check**: Memerlukan `GOOGLE_FACT_CHECK_API_KEY`.
- **Scam Scanner**: Memerlukan `VIRUSTOTAL_API_KEY`.

---

## 🛡️ Fitur Utama
- **Real-time Hoax Analysis**: Menggunakan Multi-LLM (Gemini & DeepSeek) untuk analisis narasi, bias, dan falasi logika.
- **Global Fact Check Integration**: Verifikasi silang klaim berita menggunakan API Google Fact Check secara otomatis.
- **Real-Time Notification Guard**: Memantau notifikasi masuk secara real-time. Sistem otomatis mengklasifikasikan isi notifikasi:
  - **Auto News Scan**: Menganalisis berita viral atau rumor.
  - **Auto Link Scan**: Memindai URL mencurigakan menggunakan VirusTotal.
  - **Smart Filtering**: Mengabaikan notifikasi sistem dan duplikasi untuk menghemat kuota API.
- **Cyber Scam Scanner**: Deteksi link phishing dan malware pada file APK/Dokumen secara manual.
- **OCR Hoax Scanner**: Ekstrak teks dari screenshot berita untuk dianalisis oleh AI.
- **Notification Logs & History**: Menu khusus untuk melihat riwayat notifikasi yang ditangkap dan hasil analisisnya secara mendalam.
