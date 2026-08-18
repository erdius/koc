package com.erdman.kofc6650.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.DateFormat
import java.util.Date

/**
 * Persists the last successful fetch of events/photos so a network failure
 * shows the user's last-known data instead of a dead-end error card. Not a
 * general-purpose cache -- just enough to keep the app useful when offline.
 */
class OfflineCache(context: Context) {
    private val prefs = context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val eventListAdapter = moshi.adapter<List<EventDto>>(
        Types.newParameterizedType(List::class.java, EventDto::class.java),
    )
    private val photoListAdapter = moshi.adapter<List<RecentPhotoDto>>(
        Types.newParameterizedType(List::class.java, RecentPhotoDto::class.java),
    )

    data class CachedEvents(val signupEvents: List<EventDto>, val allEvents: List<EventDto>)

    fun saveEvents(signupEvents: List<EventDto>, allEvents: List<EventDto>) {
        prefs.edit()
            .putString(KEY_SIGNUP_EVENTS, eventListAdapter.toJson(signupEvents))
            .putString(KEY_ALL_EVENTS, eventListAdapter.toJson(allEvents))
            .putLong(KEY_EVENTS_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun loadEvents(): CachedEvents? {
        val signupJson = prefs.getString(KEY_SIGNUP_EVENTS, null) ?: return null
        val allJson = prefs.getString(KEY_ALL_EVENTS, null) ?: return null
        val signup = runCatching { eventListAdapter.fromJson(signupJson) }.getOrNull() ?: return null
        val all = runCatching { eventListAdapter.fromJson(allJson) }.getOrNull() ?: return null
        return CachedEvents(signup, all)
    }

    fun savePhotos(photos: List<RecentPhotoDto>) {
        prefs.edit()
            .putString(KEY_PHOTOS, photoListAdapter.toJson(photos))
            .putLong(KEY_PHOTOS_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun loadPhotos(): List<RecentPhotoDto>? {
        val json = prefs.getString(KEY_PHOTOS, null) ?: return null
        return runCatching { photoListAdapter.fromJson(json) }.getOrNull()
    }

    fun relativeEventsSavedAt(): String = relativeTime(prefs.getLong(KEY_EVENTS_SAVED_AT, 0L))

    fun relativePhotosSavedAt(): String = relativeTime(prefs.getLong(KEY_PHOTOS_SAVED_AT, 0L))

    // Newest photo first (server returns them reverse-chronological), so
    // comparing just the first id against what was last seen is enough to
    // know whether anything new has shown up.
    fun hasNewPhotos(photos: List<RecentPhotoDto>): Boolean {
        val latest = photos.firstOrNull()?.id ?: return false
        return latest != prefs.getString(KEY_LAST_SEEN_PHOTO_ID, null)
    }

    fun markPhotosSeen(photos: List<RecentPhotoDto>) {
        val latest = photos.firstOrNull()?.id ?: return
        prefs.edit().putString(KEY_LAST_SEEN_PHOTO_ID, latest).apply()
    }

    private fun relativeTime(savedAtMillis: Long): String {
        if (savedAtMillis == 0L) return "earlier"
        val diffMinutes = (System.currentTimeMillis() - savedAtMillis) / 60_000
        return when {
            diffMinutes < 1 -> "just now"
            diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
            diffMinutes < 60 * 24 -> {
                val hours = diffMinutes / 60
                "$hours hour${if (hours == 1L) "" else "s"} ago"
            }
            else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(savedAtMillis))
        }
    }

    private companion object {
        const val KEY_SIGNUP_EVENTS = "signup_events"
        const val KEY_ALL_EVENTS = "all_events"
        const val KEY_PHOTOS = "photos"
        const val KEY_EVENTS_SAVED_AT = "events_saved_at"
        const val KEY_PHOTOS_SAVED_AT = "photos_saved_at"
        const val KEY_LAST_SEEN_PHOTO_ID = "last_seen_photo_id"
    }
}
