package com.example.checker.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface CheckerApiService {
    @POST("check-hoax")
    suspend fun checkHoax(
        @Body request: HoaxRequest
    ): HoaxResponse

    @POST("scan-url")
    suspend fun scanUrl(
        @Body request: ScamUrlRequest
    ): ScamResponse

    @Multipart
    @POST("scan-file")
    suspend fun scanFile(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: okhttp3.RequestBody,
        @Part("fileSize") fileSize: okhttp3.RequestBody
    ): ScamResponse
}

object NetworkClient {
    // =========================================================================
    // PILIHAN BASE URL UNTUK KONEKSI KE BACKEND (BFF)
    // =========================================================================

    // Pilihan 1: Localhost jika menggunakan Emulator Android bawaan (AKTIF DEFAULT)
    private const val BASE_URL = "http://10.0.2.2:3000/api/"
    
    // Pilihan 2: Localhost jika menggunakan HP Fisik (Uncomment & ganti dengan IP lokal komputer Anda)
    // private const val BASE_URL = "http://192.168.1.100:3000/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: CheckerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CheckerApiService::class.java)
    }
}
