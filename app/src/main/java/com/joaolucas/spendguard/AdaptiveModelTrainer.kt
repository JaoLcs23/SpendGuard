package com.joaolucas.spendguard

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LearnedRule(
    val keyword: String,
    val weight: Float,
    val isImpulse: Boolean,
    val categoryHint: String = "",
    val priceRangeMin: Double = 0.0,
    val priceRangeMax: Double = Double.MAX_VALUE,
    val occurrences: Int = 1
)

@Serializable
data class LearnedModel(
    val rules: List<LearnedRule> = emptyList(),
    val totalDecisions: Int = 0,
    val blockRate: Float = 0f,
    val avgBlockedPrice: Double = 0.0,
    val avgApprovedPrice: Double = 0.0,
    val categoryBlockRates: Map<String, Float> = emptyMap(),
    val lastUpdated: Long = 0L
)

object AdaptiveModelTrainer {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private const val PREFS_NAME = "spendguard_adaptive_model"
    private const val KEY_MODEL  = "learned_model"
    private const val MAX_RULES  = 200

    fun learn(
        context: Context,
        item: String,
        price: Double,
        justification: String,
        category: String,
        wasBlocked: Boolean
    ) {
        val prefs   = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = load(prefs)

        val combined  = "$item $justification".lowercase()
        val words     = combined.split(Regex("\\s+")).filter { it.length >= 4 }
        val newRules  = current.rules.toMutableList()

        for (word in words) {
            val existing = newRules.indexOfFirst { it.keyword == word && it.isImpulse == wasBlocked }
            if (existing >= 0) {
                val r = newRules[existing]
                newRules[existing] = r.copy(
                    occurrences = r.occurrences + 1,
                    weight = minOf(1f, r.weight + 0.05f)
                )
            } else {
                newRules.add(LearnedRule(
                    keyword      = word,
                    weight       = 0.3f,
                    isImpulse    = wasBlocked,
                    categoryHint = category,
                    priceRangeMin = price * 0.5,
                    priceRangeMax = price * 2.0
                ))
            }
        }

        val trimmed = newRules
            .sortedByDescending { it.occurrences }
            .take(MAX_RULES)

        val total       = current.totalDecisions + 1
        val blockCount  = (current.blockRate * current.totalDecisions + if (wasBlocked) 1 else 0)
        val newBlockRate = blockCount / total

        val catRates = current.categoryBlockRates.toMutableMap()
        val catPrev  = catRates[category] ?: 0.5f
        catRates[category] = catPrev * 0.8f + (if (wasBlocked) 1f else 0f) * 0.2f

        val updated = current.copy(
            rules            = trimmed,
            totalDecisions   = total,
            blockRate        = newBlockRate,
            avgBlockedPrice  = if (wasBlocked)
                (current.avgBlockedPrice * 0.9 + price * 0.1)
            else current.avgBlockedPrice,
            avgApprovedPrice = if (!wasBlocked)
                (current.avgApprovedPrice * 0.9 + price * 0.1)
            else current.avgApprovedPrice,
            categoryBlockRates = catRates,
            lastUpdated      = System.currentTimeMillis()
        )

        prefs.edit().putString(KEY_MODEL, json.encodeToString(updated)).apply()
    }

    fun load(context: Context): LearnedModel {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return load(prefs)
    }

    private fun load(prefs: android.content.SharedPreferences): LearnedModel {
        val raw = prefs.getString(KEY_MODEL, null) ?: return LearnedModel()
        return try { json.decodeFromString(raw) } catch (_: Exception) { LearnedModel() }
    }

    fun hasEnoughData(context: Context): Boolean =
        load(context).totalDecisions >= 10

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_MODEL).apply()
    }
}
