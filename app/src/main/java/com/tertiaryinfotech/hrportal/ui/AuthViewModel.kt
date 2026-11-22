package com.tertiaryinfotech.hrportal.ui

import com.tertiaryinfotech.hrportal.data.ViewAsRules
import com.tertiaryinfotech.hrportal.data.LeaveRules
import com.tertiaryinfotech.hrportal.data.LoginDefaults
import com.tertiaryinfotech.hrportal.data.ShownRole
import com.tertiaryinfotech.hrportal.data.ViewAsMode
import com.tertiaryinfotech.hrportal.ui.theme.Appearance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.hrportal.data.AuthException
import android.content.Context
import com.tertiaryinfotech.hrportal.data.AuthService
import com.tertiaryinfotech.hrportal.data.GoogleAuthException
import com.tertiaryinfotech.hrportal.data.GoogleSignInService
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.SessionPrefs
import com.tertiaryinfotech.hrportal.data.SessionUser
import com.tertiaryinfotech.hrportal.data.isVisibleNotification
import com.tertiaryinfotech.hrportal.push.Push
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

    /** Unread count for the notification bell — every alert addressed to this user (the server
     *  only returns the user's own), so it matches what push delivered. */
    var unreadNotifications by mutableStateOf(0)
        private set

    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val all = HrmsApi.notifications()
                unreadNotifications = all.count { !it.read && isVisibleNotification(it.type) }
            } catch (_: Exception) { /* leave last-known count on failure */ }
        }
    }

    /** From `/api/mobile/summary`: TRAINER_ADMIN holders get the Trainers tab. Enforced server-side
     *  too (403), so this only decides whether the tab is shown. */
    var canManageTrainers by mutableStateOf(false)
        private set

    /** True once `/api/mobile/summary` has answered this session, so [canManageTrainers] is real. */
    var capabilitiesKnown by mutableStateOf(false)
        private set

    /** The Trainer Admin view (More → View As) is in force for this user. */
    val isTrainerView: Boolean
        get() = ViewAsRules.isTrainerView(ViewAs.mode, canManageTrainers, capabilitiesKnown)

    /** Approver roles (web `hasAdminAccess`), whatever the view. */
    val isApprover: Boolean get() = LeaveRules.isApprover(user?.roles, user?.role)

    /** Previewing other roles is Alfred's only (iOS `canViewAs`, web view-as-policy). */
    val canViewAs: Boolean get() = ViewAsRules.canViewAs(user?.email)

    /**
     * The views this person may pick: My view; Staff / Intern / Accountant for an approver allowed to preview;
     * Trainer Admin for holders. Anyone else gets only My view (no switch).
     */
    val availableViews: List<ViewAsMode> get() = ViewAsRules.available(isApprover, canManageTrainers, canViewAs)

    /** What the screens show for the current view; forms keep the real roles. */
    val shown: ShownRole get() = ViewAsRules.shown(ViewAs.mode, user?.roles, user?.role, isTrainerView, canViewAs)

    /** An approver previewing another role (View As): clocking in is off. */
    val isRolePreview: Boolean get() = ViewAsRules.isRolePreview(ViewAs.mode, isApprover, canViewAs)

    fun refreshCapabilities() {
        viewModelScope.launch {
            try {
                canManageTrainers = HrmsApi.summary().canManageTrainers
                capabilitiesKnown = true
                // A view no longer allowed: back to My view.
                val keep = ViewAsRules.afterCapabilities(ViewAs.mode, canManageTrainers, isApprover, canViewAs)
                if (keep != ViewAs.mode) ViewAs.select(keep)
            } catch (_: Exception) { /* keep last-known value on failure */ }
        }
    }

    var rememberEmail by mutableStateOf(false)

    init {
        viewModelScope.launch {
            rememberEmail = sessionPrefs.rememberFlag()
            if (rememberEmail) email = sessionPrefs.rememberedEmail()
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
                Appearance.applyAccountDefault(existing.email)
                phase = Phase.SIGNED_IN
                Push.registerAfterSignIn()
            } else {
                ViewAs.reset()
                phase = Phase.SIGNED_OUT
            }
        }
    }

    // MARK: - Step transitions

    /**
     * The default sign-in for this email (Alfred, 22 Nov 2026; iOS `usesGoogleByDefault`): a company address signs in
     * with Google (when this build has a Google client id), any other address (interns, part-time staff) gets a
     * one-time code. Password is a link.
     */
    val usesGoogleByDefault: Boolean get() = LoginDefaults.usesGoogle(email, googleSignInAvailable)

    /** The first screen's primary action (and its keyboard action): Google for a company email, else a code. */
    fun continueFromEmail(context: Context) {
        errorMessage = null; infoMessage = null
        if (!isValidEmail(email)) { errorMessage = "Enter a valid email address."; return }
        if (usesGoogleByDefault) {
            viewModelScope.launch { persistRememberFlag(); persistRememberedEmail() }
            signInWithGoogle(context)
        } else {
            sendOTP()
        }
    }

    /** "Use password instead" from the first screen. */
    fun usePassword() {
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

    /** "Use a one-time code" on the password step sends the code straight away, as on iOS. */
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

    /** Whether this build ships a Google OAuth client id — drives the button's visibility, the
     *  same way the iOS app hides its Google button when GIDClientID is unset. */
    val googleSignInAvailable: Boolean get() = GoogleSignInService.isConfigured

    /**
     * Google sign-in. Needs an Activity [context] to launch the Custom Tab, so the screen passes
     * its own rather than the Application context held by this AndroidViewModel.
     */
    fun signInWithGoogle(context: Context) {
        if (isWorking) return
        errorMessage = null; infoMessage = null
        isWorking = true
        viewModelScope.launch {
            try {
                // The typed company email, else the one last used with Google here (one-tap hint).
                val hint = email.takeIf { GoogleSignInService.isCompanyEmail(it) } ?: sessionPrefs.googleCompanyEmail()
                val user = GoogleSignInService.signIn(context, hint)
                user.email?.takeIf { GoogleSignInService.isCompanyEmail(it) }?.let { sessionPrefs.setGoogleCompanyEmail(it.lowercase()) }
                finishSignIn(user)
            } catch (e: GoogleAuthException) {
                // A user who backed out of the consent screen isn't an error worth a banner —
                // same judgement the iOS service makes for .cancelled.
                if (e.kind != GoogleAuthException.Kind.CANCELLED) {
                    errorMessage = e.message ?: "Google sign-in failed."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Google sign-in failed."
            } finally {
                isWorking = false
            }
        }
    }

    /** Called when the login screen resumes with no redirect delivered — the user dismissed the
     *  Custom Tab, so release the suspended sign-in instead of spinning forever. */
    fun onGoogleSignInDismissed() {
        if (isWorking) GoogleSignInService.onCancelled()
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
            Push.unregisterBeforeSignOut()
            AuthService.signOut()
            user = null
            canManageTrainers = false
            capabilitiesKnown = false
            ViewAs.reset()
            password = ""; otp = ""
            step = Step.EMAIL
            phase = Phase.SIGNED_OUT
        }
    }

    // MARK: - Helpers

    private fun finishSignIn(u: SessionUser) {
        user = u
        ViewAs.reset() // a fresh sign-in starts on My view
        capabilitiesKnown = false
        password = ""; otp = ""
        infoMessage = null; errorMessage = null
        Appearance.applyAccountDefault(u.email)
        phase = Phase.SIGNED_IN
        Push.registerAfterSignIn()
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
