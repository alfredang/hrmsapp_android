package com.tertiaryinfotech.hrportal.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tertiaryinfotech.hrportal.data.AclpFilter
import com.tertiaryinfotech.hrportal.data.ViewAsMode

/**
 * The View As choice (More → View As, Trainer Admin holders only), saved on this device in the
 * SharedPreferences file `hrms_view_as`, never on the account. Reset to My view on sign-out, on a fresh
 * sign-in and when the role is gone. Compose snapshot state, so the tab bar, Home and More repaint at once.
 */
object ViewAs {
    private const val PREFS = "hrms_view_as"
    private const val KEY_MODE = "mode"
    private var prefs: SharedPreferences? = null

    var mode by mutableStateOf(ViewAsMode.MINE)
        private set

    /** A directory filter set by a Home tile, applied (and cleared) by the trainer directory when it shows. */
    var pendingDirectoryFilter by mutableStateOf<AclpFilter?>(null)

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        mode = ViewAsMode.fromKey(p.getString(KEY_MODE, null))
    }

    fun select(value: ViewAsMode) {
        mode = value
        prefs?.edit()?.putString(KEY_MODE, value.key)?.apply()
    }

    fun reset() = select(ViewAsMode.MINE)
}
