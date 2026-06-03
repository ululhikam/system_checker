# 🛡️ Cyber Shield: AI-Powered Security Guard

Cyber Shield adalah aplikasi keamanan Android yang dirancang untuk melindungi pengguna dari ancaman **Phishing** dan **Hoaks** secara real-time. Aplikasi ini memantau notifikasi yang masuk dan menggunakan kecerdasan buatan (AI) untuk menganalisis apakah sebuah pesan mengandung tautan berbahaya atau informasi palsu.

## ✨ Fitur Utama
- **🔍 Auto Notification Scanner**: Memindai setiap notifikasi masuk (WhatsApp, SMS, dll) secara otomatis.
- **🤖 AI Hoax Checker**: Analisis mendalam narasi berita menggunakan engine Gemini 2.5 Flash atau DeepSeek V3.
- **🔗 Scam & Phishing Scanner**: Deteksi link berbahaya melalui database VirusTotal dan DNS tracking.
- **📊 Security Dashboard**: Statistik pemindaian real-time dan log riwayat keamanan.
- **🛡️ Real-time Alerts**: Notifikasi peringatan instan jika terdeteksi ancaman risiko tinggi.

---

## 🏗️ Struktur Proyek
- `/app`: Source code aplikasi Android (Kotlin + Jetpack Compose).
- `/backend`: Source code BFF (Backend For Frontend) menggunakan NestJS.

---

## 🚀 Panduan Instalasi & Menjalankan

### 1. Persiapan Backend (Laptop)
Backend berfungsi sebagai otak yang menghubungkan aplikasi ke engine AI dan API Keamanan.

1. Pastikan sudah menginstal [Node.js](https://nodejs.org/).
2. Buka Terminal/CMD, arahkan ke folder backend:
   ```bash
   cd backend
   npm install
   ```
3. Jalankan server:
   ```bash
   npm run start
   ```
4. Server akan berjalan di `http://localhost:3000`.

### 2. Persiapan Android App (Android Studio)
1. Buka folder `system_checker` menggunakan **Android Studio**.
2. **Konfigurasi Koneksi (PENTING)**:
   Agar HP Android bisa terhubung ke backend di laptop:
   - Pastikan HP dan Laptop berada di **satu hotspot/Wi-Fi yang sama**.
   - Cek IP Laptop Anda di CMD: `ipconfig` (lihat bagian IPv4 Address, misal: `192.168.43.15`).
   - Buka file: `app/src/main/java/com/example/checker/data/ApiService.kt`.
   - Ubah `BASE_URL` sesuai IP laptop Anda:
     ```kotlin
     private const val BASE_URL = "http://192.168.43.15:3000/api/"
     ```
3. Klik **Sync Project with Gradle Files**.
4. Jalankan aplikasi ke HP fisik atau Emulator.

---

## 🛠️ Cara Menggunakan Fitur Scanner
1. Buka aplikasi Cyber Shield.
2. Di Dashboard, aktifkan switch **"Auto Notification Scanner"**.
3. Anda akan diarahkan ke pengaturan Android **"Notification Access"**.
4. Cari **"Cyber Shield Notification Guard"** dan aktifkan izinnya.
5. **Selesai!** Sekarang coba kirim link (misal: `http://google.com`) melalui WhatsApp ke HP tersebut. Aplikasi akan otomatis memindai dan memberikan laporan di dashboard.

---

## ⚠️ Troubleshooting (Kendala Koneksi)
Jika muncul pesan **"Gagal konek ke backend"**:
1. Pastikan IP di `ApiService.kt` sudah benar dan menggunakan awalan `http://`.
2. Matikan sementara **Windows Firewall** di laptop agar tidak memblokir koneksi dari HP.
3. Cek apakah backend di laptop masih menyala (terminal tidak error).
4. Tes melalui browser HP dengan mengetik: `http://IP-LAPTOP-ANDA:3000/api/history`. Jika muncul data, berarti koneksi aman.

---

## 🧰 Teknologi yang Digunakan
- **Frontend**: Kotlin, Jetpack Compose, Retrofit2, ML Kit (OCR), Coroutines.
- **Backend**: NestJS, TypeScript, Axios, VirusTotal API.
- **AI Engine**: Google Gemini API & DeepSeek.

---
© 2024 Cyber Shield Security Team.
