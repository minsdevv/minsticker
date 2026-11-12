package com.minslab.minsticker.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromString(value: String): ThemeMode {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                SYSTEM
            }
        }
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    VI("vi", "Tiếng Việt"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VI
        }
    }
}

/**
 * Manages user preferences for Minsticker (App Theme, Keyboard Theme, Language).
 * Backed by SharedPreferences with real-time reactive StateFlows.
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "minsticker_preferences"
        private const val KEY_APP_THEME = "key_app_theme"
        private const val KEY_KEYBOARD_THEME = "key_keyboard_theme"
        private const val KEY_LANGUAGE = "key_language"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appTheme = MutableStateFlow(
        ThemeMode.fromString(prefs.getString(KEY_APP_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val appTheme: StateFlow<ThemeMode> = _appTheme.asStateFlow()

    private val _keyboardTheme = MutableStateFlow(
        ThemeMode.fromString(prefs.getString(KEY_KEYBOARD_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val keyboardTheme: StateFlow<ThemeMode> = _keyboardTheme.asStateFlow()

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.VI.code) ?: AppLanguage.VI.code)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setAppTheme(theme: ThemeMode) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
        _appTheme.value = theme
    }

    fun setKeyboardTheme(theme: ThemeMode) {
        prefs.edit().putString(KEY_KEYBOARD_THEME, theme.name).apply()
        _keyboardTheme.value = theme
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _language.value = language
    }
}
