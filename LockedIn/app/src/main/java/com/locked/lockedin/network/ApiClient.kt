package com.locked.lockedin.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides the configured [VaultApiService] instance.
 *
 * Call [ApiClient.init] once (e.g. in Application.onCreate or MainActivity)
 * to set the base URL. The default points to the Android emulator's host
 * machine on port 8000.
 */
object ApiClient {

    /** Default base URL for the emulator (10.0.2.2 → host localhost). */
    private const val DEFAULT_BASE_URL = "http://10.50.159.189:8000/"

    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var _service: VaultApiService? = null

    /**
     * Optionally override the base URL before first use.
     * Thread-safe; rebuilds the service lazily.
     */
    fun init(baseUrl: String) {
        this.baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        _service = null  // force rebuild on next access
    }

    /** The ready-to-use API service. */
    val service: VaultApiService
        get() = _service ?: synchronized(this) {
            _service ?: buildService().also { _service = it }
        }

    private fun buildService(): VaultApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VaultApiService::class.java)
    }
}
