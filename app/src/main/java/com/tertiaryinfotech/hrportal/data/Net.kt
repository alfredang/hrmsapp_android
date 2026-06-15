package com.tertiaryinfotech.hrportal.data

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Process-wide networking singletons shared by [AuthService] and [HrmsApi]:
 * one OkHttp client riding one persistent cookie jar (the NextAuth session), plus a lenient
 * JSON parser. Initialised once from the Application. Mirrors the iOS app's two actors sharing
 * the system cookie store.
 */
object Net {
    /** Canonical NextAuth host (matches the provider callback URLs the backend reports). */
    const val BASE_URL = "https://hrms.tertiaryinfotech.com"

    lateinit var cookieJar: PersistentCookieJar
        private set

    lateinit var client: OkHttpClient
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
    }

    fun url(path: String): String =
        BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
