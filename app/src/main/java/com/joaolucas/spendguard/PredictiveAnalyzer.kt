package com.joaolucas.spendguard

import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
data class PredictivePatterns(
    val topSpendingDay: String = "",
    val topSpendingHour: String = "",
    val weeklySpendingCurrent: Double = 0.0,
    val weeklySpendingPrevious: Double = 0.0,
    val spendingTrend: String = "stable",
    val impulseRate: Double = 0.0,
    val impulseRateTrend: String = "stable",
    val riskCategory: String = "",
    val riskCategoryBlockRate: Double = 0.0,
    val totalAnalyzed: Int = 0,
    val totalBlocked: Int = 0,
    val monthTotal: Double = 0.0
)

object PredictiveAnalyzer {

    private val dayNames = mapOf(
        Calendar.SUNDAY to "Domingo",
        Calendar.MONDAY to "Segunda-feira",
        Calendar.TUESDAY to "Terça-feira",
        Calendar.WEDNESDAY to "Quarta-feira",
        Calendar.THURSDAY to "Quinta-feira",
        Calendar.FRIDAY to "Sexta-feira",
        Calendar.SATURDAY to "Sábado"
    )

    fun analyze(purchases: List<PurchaseEntity>): PredictivePatterns {
        if (purchases.size < 3) return PredictivePatterns(totalAnalyzed = purchases.size)

        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val fourteenDaysAgo = now - 14L * 24 * 60 * 60 * 1000
        val daySpending = mutableMapOf<Int, Double>()
        val hourSpending = mutableMapOf<Int, Int>()

        purchases.forEach { p ->
            cal.timeInMillis = p.timestamp
            val day = cal.get(Calendar.DAY_OF_WEEK)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            daySpending[day] = (daySpending[day] ?: 0.0) + p.price
            hourSpending[hour] = (hourSpending[hour] ?: 0) + 1
        }

        val topDay = daySpending.maxByOrNull { it.value }?.key ?: Calendar.MONDAY
        val topHour = hourSpending.maxByOrNull { it.value }?.key ?: 12
        val hourRange = "${topHour}h–${(topHour + 2).coerceAtMost(23)}h"

        val thisWeek = purchases.filter { it.timestamp > sevenDaysAgo }.sumOf { it.price }
        val lastWeek = purchases.filter { it.timestamp in fourteenDaysAgo..sevenDaysAgo }.sumOf { it.price }
        val trend = when {
            lastWeek == 0.0 -> "stable"
            thisWeek > lastWeek * 1.3 -> "rising"
            thisWeek < lastWeek * 0.7 -> "falling"
            else -> "stable"
        }

        val blocked = purchases.count { it.wasBlocked }
        val impulseRate = if (purchases.isNotEmpty()) blocked.toDouble() / purchases.size else 0.0

        val recentBlocked = purchases.filter { it.timestamp > sevenDaysAgo }.count { it.wasBlocked }
        val recentTotal = purchases.count { it.timestamp > sevenDaysAgo }
        val recentRate = if (recentTotal > 0) recentBlocked.toDouble() / recentTotal else 0.0
        val impulseRateTrend = when {
            recentRate > impulseRate + 0.15 -> "rising"
            recentRate < impulseRate - 0.15 -> "falling"
            else -> "stable"
        }

        val categoryStats = purchases.groupBy { it.category }
        val riskCategory = categoryStats.maxByOrNull { (_, items) ->
            val blockRate = items.count { it.wasBlocked }.toDouble() / items.size
            blockRate * items.size
        }?.key ?: "OUTROS"
        val riskItems = categoryStats[riskCategory] ?: emptyList()
        val riskBlockRate = if (riskItems.isNotEmpty()) riskItems.count { it.wasBlocked }.toDouble() / riskItems.size else 0.0

        return PredictivePatterns(
            topSpendingDay = dayNames[topDay] ?: "N/A",
            topSpendingHour = hourRange,
            weeklySpendingCurrent = thisWeek,
            weeklySpendingPrevious = lastWeek,
            spendingTrend = trend,
            impulseRate = impulseRate,
            impulseRateTrend = impulseRateTrend,
            riskCategory = riskCategory,
            riskCategoryBlockRate = riskBlockRate,
            totalAnalyzed = purchases.size,
            totalBlocked = blocked,
            monthTotal = purchases.sumOf { it.price }
        )
    }
}
