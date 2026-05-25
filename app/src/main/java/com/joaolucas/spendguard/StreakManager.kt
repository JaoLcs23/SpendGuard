package com.joaolucas.spendguard

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class StreakManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            "spendguard_streak",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("spendguard_streak_fb", Context.MODE_PRIVATE)
    }

    private val _streak = MutableStateFlow(currentStreak())
    val streak: StateFlow<Int> = _streak

    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun yesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    fun currentStreak(): Int = prefs.getInt("streak_count", 0)
    
    fun getStreakCount(): Int = currentStreak()
    fun getStreakLastDay(): String = prefs.getString("streak_last_day", "") ?: ""

    fun setStreakCount(count: Int) {
        prefs.edit().putInt("streak_count", count).apply()
        _streak.value = count
    }

    fun setStreakLastDay(day: String) {
        prefs.edit().putString("streak_last_day", day).apply()
    }

    fun onImpulseBlocked() {
        val today = todayKey()
        val lastDay = prefs.getString("streak_last_day", null)
        val count = prefs.getInt("streak_count", 0)

        val newCount = when (lastDay) {
            today -> count
            yesterdayKey() -> count + 1
            null -> 1
            else -> 1
        }

        prefs.edit()
            .putInt("streak_count", newCount)
            .putString("streak_last_day", today)
            .apply()
        _streak.value = newCount
    }

    fun onImpulsiveApproved() {
        prefs.edit()
            .putInt("streak_count", 0)
            .remove("streak_last_day")
            .apply()
        _streak.value = 0
    }

    fun refresh() {
        _streak.value = currentStreak()
    }
}
