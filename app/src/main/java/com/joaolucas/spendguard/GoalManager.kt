package com.joaolucas.spendguard

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoalManager(context: Context) {

    private val prefs = context.getSharedPreferences("spendguard_goals", Context.MODE_PRIVATE)

    private val _monthlyGoal = MutableStateFlow(getMonthlyGoal())
    val monthlyGoal: StateFlow<Double> = _monthlyGoal

    fun getMonthlyGoal(): Double = java.lang.Double.longBitsToDouble(
        prefs.getLong("monthly_goal_bits", java.lang.Double.doubleToLongBits(0.0))
    )

    fun setMonthlyGoal(value: Double) {
        prefs.edit()
            .putLong("monthly_goal_bits", java.lang.Double.doubleToLongBits(value))
            .apply()
        _monthlyGoal.value = value
    }

    fun clearGoal() {
        prefs.edit().remove("monthly_goal_bits").apply()
        _monthlyGoal.value = 0.0
    }

    fun progressFraction(saved: Double): Float {
        val goal = getMonthlyGoal()
        if (goal <= 0.0) return 0f
        return (saved / goal).coerceIn(0.0, 1.0).toFloat()
    }
}
