package com.tertiaryinfotech.hrportal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

/**
 * Authenticates real employees against the Coolify-hosted HRMS backend via its NextAuth
 * (Auth.js) endpoints — no data is read from the database directly. Mirrors
 * Services/AuthService.swift.
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

    private val client get() = Net.client
    private val json: Json get() = Net.json

    // MARK: - Public API

    /** Password sign-in. Returns the authenticated user on success. */
    suspend fun signIn(email: String, password: String): SessionUser = withContext(Dispatchers.IO) {
        val token = csrfToken()
        postCallback("credentials", mapOf("csrfToken" to token, "email" to email, "password" to password))
        currentUser() ?: throw AuthException(AuthException.Kind.INVALID_CREDENTIALS,
            "Invalid email or password. Please try again.")
    }

    /** Request a one-time passcode be emailed to the employee. */
    suspend fun requestOTP(email: String): Unit = withContext(Dispatchers.IO) {
        val body = JsonObject(mapOf("email" to kotlinx.serialization.json.JsonPrimitive(email)))
        val req = Request.Builder()
            .url(Net.url("api/auth/send-otp"))
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        val resp = call(req)
        val code = resp.code
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (code == 404) throw AuthException(AuthException.Kind.NO_ACCOUNT,
            "No account found for this email. Please contact HR.")
        if (code >= 400) throw AuthException(AuthException.Kind.SERVER,
            errorMessage(text) ?: "Could not send the code. Please try again.")
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
        val req = Request.Builder()
            .url(Net.url("api/auth/session"))
            .header("Accept", "application/json")
            .get()
            .build()
        val resp = call(req)
        val text = resp.body?.string()?.trim().orEmpty()
        resp.close()
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
            val form = FormBody.Builder().add("csrfToken", token).add("json", "true").build()
            val req = Request.Builder().url(Net.url("api/auth/signout")).post(form).build()
            call(req).close()
        } catch (_: Exception) { /* best effort */ }
        Net.cookieJar.clear()
    }

    // MARK: - NextAuth plumbing

    private fun csrfToken(): String {
        val req = Request.Builder()
            .url(Net.url("api/auth/csrf"))
            .header("Accept", "application/json")
            .get()
            .build()
        val resp = call(req)
        val text = resp.body?.string().orEmpty()
        resp.close()
        val token = try {
            (json.parseToJsonElement(text) as? JsonObject)?.get("csrfToken")?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
        return token ?: throw AuthException(AuthException.Kind.SERVER,
            "Could not start sign-in. Please try again.")
    }

    /**
     * POST a NextAuth credentials callback. On bad credentials NextAuth still returns 2xx but
     * does not set a session cookie, so callers must confirm via [currentUser].
     */
    private fun postCallback(provider: String, fields: Map<String, String>) {
        val form = FormBody.Builder().apply {
            fields.forEach { (k, v) -> add(k, v) }
            add("callbackUrl", Net.BASE_URL)
            add("json", "true")
        }.build()
        val req = Request.Builder()
            .url(Net.url("api/auth/callback/$provider"))
            .header("Accept", "application/json")
            .post(form)
            .build()
        val resp = call(req)
        val code = resp.code
        resp.close()
        if (code >= 500) throw AuthException(AuthException.Kind.SERVER,
            "The server is unavailable. Please try again later.")
    }

    private fun call(req: Request) = try {
        client.newCall(req).execute()
    } catch (e: Exception) {
        throw AuthException(AuthException.Kind.NETWORK,
            "Network error. Check your connection and try again.")
    }

    private fun errorMessage(body: String): String? = try {
        (json.parseToJsonElement(body) as? JsonObject)?.get("error")?.jsonPrimitive?.content
    } catch (_: Exception) {
        null
    }

    private val JSON_MEDIA = "application/json".toMediaType()
}
