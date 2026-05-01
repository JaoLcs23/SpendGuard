package com.joaolucas.spendguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReflectionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val itemName = inputData.getString("item_name")
            ?.take(200)
            ?.replace(Regex("[\\x00-\\x1F\\x7F]"), " ")
            ?: "sua compra"
        val price = inputData.getDouble("price", 0.0)
            .coerceIn(0.0, 1_000_000.0)

        showNotification(itemName, price)
        return Result.success()
    }

    private fun showNotification(itemName: String, price: Double) {
        val channelId = "reflection_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reflexão de Compras",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes para refletir antes de comprar"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val priceText = if (price > 0) "R$ ${"%.2f".format(price)}" else ""
        val bodyText = buildString {
            append("Você ainda quer comprar $itemName")
            if (priceText.isNotEmpty()) append(" por $priceText")
            append("?\n\nLembre-se: esse dinheiro poderia estar rendendo na B3!")
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de refletir!")
            .setContentText("Você ainda quer comprar $itemName?")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val MAX_ITEM_NAME_LENGTH = 200

        fun schedule(context: Context, itemName: String, price: Double, delayMinutes: Long) {
            val safeItemName = itemName
                .take(MAX_ITEM_NAME_LENGTH)
                .replace(Regex("[\\x00-\\x1F\\x7F]"), " ")
                .trim()
                .ifEmpty { "compra" }

            val safePrice = price.coerceIn(0.0, 1_000_000.0)

            val data = workDataOf(
                "item_name" to safeItemName,
                "price"     to safePrice
            )

            val request = OneTimeWorkRequestBuilder<ReflectionWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(data)
                .addTag("reflection_${safeItemName.take(50)}")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reflection_${safeItemName.take(50)}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}