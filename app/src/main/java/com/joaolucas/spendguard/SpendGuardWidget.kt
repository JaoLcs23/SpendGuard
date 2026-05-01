package com.joaolucas.spendguard

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SpendGuardWidget : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onEnabled(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids     = manager.getAppWidgetIds(ComponentName(context, SpendGuardWidget::class.java))
        ids.forEach { updateWidget(context, manager, it) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        widgetScope.launch {
            val database = SpendGuardDatabase.getDatabase(context)
            val all      = database.purchaseDao().getAllPurchases()
            val snapshot = mutableListOf<PurchaseEntity>()

            val job = launch {
                all.collect { list ->
                    snapshot.clear()
                    snapshot.addAll(list)
                }
            }
            delay(300)
            job.cancel()

            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val weekStart = cal.timeInMillis
            val thisWeek  = snapshot.filter { it.timestamp >= weekStart }

            val blocked = thisWeek.count { it.wasBlocked }
            val saved   = thisWeek.filter { it.wasBlocked }.sumOf { it.price }

            val views = RemoteViews(context.packageName, R.layout.widget_quick_entry)

            views.setTextViewText(
                R.id.widget_saved,
                if (saved > 0) "R$ ${"%.0f".format(saved)}" else "R$ 0"
            )
            views.setTextViewText(R.id.widget_blocked_count, "$blocked")
            views.setTextViewText(
                R.id.widget_week,
                SimpleDateFormat("EEE dd/MM", Locale("pt", "BR")).format(Date())
            )

            views.setOnClickPendingIntent(
                R.id.widget_root,
                makeQuickEntryIntent(context, requestCode = widgetId * 10, category = null)
            )

            listOf(
                R.id.btn_food     to SpendingCategory.ALIMENTACAO.name,
                R.id.btn_leisure  to SpendingCategory.LAZER.name,
                R.id.btn_clothing to SpendingCategory.VESTUARIO.name,
                R.id.btn_tech     to SpendingCategory.TECNOLOGIA.name
            ).forEachIndexed { index, (viewId, category) ->
                views.setOnClickPendingIntent(
                    viewId,
                    makeQuickEntryIntent(
                        context     = context,
                        requestCode = widgetId * 10 + index + 1,
                        category    = category
                    )
                )
            }

            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun makeQuickEntryIntent(
        context: Context,
        requestCode: Int,
        category: String?
    ): PendingIntent {
        val intent = Intent(context, QuickEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (category != null) putExtra(QuickEntryActivity.EXTRA_CATEGORY, category)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun saveUserId(context: Context, userId: String) {
            context.getSharedPreferences("spendguard_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_user_id", userId)
                .apply()
        }

        fun requestUpdate(context: Context) {
            val intent = Intent(context, SpendGuardWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, SpendGuardWidget::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
