package com.tertiaryinfotech.hrportal.ui.theme

import android.content.Context

/** Persists the Dark/Light choice made on the Profile screen's Theme card across app launches. */
object ThemePrefs {
    private const val PREFS = "theme_prefs"
    private const val KEY_DARK = "dark_mode"

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
    }
}
