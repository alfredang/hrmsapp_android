package com.tertiaryinfotech.hrportal.data

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Process-wide networking singletons shared by [AuthService] and [HrmsApi]:
 * one OkHttp client riding one persistent cookie jar (the NextAuth session), one Retrofit
 * instance built on that same client, plus a lenient JSON parser (also used as Retrofit's
 * kotlinx.serialization converter). Initialised once from the Application. Mirrors the iOS app's
 * two actors sharing the system cookie store.
 */
object Net {
    /** Canonical NextAuth host (matches the provider callback URLs the backend reports). */
    const val BASE_URL = "https://hrms.tertiaryinfo.tech"

    lateinit var cookieJar: PersistentCookieJar
        private set

    lateinit var client: OkHttpClient
        private set

    lateinit var retrofit: Retrofit
        private set

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun init(context: Context) {
        if (::client.isInitialized) return
        cookieJar = PersistentCookieJar(context.applicationContext)
        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl("$BASE_URL/")
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
    }

    val hrmsApiService: HrmsApiService by lazy { retrofit.create(HrmsApiService::class.java) }
    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }

    fun url(path: String): String =
        BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
