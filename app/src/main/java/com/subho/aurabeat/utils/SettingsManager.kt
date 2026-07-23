package com.subho.aurabeat.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val THEME_COLOR = stringPreferencesKey("theme_color")
    val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    val CROSSFADE = booleanPreferencesKey("crossfade")
    val GAPLESS = booleanPreferencesKey("gapless")
}

class SettingsManager(private val context: Context) {
    val themeColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.THEME_COLOR] ?: "Purple" }
    val amoledMode: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.AMOLED_MODE] ?: true }
    val crossfade: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.CROSSFADE] ?: false }

    suspend fun setThemeColor(color: String) {
        context.dataStore.edit { it[SettingsKeys.THEME_COLOR] = color }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.AMOLED_MODE] = enabled }
    }
}
