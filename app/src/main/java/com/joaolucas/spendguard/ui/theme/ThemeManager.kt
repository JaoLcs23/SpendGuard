package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppTheme(val label: String) {
    SYSTEM("Seguir sistema"),
    DARK("Escuro"),
    LIGHT("Claro")
}

class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendguard_theme", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "app_theme"
    }

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    fun getTheme(): AppTheme = _theme.value

    private fun loadTheme(): AppTheme {
        val saved = prefs.getString(KEY_THEME, AppTheme.DARK.name) ?: AppTheme.DARK.name
        return AppTheme.values().firstOrNull { it.name == saved } ?: AppTheme.DARK
    }
}