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

class WeeklyReviewWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val proManager = ProManager(applicationContext)
        if (!proManager.isPro.value) return Result.success()

        val analyzed  = inputData.getInt("analyzed", 0)
        val blocked   = inputData.getInt("blocked", 0)
        val savedAmount = inputData.getDouble("saved_amount", 0.0)

        if (analyzed == 0) return Result.success()

        showWeeklySummary(analyzed, blocked, savedAmount)
        return Result.success()
    }

    private fun showWeeklySummary(analyzed: Int, blocked: Int, savedAmount: Double) {
        val channelId = "weekly_review_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Resumo Semanal", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Resumo financeiro semanal do SpendGuard" }
            )
        }

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val savingsText = if (savedAmount > 0)
            " e evitou gastar R$ ${"%.2f".format(savedAmount)}" else ""

        val body = "Você analisou $analyzed compra${if (analyzed != 1) "s" else ""}, " +
                "bloqueou $blocked$savingsText. Continue assim! "

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Seu resumo semanal")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "weekly_review"

        fun scheduleWeekly(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeeklyReviewWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(calculateDelayUntilNextMonday(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerNow(context: Context, analyzed: Int, blocked: Int, savedAmount: Double) {
            val data = workDataOf(
                "analyzed"     to analyzed,
                "blocked"      to blocked,
                "saved_amount" to savedAmount
            )
            val request = OneTimeWorkRequestBuilder<WeeklyReviewWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        private fun calculateDelayUntilNextMonday(): Long {
            val now = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= now) add(java.util.Calendar.WEEK_OF_YEAR, 1)
            }
            return (cal.timeInMillis - now).coerceAtLeast(0)
        }
    }
}