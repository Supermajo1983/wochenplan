package de.wochenplan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshAsync(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) refreshAsync(context) else super.onReceive(context, intent)
    }

    private fun refreshAsync(context: Context) {
        val pending = goAsync()
        thread {
            try {
                update(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "de.wochenplan.app.REFRESH"

        /** Muss auf einem Hintergrund-Thread laufen (Netzwerk). */
        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val configured = Plan.isConfigured(context)
            val (record, offline) = if (configured) Plan.load(context) else (null to false)

            val body = when {
                !configured -> "Bitte zuerst die JSONbin-Daten eintragen (Antippen)."
                record == null -> "Keine Daten verfügbar."
                else -> Plan.todayLines(record).joinToString("\n")
            }
            val time = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date())
            val status = if (offline) "Offline – letzter Stand ($time)" else "Aktualisiert $time"

            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val target = if (configured) MainActivity::class.java else SettingsActivity::class.java
            val open = PendingIntent.getActivity(context, 0, Intent(context, target), flags)
            val refresh = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, TodayWidgetProvider::class.java).setAction(ACTION_REFRESH), flags
            )

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_today)
                views.setTextViewText(R.id.widget_title, Plan.todayTitle())
                views.setTextViewText(R.id.widget_body, body)
                views.setTextViewText(R.id.widget_status, if (configured) status else "")
                views.setOnClickPendingIntent(R.id.widget_root, open)
                views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
