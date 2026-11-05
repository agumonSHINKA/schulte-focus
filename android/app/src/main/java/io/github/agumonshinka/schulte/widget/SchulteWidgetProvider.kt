package io.github.agumonshinka.schulte.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.agumonshinka.schulte.MainActivity
import io.github.agumonshinka.schulte.R
import io.github.agumonshinka.schulte.StatsStore

class SchulteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_REFRESH,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> refreshAll(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "io.github.agumonshinka.schulte.ACTION_WIDGET_REFRESH"

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, SchulteWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            if (ids.isEmpty()) return
            for (id in ids) updateWidget(context, mgr, id)
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val store = StatsStore(context)
            val snap = store.snapshot()
            val views = RemoteViews(context.packageName, R.layout.widget_schulte)

            views.setTextViewText(
                R.id.widget_streak,
                context.getString(R.string.widget_streak, snap.streak)
            )

            val bestLabel = snap.bestTodaySecondsLabel()
            if (bestLabel != null && snap.gamesToday > 0) {
                views.setTextViewText(
                    R.id.widget_best,
                    context.getString(R.string.widget_best, bestLabel)
                )
            } else {
                views.setTextViewText(R.id.widget_best, context.getString(R.string.widget_no_practice))
            }

            views.setTextViewText(
                R.id.widget_games,
                context.getString(R.string.widget_games_today, snap.gamesToday)
            )

            val launch = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_START_GAME
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_START_GAME, true)
            }
            val pi = PendingIntent.getActivity(
                context,
                appWidgetId,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start, pi)
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
