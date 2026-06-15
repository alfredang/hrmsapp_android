package com.tertiaryinfotech.hrportal.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.hrportal.data.AuthException
import com.tertiaryinfotech.hrportal.data.AuthService
import com.tertiaryinfotech.hrportal.data.SessionUser
import kotlinx.coroutines.launch

/**
 * The single source of truth the screens observe. Owns the auth flow state machine, drives
 * [AuthService], and persists the optionally-remembered login email. Mirrors AuthViewModel.swift.
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    enum class Step { EMAIL, PASSWORD, OTP }
    enum class Phase { LOADING, SIGNED_OUT, SIGNED_IN }

    private val prefs = app.getSharedPreferences("hrms_prefs", Application.MODE_PRIVATE)

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

    var rememberEmail by mutableStateOf(prefs.getBoolean(KEY_REMEMBER_FLAG, false))

    init {
        if (rememberEmail) email = prefs.getString(KEY_REMEMBERED_EMAIL, "") ?: ""
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
        persistRememberFlag()
        persistRememberedEmail()
        step = Step.PASSWORD
    }

    fun backToEmail() {
        errorMessage = null; infoMessage = null
        password = ""; otp = ""
        step = Step.EMAIL
    }

    fun switchToOTP() {
        errorMessage = null
        sendOTP()
    }

    fun switchToPassword() {
        errorMessage = null; infoMessage = null
        otp = ""
        step = Step.PASSWORD
    }

    fun setRemember(value: Boolean) {
        rememberEmail = value
        persistRememberFlag()
        persistRememberedEmail()
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

    fun sendOTP() {
        if (isWorking) return
        errorMessage = null; infoMessage = null
        if (!isValidEmail(email)) { errorMessage = "Enter a valid email address."; return }
        isWorking = true
        viewModelScope.launch {
            try {
                AuthService.requestOTP(normalizedEmail())
                persistRememberedEmail()
                step = Step.OTP
                infoMessage = "We emailed a 6-digit code to ${normalizedEmail()}."
            } catch (e: Exception) {
                errorMessage = (e as? AuthException)?.message ?: "Could not send the code."
            } finally {
                isWorking = false
            }
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

    private fun persistRememberFlag() {
        prefs.edit().putBoolean(KEY_REMEMBER_FLAG, rememberEmail).apply()
    }

    private fun persistRememberedEmail() {
        if (rememberEmail) prefs.edit().putString(KEY_REMEMBERED_EMAIL, normalizedEmail()).apply()
        else prefs.edit().remove(KEY_REMEMBERED_EMAIL).apply()
    }

    private fun isValidEmail(s: String): Boolean {
        val t = s.trim()
        return t.contains("@") && t.contains(".") && t.length >= 5
    }

    private companion object {
        const val KEY_REMEMBER_FLAG = "hrms_remember_email"
        const val KEY_REMEMBERED_EMAIL = "hrms_remembered_email"
    }
}
