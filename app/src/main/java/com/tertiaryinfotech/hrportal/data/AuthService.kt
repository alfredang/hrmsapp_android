package com.tertiaryinfotech.hrportal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

/**
 * Authenticates real employees against the Coolify-hosted HRMS backend via its NextAuth
 * (Auth.js) endpoints — no data is read from the database directly. Backed by Retrofit
 * (`AuthApiService`, `data/Net.kt`). Mirrors Services/AuthService.swift.
 *
 * NextAuth credential sign-in is a small dance:
 *   1. GET  /api/auth/csrf                → csrfToken (+ csrf cookie)
 *   2. POST /api/auth/callback/{provider} → sets the session cookie on success
 *   3. GET  /api/auth/session             → the signed-in user (or null)
 * The shared [PersistentCookieJar] carries the session between calls and across launches.
 */
class AuthException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { INVALID_CREDENTIALS, NO_ACCOUNT, NETWORK, SERVER }
}

object AuthService {

    private val service get() = Net.authApiService
    private val json: Json get() = Net.json

    // MARK: - Public API

    /** Password sign-in. Returns the authenticated user on success. */
    suspend fun signIn(email: String, password: String): SessionUser = withContext(Dispatchers.IO) {
        val token = csrfToken()
        postCallback("credentials", mapOf("csrfToken" to token, "email" to email, "password" to password))
        currentUser() ?: throw AuthException(AuthException.Kind.INVALID_CREDENTIALS,
            "Invalid email or password. Please try again.")
    }

    /** Request a one-time passcode be emailed to the employee. Returns the server's own success
     *  copy (e.g. "OTP has been sent to your email address") so the UI shows real server text
     *  rather than a client-guessed message. */
    suspend fun requestOTP(email: String): String = withContext(Dispatchers.IO) {
        val resp = call { service.sendOtp(SendOtpBody(email)) }
        val code = resp.code()
        if (code == 404) throw AuthException(AuthException.Kind.NO_ACCOUNT,
            "No account found for this email. Please contact HR.")
        if (code >= 400) throw AuthException(AuthException.Kind.SERVER,
            errorMessage(resp) ?: "Could not send the code. Please try again.")
        resp.body()?.message ?: "OTP sent to your email."
    }

    /** Verify the emailed OTP and sign in. */
    suspend fun verifyOTP(email: String, otp: String): SessionUser = withContext(Dispatchers.IO) {
        val token = csrfToken()
        postCallback("otp", mapOf("csrfToken" to token, "email" to email, "otp" to otp))
        currentUser() ?: throw AuthException(AuthException.Kind.INVALID_CREDENTIALS,
            "That code is incorrect or expired. Request a new one.")
    }

    /** The current session user, if a valid session cookie is present. */
    suspend fun currentUser(): SessionUser? = withContext(Dispatchers.IO) {
        val resp = call { service.session() }
        val text = resp.body()?.string()?.trim().orEmpty()
        if (text.isEmpty() || text == "null") return@withContext null
        try {
            json.decodeFromString(SessionResponse.serializer(), text).user
        } catch (_: Exception) {
            null
        }
    }

    /** Clear the server session and local cookies. */
    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            val token = csrfToken()
            call { service.signOut(mapOf("csrfToken" to token, "json" to "true")) }
        } catch (_: Exception) { /* best effort */ }
        Net.cookieJar.clear()
    }

    // MARK: - NextAuth plumbing

    private suspend fun csrfToken(): String {
        val resp = call { service.csrf() }
        return resp.body()?.csrfToken ?: throw AuthException(AuthException.Kind.SERVER,
            "Could not start sign-in. Please try again.")
    }

    /**
     * POST a NextAuth credentials callback. On bad credentials NextAuth still returns 2xx but
     * does not set a session cookie, so callers must confirm via [currentUser].
     */
    private suspend fun postCallback(provider: String, fields: Map<String, String>) {
        val form = fields + mapOf("callbackUrl" to Net.BASE_URL, "json" to "true")
        val resp = call { service.callback(provider, form) }
        if (resp.code() >= 500) throw AuthException(AuthException.Kind.SERVER,
            "The server is unavailable. Please try again later.")
    }

    private suspend fun <T> call(req: suspend () -> Response<T>): Response<T> = try {
        req()
    } catch (e: Exception) {
        throw AuthException(AuthException.Kind.NETWORK,
            "Network error. Check your connection and try again.")
    }

    private fun errorMessage(resp: Response<*>): String? = try {
        val body = resp.errorBody()?.string().orEmpty()
        (json.parseToJsonElement(body) as? JsonObject)?.get("error")?.jsonPrimitive?.content
    } catch (_: Exception) {
        null
    }
}
