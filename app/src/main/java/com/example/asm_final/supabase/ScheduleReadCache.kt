package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import org.json.JSONObject

/** Account- and project-scoped last known timetable/to-do reads. No writes are queued offline. */
internal class ScheduleReadCache(private val context: Context) {
    private fun key(owner: Int, op: String, data: JSONObject): String? {
        val remoteId = RemoteAccountLinks(context).use {
            it.remoteId(owner, BuildConfig.SUPABASE_URL)
        }.takeIf(String::isNotBlank) ?: return null
        val day = data.optInt("day", 0)
        val date = data.optString("date", "")
        return ScheduleCacheKey.of(BuildConfig.SUPABASE_URL, remoteId, owner, op, day, date)
    }

    fun save(owner: Int, op: String, data: JSONObject, value: JSONObject) {
        val key = key(owner, op, data) ?: return
        context.getSharedPreferences("schedule_read_cache", Context.MODE_PRIVATE)
            .edit().putString(key, value.toString()).apply()
    }

    fun load(owner: Int, op: String, data: JSONObject): JSONObject? {
        val key = key(owner, op, data) ?: return null
        val body = context.getSharedPreferences("schedule_read_cache", Context.MODE_PRIVATE)
            .getString(key, null) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }
}
