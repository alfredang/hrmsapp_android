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
    /** The company address last signed in with Google here: the one-tap login hint (22 Nov 2026). */
    private val googleCompanyEmailKey = stringPreferencesKey("hrms_google_company_email")

    suspend fun googleCompanyEmail(): String? = dataStore.data.first()[googleCompanyEmailKey]

    suspend fun setGoogleCompanyEmail(email: String) {
        dataStore.edit { it[googleCompanyEmailKey] = email }
    }

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
