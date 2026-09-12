package com.tertiaryinfotech.hrportal.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.tertiaryinfotech.hrportal.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

/**
 * Google Sign-In, built on Chrome Custom Tabs — no Google Play Services SDK, keeping the app on
 * the same tiny dependency set. The Android counterpart of the iOS
 * `Services/GoogleSignInService.swift`, and deliberately the same flow:
 *
 *   1. Open Google's consent screen in a Custom Tab with a PKCE challenge.
 *   2. Google redirects back to our reversed-client-id scheme with an authorization code.
 *   3. Exchange the code (+ verifier) at Google's token endpoint for an `id_token`.
 *   4. POST that `id_token` to `/api/auth/google-mobile`, which verifies it with Google, checks
 *      the audience against GOOGLE_CLIENT_ID / GOOGLE_IOS_CLIENT_ID / GOOGLE_ANDROID_CLIENT_ID,
 *      blocks INACTIVE employees, and sets the same NextAuth session cookie every other sign-in
 *      path uses.
 *
 * Step 4 is why this is safe client-side: the app holds no secret and never trusts its own view
 * of who the user is — the backend independently verifies the token with Google.
 *
 * Note the deliberate asymmetry with iOS: a Google **Android** client authenticates by package
 * name + signing certificate rather than a secret, so it issues no `client_secret` and the token
 * exchange below correctly sends none.
 */
class GoogleAuthException(val kind: Kind, message: String?) : Exception(message) {
    enum class Kind { NOT_CONFIGURED, CANCELLED, GOOGLE, SERVER }
}

object GoogleSignInService {

    /** Public value, not a secret — see the class note. Empty in a build that never configured it. */
    private val clientId: String get() = BuildConfig.GOOGLE_CLIENT_ID.trim()

    /** Whether this build can offer Google sign-in at all (drives the button's visibility). */
    val isConfigured: Boolean get() = clientId.isNotEmpty()

    /** Google's convention for native clients: the redirect URI is the client id reversed. */
    private val redirectUri: String
        get() = clientId.split(".").reversed().joinToString(".") + ":/oauth2redirect"

    /** Set while a sign-in is in flight; completed by [onRedirect] when the browser comes back. */
    @Volatile
    private var pending: CompletableDeferred<Uri>? = null

    @Volatile
    private var expectedState: String? = null

    /**
     * Launch the consent screen. Suspends until [onRedirect] delivers the callback (or the user
     * backs out and [onCancelled] fires), then completes the exchange and returns the HRMS user.
     */
    suspend fun signIn(context: Context): SessionUser {
        if (!isConfigured) {
            throw GoogleAuthException(GoogleAuthException.Kind.NOT_CONFIGURED,
                "Google sign-in isn't configured in this build.")
        }

        val verifier = randomVerifier()
        val state = randomVerifier()
        val deferred = CompletableDeferred<Uri>()
        pending = deferred
        expectedState = state

        try {
            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "openid email profile")
                .appendQueryParameter("code_challenge", codeChallenge(verifier))
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("state", state)
                // Company workspace accounts first; personal accounts still resolve against the
                // employee directory server-side.
                .appendQueryParameter("hd", "tertiaryinfotech.com")
                .appendQueryParameter("prompt", "select_account")
                .build()

            try {
                CustomTabsIntent.Builder().build().apply {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.launchUrl(context, authUrl)
            } catch (_: Exception) {
                throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE,
                    "Google sign-in could not be opened. Please try again.")
            }

            val callback = deferred.await()
            val code = authorizationCode(callback, state)
            val idToken = exchange(code, verifier)
            return establishSession(idToken)
        } finally {
            pending = null
            expectedState = null
        }
    }

    /** Called by [GoogleAuthRedirectActivity] when Google redirects back to our scheme. */
    fun onRedirect(uri: Uri) {
        pending?.complete(uri)
    }

    /**
     * Called when the user dismissed the Custom Tab without completing sign-in. Without this the
     * coroutine would hang forever on a back-press, leaving the login button spinning.
     */
    fun onCancelled() {
        pending?.completeExceptionally(
            GoogleAuthException(GoogleAuthException.Kind.CANCELLED, null),
        )
    }

    /** Pull the `code` out of the redirect, rejecting a mismatched `state` (CSRF guard). */
    private fun authorizationCode(uri: Uri, expected: String): String {
        uri.getQueryParameter("error")?.let { err ->
            if (err == "access_denied") throw GoogleAuthException(GoogleAuthException.Kind.CANCELLED, null)
            throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE, "Google sign-in failed ($err).")
        }
        if (uri.getQueryParameter("state") != expected) {
            throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE,
                "Google sign-in could not be verified. Please try again.")
        }
        return uri.getQueryParameter("code")?.takeIf { it.isNotEmpty() }
            ?: throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE,
                "Google did not return a sign-in code.")
    }

    /** Trade the authorization code + PKCE verifier for an id_token. No client_secret: see above. */
    private suspend fun exchange(code: String, verifier: String): String = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("code", code)
            .add("code_verifier", verifier)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", redirectUri)
            .build()
        val req = Request.Builder().url("https://oauth2.googleapis.com/token").post(body).build()

        val resp = try {
            Net.client.newCall(req).execute()
        } catch (_: Exception) {
            throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE,
                "Network error. Check your connection and try again.")
        }
        resp.use {
            val text = it.body?.string().orEmpty()
            val parsed = runCatching { Net.json.decodeFromString<GoogleTokenResponse>(text) }.getOrNull()
            val idToken = parsed?.idToken
            if (idToken.isNullOrEmpty()) {
                val detail = parsed?.errorDescription ?: parsed?.error
                throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE,
                    detail ?: "Google rejected the sign-in (HTTP ${it.code}).")
            }
            idToken
        }
    }

    /** POST the Google id_token to the backend, which sets the NextAuth session cookie. */
    private suspend fun establishSession(idToken: String): SessionUser = withContext(Dispatchers.IO) {
        val resp = try {
            Net.authApiService.googleMobile(GoogleMobileBody(idToken))
        } catch (_: Exception) {
            throw GoogleAuthException(GoogleAuthException.Kind.GOOGLE,
                "Network error. Check your connection and try again.")
        }

        if (!resp.isSuccessful) {
            val msg = resp.errorBody()?.string()?.let { body ->
                runCatching { Net.json.decodeFromString<ApiErrorBody>(body).error }.getOrNull()
            }
            val fallback = when (resp.code()) {
                401 -> "That Google account isn't allowed to sign in."
                403 -> "This account is inactive. Please contact HR."
                else -> "Sign-in failed. Please try again."
            }
            throw GoogleAuthException(GoogleAuthException.Kind.SERVER, msg ?: fallback)
        }

        // The endpoint set the NextAuth cookie on the shared cookie jar, so the canonical session
        // read is the same one password/OTP sign-in uses.
        AuthService.currentUser()
            ?: throw GoogleAuthException(GoogleAuthException.Kind.SERVER,
                "Signed in with Google, but the session did not start. Please try again.")
    }

    // MARK: - PKCE (RFC 7636)

    /** 32 random bytes, base64url-encoded. */
    private fun randomVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return base64Url(bytes)
    }

    /** base64url(SHA-256(verifier)). */
    private fun codeChallenge(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
