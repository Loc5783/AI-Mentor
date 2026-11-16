package com.example.asm_final.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Persists and tracks active study time locally on the device per user.
 */
object LocalStudyTimeStore {
    private const val PREFS_NAME = "study_time_tracker_prefs"
    private const val KEY_PREFIX_SECONDS = "study_seconds_user_"

    private var activeStartTime: Long = 0L

    @JvmStatic
    fun startSession() {
        activeStartTime = System.currentTimeMillis()
    }

    @JvmStatic
    fun recordActiveSession(context: Context, ownerId: Int) {
        if (ownerId <= 0) return
        val now = System.currentTimeMillis()
        if (activeStartTime > 0L) {
            val elapsedMs = now - activeStartTime
            if (elapsedMs > 0) {
                val seconds = elapsedMs / 1000L
                if (seconds in 1..86400) {
                    addStudySeconds(context, ownerId, seconds)
                }
            }
        }
        activeStartTime = now
    }

    @JvmStatic
    fun addStudySeconds(context: Context, ownerId: Int, seconds: Long) {
        if (ownerId <= 0 || seconds <= 0) return
        val current = getStudySeconds(context, ownerId)
        val updated = current + seconds
        prefs(context).edit().putLong("$KEY_PREFIX_SECONDS$ownerId", updated).apply()
    }

    @JvmStatic
    fun getStudySeconds(context: Context, ownerId: Int): Long {
        if (ownerId <= 0) return 0L
        return prefs(context).getLong("$KEY_PREFIX_SECONDS$ownerId", 0L)
    }

    @JvmStatic
    fun getSummaryJsonObject(context: Context, ownerId: Int, cloudSummary: JSONObject?): JSONObject {
        val localSecs = getStudySeconds(context, ownerId)
        val cloudSecs = cloudSummary?.optLong("totalSeconds") ?: 0L
        val totalSecs = maxOf(localSecs, cloudSecs)

        val result = JSONObject()
        result.put("totalSeconds", totalSecs)
        if (cloudSummary != null && cloudSummary.has("week")) {
            result.put("week", cloudSummary.getJSONArray("week"))
        }
        return result
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
