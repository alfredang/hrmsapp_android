package com.tertiaryinfotech.hrportal.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The three choices in My Profile -> Appearance. */
enum class AppearanceMode(val key: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    SYSTEM("system", "System");

    companion object {
        fun fromKey(key: String?): AppearanceMode? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Appearance (Alfred, 19 Nov 2026): Light, Dark or System, chosen in My Profile. The choice is
 * saved on this device (SharedPreferences), never on the account. Nothing saved = System, except
 * the accounts in [LIGHT_DEFAULT_EMAILS], which start on Light.
 *
 * [mode] and [systemDark] are Compose snapshot state, and every `Brand.*` colour reads [isDark],
 * so a change repaints the whole app at once with no restart.
 */
object Appearance {
    private const val PREFS = "hrms_appearance"
    private const val KEY_MODE = "mode"

    /** These accounts start on Light instead of System; they can still change it. */
    val LIGHT_DEFAULT_EMAILS = listOf("angch@tertiaryinfotech.com", "tansc@tertiaryinfotech.com")

    private var prefs: SharedPreferences? = null

    var mode by mutableStateOf(AppearanceMode.SYSTEM)
        private set

    /** The device's own light / dark setting; kept current by MainActivity. */
    var systemDark by mutableStateOf(false)

    val isDark: Boolean
        get() = when (mode) {
            AppearanceMode.LIGHT -> false
            AppearanceMode.DARK -> true
            AppearanceMode.SYSTEM -> systemDark
        }

    /** Read the saved choice synchronously (Application.onCreate), before the first frame. */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        mode = AppearanceMode.fromKey(p.getString(KEY_MODE, null)) ?: AppearanceMode.SYSTEM
        readSystem(context.resources.configuration)
    }

    fun readSystem(configuration: Configuration) {
        systemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun select(value: AppearanceMode) {
        mode = value
        prefs?.edit()?.putString(KEY_MODE, value.key)?.apply()
    }

    /** After sign-in or session restore: apply the account's starting choice if none is saved. */
    fun applyAccountDefault(email: String?) {
        val p = prefs ?: return
        if (AppearanceMode.fromKey(p.getString(KEY_MODE, null)) != null) return
        val start = defaultFor(email)
        if (start != AppearanceMode.SYSTEM) select(start)
    }

    fun defaultFor(email: String?): AppearanceMode =
        if (email != null && email.trim().lowercase() in LIGHT_DEFAULT_EMAILS) AppearanceMode.LIGHT
        else AppearanceMode.SYSTEM
}
