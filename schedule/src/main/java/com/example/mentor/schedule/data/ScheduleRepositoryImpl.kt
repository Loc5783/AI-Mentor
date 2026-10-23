package com.example.mentor.schedule.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.mentor.data.ModuleDatabaseHelper
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository

class ScheduleDatabaseHelper(context: Context) : ModuleDatabaseHelper(context, "schedule_schema", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS schedule_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id TEXT NOT NULL,
                title TEXT NOT NULL,
                day_of_week INTEGER NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                room TEXT,
                teacher TEXT,
                notes TEXT,
                color_label INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_schedule_account_day ON schedule_entries(account_id, day_of_week)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No upgrade logic needed for v1
    }
}

class ScheduleRepositoryImpl(private val context: Context) : ScheduleRepository {
    
    private val dbHelper = ScheduleDatabaseHelper(context)
    
    override fun getEntries(accountId: String, dayOfWeek: Int): List<ScheduleEntry> {
        val list = mutableListOf<ScheduleEntry>()
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM schedule_entries WHERE account_id = ? AND day_of_week = ? ORDER BY start_time ASC",
            arrayOf(accountId, dayOfWeek.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ScheduleEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        accountId = cursor.getString(cursor.getColumnIndexOrThrow("account_id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        dayOfWeek = cursor.getInt(cursor.getColumnIndexOrThrow("day_of_week")),
                        startTimeMin = cursor.getInt(cursor.getColumnIndexOrThrow("start_time")),
                        endTimeMin = cursor.getInt(cursor.getColumnIndexOrThrow("end_time")),
                        room = cursor.getString(cursor.getColumnIndexOrThrow("room")),
                        teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                        colorLabel = cursor.getInt(cursor.getColumnIndexOrThrow("color_label"))
                    )
                )
            }
        }
        return list
    }

    override fun getNextEntryToday(accountId: String, dayOfWeek: Int, currentMinutes: Int): ScheduleEntry? {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM schedule_entries WHERE account_id = ? AND day_of_week = ? AND end_time > ? ORDER BY start_time ASC LIMIT 1",
            arrayOf(accountId, dayOfWeek.toString(), currentMinutes.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return ScheduleEntry(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    accountId = cursor.getString(cursor.getColumnIndexOrThrow("account_id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    dayOfWeek = cursor.getInt(cursor.getColumnIndexOrThrow("day_of_week")),
                    startTimeMin = cursor.getInt(cursor.getColumnIndexOrThrow("start_time")),
                    endTimeMin = cursor.getInt(cursor.getColumnIndexOrThrow("end_time")),
                    room = cursor.getString(cursor.getColumnIndexOrThrow("room")),
                    teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                    colorLabel = cursor.getInt(cursor.getColumnIndexOrThrow("color_label"))
                )
            }
        }
        return null
    }

    override fun insertEntry(entry: ScheduleEntry): Long {
        require(entry.endTimeMin > entry.startTimeMin) { "End time must be after start time" }
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("account_id", entry.accountId)
            put("title", entry.title)
            put("day_of_week", entry.dayOfWeek)
            put("start_time", entry.startTimeMin)
            put("end_time", entry.endTimeMin)
            put("room", entry.room)
            put("teacher", entry.teacher)
            put("notes", entry.notes)
            put("color_label", entry.colorLabel)
        }
        return db.insert("schedule_entries", null, cv)
    }

    override fun updateEntry(entry: ScheduleEntry) {
        require(entry.endTimeMin > entry.startTimeMin) { "End time must be after start time" }
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", entry.title)
            put("day_of_week", entry.dayOfWeek)
            put("start_time", entry.startTimeMin)
            put("end_time", entry.endTimeMin)
            put("room", entry.room)
            put("teacher", entry.teacher)
            put("notes", entry.notes)
            put("color_label", entry.colorLabel)
        }
        db.update("schedule_entries", cv, "id = ? AND account_id = ?", arrayOf(entry.id.toString(), entry.accountId))
    }

    override fun deleteEntry(id: Long, accountId: String) {
        val db = dbHelper.writableDatabase
        db.delete("schedule_entries", "id = ? AND account_id = ?", arrayOf(id.toString(), accountId))
    }
}