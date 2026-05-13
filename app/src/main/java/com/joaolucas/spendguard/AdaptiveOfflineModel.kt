package com.joaolucas.spendguard

import android.content.Context

object AdaptiveOfflineModel {

    fun analyze(
        context: Context,
        item: String,
        price: Double,
        justification: String
    ): InterventionResult {
        val model   = AdaptiveModelTrainer.load(context)
        val hasData = AdaptiveModelTrainer.hasEnoughData(context)

        return if (hasData) {
            analyzeWithModel(model, item, price, justification)
        } else {
            OfflineAnalyzer.analyze(item, price, justification)
        }
    }

    private fun analyzeWithModel(
        model: LearnedModel,
        item: String,
        price: Double,
        justification: String
    ): InterventionResult {
        val combined = "$item $justification".lowercase()
        val words    = combined.split(Regex("\\s+")).filter { it.length >= 4 }

        var impulseScore    = 0f
        var necessityScore  = 0f
        val matchedRules    = mutableListOf<LearnedRule>()

        for (word in words) {
            val rule = model.rules.find { it.keyword == word } ?: continue
            matchedRules.add(rule)
            if (rule.isImpulse) {
                impulseScore += rule.weight * rule.occurrences.coerceAtMost(5)
            } else {
                necessityScore += rule.weight * rule.occurrences.coerceAtMost(5)
            }
        }

        val baseResult = OfflineAnalyzer.analyze(item, price, justification)

        val inferredCategory = matchedRules
            .filter { it.categoryHint.isNotEmpty() }
            .groupBy { it.categoryHint }
            .maxByOrNull { it.value.size }
            ?.key ?: baseResult.category

        val catBlockRate  = model.categoryBlockRates[inferredCategory] ?: model.blockRate
        val priceSignal   = when {
            model.avgBlockedPrice > 0 && price >= model.avgBlockedPrice * 0.8  -> 0.4f
            model.avgApprovedPrice > 0 && price <= model.avgApprovedPrice * 1.2 -> -0.2f
            else -> 0f
        }

        val totalImpulse   = impulseScore + catBlockRate * 2f + priceSignal
        val totalNecessity = necessityScore

        val allowed = when {
            totalNecessity > totalImpulse * 1.5f -> true
            totalImpulse > totalNecessity * 1.5f -> false
            else -> baseResult.allowed
        }

        val coolingOff = if (!allowed) when {
            price > 1000 -> 168
            price > 300  -> 48
            price > 100  -> 24
            else         -> 24
        } else 0

        val confidence = when {
            matchedRules.size >= 3 -> "alta"
            matchedRules.size >= 1 -> "média"
            else -> "baixa"
        }

        val message = if (allowed) {
            "[Offline • confiança $confidence] Com base no seu histórico, essa compra parece razoável. Verifique com o Guardião online para uma análise completa."
        } else {
            "[Offline • confiança $confidence] Seu histórico sugere que essa compra tem perfil de impulso. Aguarde ${coolingOff}h antes de decidir."
        }

        return InterventionResult(
            allowed        = allowed,
            message        = message,
            coolingOffTime = coolingOff,
            category       = inferredCategory
        )
    }
}
