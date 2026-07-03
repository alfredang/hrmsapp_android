package com.tertiaryinfotech.hrportal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed "remember my email" preference — replaces the raw SharedPreferences access
 * `AuthViewModel` used to do directly. Session/cookie persistence itself lives in
 * `PersistentCookieJar` (unrelated to this class).
 */
@Singleton
class SessionPrefs @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private val rememberFlagKey = booleanPreferencesKey("hrms_remember_email")
    private val rememberedEmailKey = stringPreferencesKey("hrms_remembered_email")

    suspend fun rememberFlag(): Boolean = dataStore.data.first()[rememberFlagKey] ?: false

    suspend fun rememberedEmail(): String = dataStore.data.first()[rememberedEmailKey] ?: ""

    suspend fun setRememberFlag(value: Boolean) {
        dataStore.edit { it[rememberFlagKey] = value }
    }

    suspend fun setRememberedEmail(email: String?) {
        dataStore.edit {
            if (email != null) it[rememberedEmailKey] = email else it.remove(rememberedEmailKey)
        }
    }
}
