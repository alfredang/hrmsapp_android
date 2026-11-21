package com.tertiaryinfotech.hrportal.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tertiaryinfotech.hrportal.BuildConfig
import com.tertiaryinfotech.hrportal.data.AppVersion
import com.tertiaryinfotech.hrportal.data.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Request

/**
 * Force update (Alfred, 22 Nov 2026; iOS `UpdateGate`). The app is blocked by a full-screen "Update required"
 * when the version live on Play is newer than this one: from `GET /api/public/app-version` at launch and on
 * resume, or from any HRMS answer of 426 Upgrade Required. A failed check never blocks.
 */
object UpdateGate {
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.tertiaryinfotech.hrportal"

    var required by mutableStateOf(false)
        private set
    var storeUrl by mutableStateOf(PLAY_URL)
        private set

    private val main = Handler(Looper.getMainLooper())

    fun init() {
        Net.onUpgradeRequired = { url -> main.post { block(url) } }
    }

    private fun block(url: String?) {
        if (!url.isNullOrBlank()) storeUrl = url
        required = true
    }

    /** Asks the server which version is live on Play; blocks when this one is older. */
    suspend fun check() {
        val live = withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(Net.url("/api/public/app-version")).build()
                Net.client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val android = (Net.json.parseToJsonElement(resp.body?.string().orEmpty()) as? JsonObject)
                        ?.get("android") as? JsonObject ?: return@use null
                    val version = (android["liveVersion"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                    val url = (android["storeUrl"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                    version to url
                }
            } catch (_: Exception) {
                null
            }
        } ?: return
        if (AppVersion.mustUpdate(BuildConfig.VERSION_NAME, live.first)) block(live.second)
    }
}
