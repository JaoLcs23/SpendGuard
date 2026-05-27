package com.joaolucas.spendguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class PredictiveInsightWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "predictive_insight_daily"
        const val CHANNEL_ID = "predictive_insights"
        private const val NOTIF_ID = 9001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PredictiveInsightWorker>(
                24, TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val manager = PredictiveInsightManager(appContext)
            if (!manager.shouldRefresh()) return Result.success()

            val userRepo = UserRepository()
            val userId = userRepo.getCurrentUserId() ?: return Result.success()
            if (!userRepo.isPro()) return Result.success()

            val db = SpendGuardDatabase.getDatabase(appContext)
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            val purchases = db.purchaseDao().getPurchasesSince(userId, thirtyDaysAgo)

            if (purchases.size < 3) return Result.success()

            val patterns = PredictiveAnalyzer.analyze(purchases)
            val profileManager = ProfileManager(appContext)
            val profile = profileManager.load()

            val geminiService = GeminiService(BuildConfig.BACKEND_URL)
            val insight = geminiService.predictInsight(patterns, profile)
                .copy(generatedAt = System.currentTimeMillis())

            manager.save(insight)
            showNotification(insight)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("PredictiveWorker", "Failed to generate insight", e)
            Result.retry()
        }
    }

    private fun showNotification(insight: PredictiveInsight) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Previsões do Guardião",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alertas preditivos baseados nos seus padrões de consumo"
                }
            )
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, NOTIF_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (insight.riskLevel) {
            "high" -> R.drawable.ic_notification
            "medium" -> R.drawable.ic_notification
            else -> R.drawable.ic_notification
        }

        nm.notify(NOTIF_ID, NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle("🔮 ${insight.title}")
            .setContentText(insight.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(insight.message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        )
    }
}
