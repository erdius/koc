package com.erdman.kofc6650.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.erdman.kofc6650.R
import com.erdman.kofc6650.data.NextEventWidgetData

class NextEventWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NextEventWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_next_event)
            val info = NextEventWidgetData.load(context)
            if (info != null) {
                views.setTextViewText(R.id.widget_title, info.title)
                views.setTextViewText(
                    R.id.widget_datetime,
                    info.dateDisplay + (info.time?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                )
                if (!info.location.isNullOrBlank()) {
                    views.setViewVisibility(R.id.widget_location, View.VISIBLE)
                    views.setTextViewText(R.id.widget_location, info.location)
                } else {
                    views.setViewVisibility(R.id.widget_location, View.GONE)
                }
            } else {
                views.setTextViewText(R.id.widget_title, "No upcoming events")
                views.setTextViewText(R.id.widget_datetime, "")
                views.setViewVisibility(R.id.widget_location, View.GONE)
            }
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
