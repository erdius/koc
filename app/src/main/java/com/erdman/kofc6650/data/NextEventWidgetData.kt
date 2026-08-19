package com.erdman.kofc6650.data

import android.content.Context

/**
 * The one piece of state handed off from the app to the home screen widget
 * -- deliberately tiny, since it's all the widget needs to render "what's
 * next." Written whenever events refresh, read by the widget provider on
 * each update. Ordinary SharedPreferences work fine here since the widget
 * provider runs in the same process as the app (no separate :widget
 * process is declared).
 */
data class NextEventInfo(
    val title: String,
    val dateDisplay: String,
    val time: String?,
    val location: String?,
)

object NextEventWidgetData {
    private const val PREFS_NAME = "next_event_widget"
    private const val KEY_TITLE = "title"
    private const val KEY_DATE = "date"
    private const val KEY_TIME = "time"
    private const val KEY_LOCATION = "location"

    fun save(context: Context, info: NextEventInfo?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (info == null) {
            prefs.edit().clear().apply()
            return
        }
        prefs.edit()
            .putString(KEY_TITLE, info.title)
            .putString(KEY_DATE, info.dateDisplay)
            .putString(KEY_TIME, info.time)
            .putString(KEY_LOCATION, info.location)
            .apply()
    }

    fun load(context: Context): NextEventInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null) ?: return null
        val date = prefs.getString(KEY_DATE, null) ?: return null
        return NextEventInfo(
            title = title,
            dateDisplay = date,
            time = prefs.getString(KEY_TIME, null),
            location = prefs.getString(KEY_LOCATION, null),
        )
    }
}
