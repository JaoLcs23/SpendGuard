package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoalManager(private val context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val PREFS_NAME = "spendguard_goals_secure"
        private const val TAG = "GoalManager"
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            buildEncryptedPrefs(context)
        } catch (e: Exception) {
            Log.w(TAG, "Falha no EncryptedSharedPreferences, recriando.", e)
            try {
                context.deleteSharedPreferences(PREFS_NAME)
                buildEncryptedPrefs(context)
            } catch (e2: Exception) {
                Log.e(TAG, "Falha critica no armazenamento seguro de Goals", e2)
                context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
