package com.joaolucas.spendguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class StrictModeManager(private val context: Context) {

    /**
     * CORREÇÃO DE SEGURANÇA (Issue #9):
     * Substituído getSharedPreferences() comum por EncryptedSharedPreferences.
     *
     * O limite mensal e os flags de notificação são dados financeiros do usuário.
     * Em dispositivos rooteados, o arquivo de preferências era acessível em texto
     * claro. Com AES-256-GCM tanto chaves quanto valores ficam cifrados em repouso.
     */
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val KEY_ENABLED       = "strict_enabled"
        private const val KEY_LIMIT         = "strict_limit"
        private const val KEY_NOTIFIED_80   = "strict_notified_80"
        private const val KEY_NOTIFIED_100  = "strict_notified_100"
        private const val PREFS_NAME        = "spendguard_strict_secure"
        private const val TAG               = "StrictModeManager"

        const val CHANNEL_ID   = "strict_mode_alerts"
        const val NOTIF_ID_80  = 3001
        const val NOTIF_ID_100 = 3002

        const val DEFAULT_LIMIT = 500.0
    }

    private val _state = MutableStateFlow(buildState(0.0))
    val state: StateFlow<StrictModeState> = _state

    init { createNotificationChannel() }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun getLimit(): Double = prefs.getFloat(KEY_LIMIT, DEFAULT_LIMIT.toFloat()).toDouble()

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        refreshState(0.0)
    }

    fun setLimit(limit: Double) {
        prefs.edit().putFloat(KEY_LIMIT, limit.toFloat()).apply()
    }

    fun onSpendingUpdated(monthlySpent: Double) {
        refreshState(monthlySpent)
        if (!isEnabled()) return
        val limit    = getLimit()
        val pct      = if (limit > 0) monthlySpent / limit else 0.0
        val monthKey = currentMonthKey()

        if (pct >= 1.0 && prefs.getString(KEY_NOTIFIED_100, "") != monthKey) {
            sendNotification(
                id      = NOTIF_ID_100,
                title   = "🚨 Limite mensal atingido",
                message = "Você gastou R$ ${"%.2f".format(monthlySpent)} de R$ ${"%.2f".format(limit)} este mês. Modo Estrito ativo."
            )
            prefs.edit().putString(KEY_NOTIFIED_100, monthKey).apply()
        } else if (pct >= 0.8 && prefs.getString(KEY_NOTIFIED_80, "") != monthKey) {
            sendNotification(
                id      = NOTIF_ID_80,
                title   = "⚠️ 80% do limite atingido",
                message = "Você já gastou R$ ${"%.2f".format(monthlySpent)} de R$ ${"%.2f".format(limit)} este mês."
            )
            prefs.edit().putString(KEY_NOTIFIED_80, monthKey).apply()
        }
    }

    fun refreshState(monthlySpent: Double) {
        _state.value = buildState(monthlySpent)
    }

    private fun buildState(monthlySpent: Double): StrictModeState {
        val limit     = getLimit()
        val enabled   = isEnabled()
        val fraction  = if (limit > 0 && enabled) (monthlySpent / limit).coerceIn(0.0, 1.0) else 0.0
        val remaining = (limit - monthlySpent).coerceAtLeast(0.0)
        return StrictModeState(
            enabled      = enabled,
            limit        = limit,
            monthlySpent = monthlySpent,
            fraction     = fraction.toFloat(),
            remaining    = remaining,
            isOverLimit  = enabled && monthlySpent >= limit
        )
    }

    data class StrictModeState(
        val enabled: Boolean,
        val limit: Double,
        val monthlySpent: Double,
        val fraction: Float,
        val remaining: Double,
        val isOverLimit: Boolean
    )

    private fun currentMonthKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(id, notif)
    }

    private fun createNotificationChannel() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Modo Estrito — Alertas de Limite",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos quando você se aproxima ou atinge o limite mensal do Modo Estrito"
                }
            )
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            buildEncryptedPrefs(context)
        } catch (firstException: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences falhou, tentando recriar.", firstException)
            try {
                context.deleteSharedPreferences(PREFS_NAME)
                buildEncryptedPrefs(context)
            } catch (secondException: Exception) {
                Log.e(TAG, "Falha crítica no armazenamento seguro do StrictMode.", secondException)
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
}