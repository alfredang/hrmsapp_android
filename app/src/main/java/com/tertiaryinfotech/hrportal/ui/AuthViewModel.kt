package com.tertiaryinfotech.hrportal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.hrportal.data.AuthException
import com.tertiaryinfotech.hrportal.data.AuthService
import com.tertiaryinfotech.hrportal.data.BrandingResponse
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.SessionPrefs
import com.tertiaryinfotech.hrportal.data.SessionUser
import com.tertiaryinfotech.hrportal.data.STAFF_NOTIFICATION_TYPES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single source of truth the screens observe. Owns the auth flow state machine, drives
 * [AuthService], and persists the optionally-remembered login email via [SessionPrefs]
 * (DataStore-backed). Mirrors AuthViewModel.swift.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(private val sessionPrefs: SessionPrefs) : ViewModel() {

    enum class Step { EMAIL, PASSWORD, OTP }
    enum class Phase { LOADING, SIGNED_OUT, SIGNED_IN }

    var phase by mutableStateOf(Phase.LOADING)
        private set
    var step by mutableStateOf(Step.EMAIL)
        private set

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var otp by mutableStateOf("")

    var isWorking by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var infoMessage by mutableStateOf<String?>(null)
        private set

    var user by mutableStateOf<SessionUser?>(null)
        private set

    /** Unread count for the notification bell — staff/intern-relevant types only,
     *  mirrors `EMPLOYEE_TYPES` filtering in the web's `notification-bell.tsx`. */
    var unreadNotifications by mutableStateOf(0)
        private set

    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val all = HrmsApi.notifications()
                unreadNotifications = all.count { !it.read && it.type in STAFF_NOTIFICATION_TYPES }
            } catch (_: Exception) { /* leave last-known count on failure */ }
        }
    }

    var rememberEmail by mutableStateOf(false)

    /** Company logo/name for the login card — mirrors the web login page's `GET
     *  /api/public/branding` fetch-on-mount, shown behind a skeleton while in flight
     *  (`brandingLoading`). Unauthenticated endpoint, safe to call before sign-in. Falls back to
     *  the bundled default mark on failure — never blocks the login form from being usable. */
    var branding by mutableStateOf<BrandingResponse?>(null)
        private set
    var brandingLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            rememberEmail = sessionPrefs.rememberFlag()
            if (rememberEmail) email = sessionPrefs.rememberedEmail()
        }
        viewModelScope.launch {
            branding = try {
                HrmsApi.branding()
            } catch (_: Exception) {
                null
            }
            brandingLoading = false
        }
    }

    // MARK: - Lifecycle

    /** Restore an existing session on launch (employee stays logged in). */
    fun bootstrap() {
        viewModelScope.launch {
            val existing = try {
                AuthService.currentUser()
            } catch (_: Exception) {
                null
            }
            if (existing != null) {
                user = existing
                phase = Phase.SIGNED_IN
            } else {
                phase = Phase.SIGNED_OUT
            }
        }
    }

    // MARK: - Step transitions

    fun continueFromEmail() {
        errorMessage = null; infoMessage = null
        if (!isValidEmail(email)) { errorMessage = "Enter a valid email address."; return }
        viewModelScope.launch {
            persistRememberFlag()
            persistRememberedEmail()
            step = Step.PASSWORD
        }
    }

    fun backToEmail() {
        errorMessage = null; infoMessage = null
        password = ""; otp = ""
        step = Step.EMAIL
    }

    /** "Sign in with OTP instead" (password step) — mirrors the web's `goToEmail()`: returns to
     *  the email step so the user re-sends an OTP themselves, it does not auto-send one. */
    fun switchToOTP() {
        errorMessage = null; infoMessage = null
        step = Step.EMAIL
    }

    fun switchToPassword() {
        errorMessage = null; infoMessage = null
        otp = ""
        step = Step.PASSWORD
    }

    fun setRemember(value: Boolean) {
        rememberEmail = value
        viewModelScope.launch {
            persistRememberFlag()
            persistRememberedEmail()
        }
    }

    // MARK: - Actions

    fun signInWithPassword() {
        if (isWorking) return
        errorMessage = null; infoMessage = null
        if (password.isEmpty()) { errorMessage = "Enter your password."; return }
        isWorking = true
        viewModelScope.launch {
            try {
                val u = AuthService.signIn(normalizedEmail(), password)
                finishSignIn(u)
            } catch (e: Exception) {
                errorMessage = (e as? AuthException)?.message ?: "Sign-in failed."
            } finally {
                isWorking = false
            }
        }
    }

    /** Initial "Send OTP" from the email step — uses the server's own success copy, mirrors
     *  the web's `handleSendOtp`. */
    fun sendOTP() {
        if (isWorking) return
        errorMessage = null; infoMessage = null
        if (!isValidEmail(email)) { errorMessage = "Enter a valid email address."; return }
        isWorking = true
        viewModelScope.launch {
            try {
                val serverMessage = AuthService.requestOTP(normalizedEmail())
                persistRememberedEmail()
                step = Step.OTP
                infoMessage = serverMessage
            } catch (e: Exception) {
                errorMessage = (e as? AuthException)?.message ?: "Could not send the code."
            } finally {
                isWorking = false
            }
        }
    }

    var isResending by mutableStateOf(false)
        private set

    /** "Resend OTP" link on the OTP step — mirrors the web's `handleResendOtp`: distinct client
     *  copy from the initial send, and the button stays disabled for a flat 5s regardless of how
     *  quickly the request actually completes (matches web's `setTimeout(..., 5000)`). */
    fun resendOTP() {
        if (isResending) return
        errorMessage = null; infoMessage = null
        isResending = true
        viewModelScope.launch {
            try {
                AuthService.requestOTP(normalizedEmail())
                infoMessage = "A new OTP has been sent to your email."
            } catch (e: Exception) {
                errorMessage = (e as? AuthException)?.message ?: "Failed to resend OTP."
            }
            delay(5000)
            isResending = false
        }
    }

    fun verifyOTP() {
        if (isWorking) return
        errorMessage = null
        if (otp.length < 4) { errorMessage = "Enter the code from your email."; return }
        isWorking = true
        viewModelScope.launch {
            try {
                val u = AuthService.verifyOTP(normalizedEmail(), otp)
                finishSignIn(u)
            } catch (_: Exception) {
                errorMessage = "That code is incorrect or expired. Request a new one."
            } finally {
                isWorking = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            AuthService.signOut()
            user = null
            password = ""; otp = ""
            step = Step.EMAIL
            phase = Phase.SIGNED_OUT
        }
    }

    // MARK: - Helpers

    private fun finishSignIn(u: SessionUser) {
        user = u
        password = ""; otp = ""
        infoMessage = null; errorMessage = null
        phase = Phase.SIGNED_IN
    }

    private fun normalizedEmail(): String = email.lowercase().trim()

    private suspend fun persistRememberFlag() {
        sessionPrefs.setRememberFlag(rememberEmail)
    }

    private suspend fun persistRememberedEmail() {
        sessionPrefs.setRememberedEmail(if (rememberEmail) normalizedEmail() else null)
    }

    private fun isValidEmail(s: String): Boolean {
        val t = s.trim()
        return t.contains("@") && t.contains(".") && t.length >= 5
    }
}
