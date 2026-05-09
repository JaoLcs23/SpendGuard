package com.joaolucas.spendguard

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WeeklyInsightManager(context: Context) {

    private val prefs = context.getSharedPreferences("spendguard_weekly_insight", Context.MODE_PRIVATE)
    private val json  = Json { ignoreUnknownKeys = true }

    private val _insight = MutableStateFlow(loadInsight())
    val insight: StateFlow<WeeklyInsight?> = _insight

    fun loadInsight(): WeeklyInsight? {
        val raw = prefs.getString("last_insight", null) ?: return null
        return try { json.decodeFromString<WeeklyInsight>(raw) } catch (_: Exception) { null }
    }

    fun saveInsight(insight: WeeklyInsight) {
        prefs.edit().putString("last_insight", json.encodeToString(insight)).apply()
        _insight.value = insight
    }

    fun shouldRefresh(): Boolean {
        val last = loadInsight()?.generatedAt ?: 0L
        val weekMs = 7L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - last > weekMs
    }

    fun clear() {
        prefs.edit().remove("last_insight").apply()
        _insight.value = null
    }
}
