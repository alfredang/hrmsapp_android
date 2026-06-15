package com.tertiaryinfotech.hrportal.data

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * A cookie jar that persists the NextAuth session cookie across launches, mirroring the
 * iOS app's shared HTTPCookieStorage. Both [AuthService] and [HrmsApi] share one instance,
 * so the session established at login rides every subsequent API call exactly as a browser would.
 *
 * Backed by SharedPreferences. Only non-expired cookies are kept; clearing on sign-out wipes
 * the store. Serialisation is a simple line format — enough for the handful of auth cookies.
 */
class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs = context.getSharedPreferences("hrms_cookies", Context.MODE_PRIVATE)
    private val cache = ConcurrentHashMap<String, Cookie>()

    init {
        load()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        var changed = false
        for (cookie in cookies) {
            val key = keyOf(cookie)
            if (cookie.expiresAt < System.currentTimeMillis() && !cookie.persistent) {
                // session cookie with no explicit expiry: keep in memory, still persist
            }
            cache[key] = cookie
            changed = true
        }
        if (changed) persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val valid = mutableListOf<Cookie>()
        val expired = mutableListOf<String>()
        for ((key, cookie) in cache) {
            if (cookie.expiresAt < now) {
                expired.add(key)
            } else if (cookie.matches(url)) {
                valid.add(cookie)
            }
        }
        if (expired.isNotEmpty()) {
            expired.forEach { cache.remove(it) }
            persist()
        }
        return valid
    }

    @Synchronized
    fun clear() {
        cache.clear()
        prefs.edit().clear().apply()
    }

    // MARK: - Persistence

    private fun keyOf(c: Cookie): String = "${c.domain}|${c.path}|${c.name}"

    private fun persist() {
        val serialized = cache.values.mapNotNull { encode(it) }.toSet()
        prefs.edit().putStringSet("cookies", serialized).apply()
    }

    private fun load() {
        val stored = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        val now = System.currentTimeMillis()
        for (line in stored) {
            val cookie = decode(line) ?: continue
            if (cookie.expiresAt >= now) cache[keyOf(cookie)] = cookie
        }
    }

    // name\tvalue\tdomain\tpath\texpiresAt\tsecure\thttpOnly\thostOnly
    private fun encode(c: Cookie): String =
        listOf(
            c.name, c.value, c.domain, c.path, c.expiresAt.toString(),
            c.secure.toString(), c.httpOnly.toString(), c.hostOnly.toString(),
        ).joinToString("\t")

    private fun decode(line: String): Cookie? {
        val p = line.split("\t")
        if (p.size < 8) return null
        return try {
            val builder = Cookie.Builder()
                .name(p[0])
                .value(p[1])
                .path(p[3])
                .expiresAt(p[4].toLong())
            if (p[7].toBoolean()) builder.hostOnlyDomain(p[2]) else builder.domain(p[2])
            if (p[5].toBoolean()) builder.secure()
            if (p[6].toBoolean()) builder.httpOnly()
            builder.build()
        } catch (_: Exception) {
            null
        }
    }
}
