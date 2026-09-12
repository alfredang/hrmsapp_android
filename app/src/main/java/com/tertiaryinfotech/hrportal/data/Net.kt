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
    /** Canonical NextAuth host (matches the provider callback URLs the backend reports,
     *  and the iOS app's `AuthService.baseURL`). The `hrms.tertiaryinfo.tech` alias resolves
     *  to the same Coolify deployment, but NextAuth reports its callbacks on this host — so
     *  signing in against the alias hands back cookies scoped to a domain the app then can't
     *  match. Always use the canonical host. */
    const val BASE_URL = "https://hrms.tertiaryinfotech.com"

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

    /** Resolves a host-relative API/asset path (e.g. `/branding/company-logo.png`) against
     *  [BASE_URL]. Already-absolute URLs (e.g. a Google-hosted avatar) pass through unchanged —
     *  the backend mixes both shapes across different fields, so this must handle either. */
    fun url(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
