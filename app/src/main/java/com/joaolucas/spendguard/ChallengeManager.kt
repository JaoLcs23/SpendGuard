package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class ChallengeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendguard_challenge", Context.MODE_PRIVATE)

    companion object {
        const val CHALLENGE_DAYS        = 30
        private const val KEY_START_MS  = "challenge_start_ms"
        private const val KEY_STATUS    = "challenge_status"
        private const val KEY_FAIL_ITEM = "challenge_fail_item"
        private const val KEY_FAIL_DAY  = "challenge_fail_day"

        const val STATUS_IDLE      = "idle"
        const val STATUS_ACTIVE    = "active"
        const val STATUS_FAILED    = "failed"
        const val STATUS_COMPLETED = "completed"
    }

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<ChallengeState> = _state

    fun start() {
        prefs.edit()
            .putLong(KEY_START_MS, System.currentTimeMillis())
            .putString(KEY_STATUS, STATUS_ACTIVE)
            .remove(KEY_FAIL_ITEM)
            .remove(KEY_FAIL_DAY)
            .apply()
        _state.value = buildState()
    }

    fun reset() {
        prefs.edit()
            .remove(KEY_START_MS)
            .putString(KEY_STATUS, STATUS_IDLE)
            .remove(KEY_FAIL_ITEM)
            .remove(KEY_FAIL_DAY)
            .apply()
        _state.value = buildState()
    }

    fun registerImpulseBuy(itemName: String) {
        if (prefs.getString(KEY_STATUS, STATUS_IDLE) != STATUS_ACTIVE) return
        val day = elapsedDays() + 1
        prefs.edit()
            .putString(KEY_STATUS, STATUS_FAILED)
            .putString(KEY_FAIL_ITEM, itemName)
            .putInt(KEY_FAIL_DAY, day)
            .apply()
        _state.value = buildState()
    }

    fun checkCompletion(): Boolean {
        if (prefs.getString(KEY_STATUS, STATUS_IDLE) != STATUS_ACTIVE) return false
        if (elapsedDays() >= CHALLENGE_DAYS) {
            prefs.edit().putString(KEY_STATUS, STATUS_COMPLETED).apply()
            _state.value = buildState()
            return true
        }
        return false
    }

    fun getStatus(): String = prefs.getString(KEY_STATUS, STATUS_IDLE) ?: STATUS_IDLE

    fun isActive(): Boolean = getStatus() == STATUS_ACTIVE

    fun elapsedDays(): Int {
        val startMs = prefs.getLong(KEY_START_MS, 0L)
        if (startMs == 0L) return 0
        val elapsed = System.currentTimeMillis() - startMs
        return TimeUnit.MILLISECONDS.toDays(elapsed).toInt().coerceIn(0, CHALLENGE_DAYS)
    }

    fun daysRemaining(): Int = (CHALLENGE_DAYS - elapsedDays()).coerceAtLeast(0)

    fun progressFraction(): Float = elapsedDays().toFloat() / CHALLENGE_DAYS

    fun startDateFormatted(): String {
        val startMs = prefs.getLong(KEY_START_MS, 0L)
        if (startMs == 0L) return ""
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
        return sdf.format(java.util.Date(startMs))
    }

    fun endDateFormatted(): String {
        val startMs = prefs.getLong(KEY_START_MS, 0L)
        if (startMs == 0L) return ""
        val endMs = startMs + TimeUnit.DAYS.toMillis(CHALLENGE_DAYS.toLong())
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
        return sdf.format(java.util.Date(endMs))
    }

    private fun buildState(): ChallengeState {
        return when (prefs.getString(KEY_STATUS, STATUS_IDLE)) {
            STATUS_ACTIVE    -> ChallengeState.Active(
                elapsedDays     = elapsedDays(),
                daysRemaining   = daysRemaining(),
                progressFraction = progressFraction(),
                startDate       = startDateFormatted(),
                endDate         = endDateFormatted()
            )
            STATUS_FAILED    -> ChallengeState.Failed(
                onDay    = prefs.getInt(KEY_FAIL_DAY, 0),
                itemName = prefs.getString(KEY_FAIL_ITEM, "") ?: ""
            )
            STATUS_COMPLETED -> ChallengeState.Completed
            else             -> ChallengeState.Idle
        }
    }

    sealed class ChallengeState {
        object Idle      : ChallengeState()
        object Completed : ChallengeState()
        data class Active(
            val elapsedDays: Int,
            val daysRemaining: Int,
            val progressFraction: Float,
            val startDate: String,
            val endDate: String
        ) : ChallengeState()
        data class Failed(
            val onDay: Int,
            val itemName: String
        ) : ChallengeState()
    }
}