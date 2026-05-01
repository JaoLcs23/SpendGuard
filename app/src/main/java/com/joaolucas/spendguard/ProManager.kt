package com.joaolucas.spendguard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class ProManager(context: Context) {

    companion object {
        const val FREE_WEEKLY_LIMIT         = 5
        const val FREE_WEEKLY_SAVES_LIMIT   = 3

        private const val PREFS              = "spendguard_pro_secure"
        private const val KEY_IS_PRO         = "is_pro"
        private const val KEY_PLAN           = "plan"
        private const val KEY_GUARDIAN_WEEK  = "guardian_week"
        private const val KEY_GUARDIAN_COUNT = "guardian_count"
        private const val KEY_CALC_WEEK      = "calculator_week"
        private const val KEY_CALC_COUNT     = "calculator_count"
        private const val KEY_SAVES_WEEK     = "library_saves_week"
        private const val KEY_SAVES_COUNT    = "library_saves_count"
        private const val KEY_TRIAL_EXPIRES  = "trial_expires_at"
        private const val TAG                = "ProManager"
    }

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro

    private val _plan = MutableStateFlow(prefs.getString(KEY_PLAN, "none") ?: "none")
    val plan: StateFlow<String> = _plan

    fun canUseGuardian(): Boolean {
        checkTrialExpiry()
        if (_isPro.value) return true
        return getWeeklyCount(KEY_GUARDIAN_WEEK, KEY_GUARDIAN_COUNT) < FREE_WEEKLY_LIMIT
    }

    fun guardianUsesLeft(): Int {
        if (_isPro.value) return Int.MAX_VALUE
        return maxOf(0, FREE_WEEKLY_LIMIT - getWeeklyCount(KEY_GUARDIAN_WEEK, KEY_GUARDIAN_COUNT))
    }

    fun registerGuardianUse() {
        if (!_isPro.value) incrementWeeklyCount(KEY_GUARDIAN_WEEK, KEY_GUARDIAN_COUNT)
    }

    fun canUseCalculator(): Boolean {
        checkTrialExpiry()
        if (_isPro.value) return true
        return getWeeklyCount(KEY_CALC_WEEK, KEY_CALC_COUNT) < FREE_WEEKLY_LIMIT
    }

    fun calculatorUsesLeft(): Int {
        if (_isPro.value) return Int.MAX_VALUE
        return maxOf(0, FREE_WEEKLY_LIMIT - getWeeklyCount(KEY_CALC_WEEK, KEY_CALC_COUNT))
    }

    fun registerCalculatorUse() {
        if (!_isPro.value) incrementWeeklyCount(KEY_CALC_WEEK, KEY_CALC_COUNT)
    }

    fun canSaveToLibrary(): Boolean {
        checkTrialExpiry()
        if (_isPro.value) return true
        return getWeeklyCount(KEY_SAVES_WEEK, KEY_SAVES_COUNT) < FREE_WEEKLY_SAVES_LIMIT
    }

    fun librarySavesLeft(): Int {
        if (_isPro.value) return Int.MAX_VALUE
        return maxOf(0, FREE_WEEKLY_SAVES_LIMIT - getWeeklyCount(KEY_SAVES_WEEK, KEY_SAVES_COUNT))
    }

    fun registerLibrarySave() {
        if (!_isPro.value) incrementWeeklyCount(KEY_SAVES_WEEK, KEY_SAVES_COUNT)
    }

    fun canUseNotifications(): Boolean = _isPro.value

    fun activatePro(plan: String) {
        prefs.edit()
            .putBoolean(KEY_IS_PRO, true)
            .putString(KEY_PLAN, plan)
            .apply()
        _isPro.value = true
        _plan.value = plan
    }

    fun activateTrialPro(days: Int) {
        val expiresAt = System.currentTimeMillis() + days.toLong() * 24 * 60 * 60 * 1000
        prefs.edit()
            .putBoolean(KEY_IS_PRO, true)
            .putString(KEY_PLAN, "trial")
            .putLong(KEY_TRIAL_EXPIRES, expiresAt)
            .apply()
        _isPro.value = true
        _plan.value = "trial"
    }

    private fun checkTrialExpiry() {
        val expiresAt = prefs.getLong(KEY_TRIAL_EXPIRES, 0L)
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            deactivatePro()
            prefs.edit().putLong(KEY_TRIAL_EXPIRES, 0L).apply()
        }
    }

    fun deactivatePro() {
        prefs.edit()
            .putBoolean(KEY_IS_PRO, false)
            .putString(KEY_PLAN, "none")
            .apply()
        _isPro.value = false
        _plan.value = "none"
    }

    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun getWeeklyCount(weekKey: String, countKey: String): Int {
        val savedWeek = prefs.getString(weekKey, "") ?: ""
        if (savedWeek != currentWeekKey()) return 0
        return prefs.getInt(countKey, 0)
    }

    private fun incrementWeeklyCount(weekKey: String, countKey: String) {
        val current = getWeeklyCount(weekKey, countKey)
        prefs.edit()
            .putString(weekKey, currentWeekKey())
            .putInt(countKey, current + 1)
            .apply()
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            buildEncryptedPrefs(context)
        } catch (firstException: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences falhou na primeira tentativa, tentando recriar.", firstException)
            try {
                context.deleteSharedPreferences(PREFS)
                buildEncryptedPrefs(context)
            } catch (secondException: Exception) {
                Log.e(TAG, "Falha crítica ao inicializar armazenamento seguro.", secondException)
                throw SecurityException(
                    "Não foi possível inicializar o armazenamento seguro do app. " +
                            "Tente desinstalar e reinstalar o aplicativo.",
                    secondException
                )
            }
        }
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}