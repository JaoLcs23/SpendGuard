package com.joaolucas.spendguard

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class PredictiveInsight(
    val title: String = "",
    val message: String = "",
    val riskLevel: String = "low",
    val generatedAt: Long = 0L
)

class PredictiveInsightManager(context: Context) {
    private val prefs = context.getSharedPreferences("spendguard_predictive", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(insight: PredictiveInsight) {
        prefs.edit()
            .putString("last_insight", json.encodeToString(insight))
            .putLong("last_generated", System.currentTimeMillis())
            .apply()
    }

    fun load(): PredictiveInsight? {
        val raw = prefs.getString("last_insight", null) ?: return null
        return try {
            json.decodeFromString<PredictiveInsight>(raw)
        } catch (_: Exception) { null }
    }

    fun shouldRefresh(): Boolean {
        val last = prefs.getLong("last_generated", 0L)
        return System.currentTimeMillis() - last > 20 * 60 * 60 * 1000
    }
}
