package com.tertiaryinfotech.hrportal.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.hrmsPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hrms_prefs")

/**
 * Provides the app's Preferences DataStore — used for session-adjacent app prefs (remembered
 * login email). Cookie/session persistence stays in `PersistentCookieJar` (SharedPreferences-
 * backed, wired through `data/Net.kt`), which DataStore's key-value model doesn't fit.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.hrmsPrefsDataStore
}
