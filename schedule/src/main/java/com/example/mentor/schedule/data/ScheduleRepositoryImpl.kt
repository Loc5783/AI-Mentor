package com.example.mentor.schedule.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.mentor.data.ModuleDatabaseHelper
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository

import com.example.mentor.schedule.domain.TodoTask
import com.example.mentor.schedule.domain.ScheduleActivityLog

class ScheduleDatabaseHelper(context: Context) : ModuleDatabaseHelper(context, "schedule_schema", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS schedule_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id TEXT NOT NULL,
                title TEXT NOT NULL,
                day_of_week INTEGER NOT NULL,
                period INTEGER NOT NULL,
                teacher TEXT,
                notes TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_schedule_account_day ON schedule_entries(account_id, day_of_week)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS todo_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id TEXT NOT NULL,
                title TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                day_of_week INTEGER NOT NULL,
                priority INTEGER NOT NULL DEFAULT 1,
                linked_subject TEXT,
                created_at INTEGER NOT NULL,
                completed_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_account_day ON todo_tasks(account_id, day_of_week)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id TEXT NOT NULL,
                action_type TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_account ON activity_logs(account_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Upgrade to v2: Add new tables
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS todo_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    day_of_week INTEGER NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 1,
                    linked_subject TEXT,
                    created_at INTEGER NOT NULL,
                    completed_at INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_account_day ON todo_tasks(account_id, day_of_week)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_account ON activity_logs(account_id)")
        }
    }
}

class ScheduleRepositoryImpl(context: Context) : ScheduleRepository, com.example.mentor.schedule.domain.ScheduleImportRepository {
    
    private val dbHelper = ScheduleDatabaseHelper(context)
    
    override fun getEntries(accountId: String, dayOfWeek: Int): List<ScheduleEntry> {
        val list = mutableListOf<ScheduleEntry>()
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM schedule_entries WHERE account_id = ? AND day_of_week = ? ORDER BY period ASC",
            arrayOf(accountId, dayOfWeek.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ScheduleEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        accountId = cursor.getString(cursor.getColumnIndexOrThrow("account_id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        dayOfWeek = cursor.getInt(cursor.getColumnIndexOrThrow("day_of_week")),
                        period = cursor.getInt(cursor.getColumnIndexOrThrow("period")),
                        teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    /** Atomic additive import. Existing occupied slots are never overwritten. */
    override fun importVacantEntries(accountId: String, entries: List<ScheduleEntry>): Int {
        require(accountId.toIntOrNull()?.let { it > 0 } == true)
        require(entries.isNotEmpty() && entries.size <= 70)
        require(entries.all { it.accountId == accountId && it.dayOfWeek in 1..7 && it.period in 1..10 && it.title.isNotBlank() && it.title.length <= 120 })
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var inserted = 0
            entries.distinctBy { it.dayOfWeek to it.period }.forEach { entry ->
                val occupied = db.rawQuery("SELECT 1 FROM schedule_entries WHERE account_id=? AND day_of_week=? AND period=? LIMIT 1",
                    arrayOf(accountId, entry.dayOfWeek.toString(), entry.period.toString())).use { it.moveToFirst() }
                if (!occupied) {
                    val values = ContentValues().apply {
                        put("account_id", accountId); put("title", entry.title)
                        put("day_of_week", entry.dayOfWeek); put("period", entry.period)
                    }
                    db.insertOrThrow("schedule_entries", null, values)
                    inserted++
                }
            }
            db.setTransactionSuccessful()
            return inserted
        } finally { db.endTransaction() }
    }

    override fun close() { dbHelper.close() }

    override fun insertEntry(entry: ScheduleEntry): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("account_id", entry.accountId)
            put("title", entry.title)
            put("day_of_week", entry.dayOfWeek)
            put("period", entry.period)
            put("teacher", entry.teacher)
            put("notes", entry.notes)
        }
        return db.insert("schedule_entries", null, cv)
    }

    override fun updateEntry(entry: ScheduleEntry) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", entry.title)
            put("day_of_week", entry.dayOfWeek)
            put("period", entry.period)
            put("teacher", entry.teacher)
            put("notes", entry.notes)
        }
        db.update("schedule_entries", cv, "id = ? AND account_id = ?", arrayOf(entry.id.toString(), entry.accountId))
    }

    override fun deleteEntry(id: Long, accountId: String) {
        val db = dbHelper.writableDatabase
        db.delete("schedule_entries", "id = ? AND account_id = ?", arrayOf(id.toString(), accountId))
    }

    override fun getTodos(accountId: String, dayOfWeek: Int): List<TodoTask> {
        val list = mutableListOf<TodoTask>()
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM todo_tasks WHERE account_id = ? AND day_of_week = ? ORDER BY is_completed ASC, priority DESC",
            arrayOf(accountId, dayOfWeek.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val completedAtIdx = cursor.getColumnIndexOrThrow("completed_at")
                val completedAt = if (cursor.isNull(completedAtIdx)) null else cursor.getLong(completedAtIdx)
                val linkedSubjectIdx = cursor.getColumnIndexOrThrow("linked_subject")
                val linkedSubject = if (cursor.isNull(linkedSubjectIdx)) null else cursor.getString(linkedSubjectIdx)
                
                list.add(
                    TodoTask(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        accountId = cursor.getString(cursor.getColumnIndexOrThrow("account_id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1,
                        dayOfWeek = cursor.getInt(cursor.getColumnIndexOrThrow("day_of_week")),
                        priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority")),
                        linkedSubject = linkedSubject,
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        completedAt = completedAt
                    )
                )
            }
        }
        return list
    }

    override fun insertTodo(task: TodoTask): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("account_id", task.accountId)
            put("title", task.title)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("day_of_week", task.dayOfWeek)
            put("priority", task.priority)
            put("linked_subject", task.linkedSubject)
            put("created_at", task.createdAt)
            put("completed_at", task.completedAt)
        }
        return db.insert("todo_tasks", null, cv)
    }

    override fun updateTodo(task: TodoTask) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", task.title)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("day_of_week", task.dayOfWeek)
            put("priority", task.priority)
            put("linked_subject", task.linkedSubject)
            put("completed_at", task.completedAt)
        }
        db.update("todo_tasks", cv, "id = ? AND account_id = ?", arrayOf(task.id.toString(), task.accountId))
    }

    override fun deleteTodo(id: Long, accountId: String) {
        val db = dbHelper.writableDatabase
        db.delete("todo_tasks", "id = ? AND account_id = ?", arrayOf(id.toString(), accountId))
    }

    override fun updateTodoStatus(id: Long, accountId: String, isCompleted: Boolean, completedAt: Long?) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_completed", if (isCompleted) 1 else 0)
            put("completed_at", completedAt)
        }
        db.update("todo_tasks", cv, "id = ? AND account_id = ?", arrayOf(id.toString(), accountId))
    }

    override fun getLogs(accountId: String, limit: Int): List<ScheduleActivityLog> {
        val list = mutableListOf<ScheduleActivityLog>()
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM activity_logs WHERE account_id = ? ORDER BY timestamp DESC LIMIT ?",
            arrayOf(accountId, limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ScheduleActivityLog(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        accountId = cursor.getString(cursor.getColumnIndexOrThrow("account_id")),
                        actionType = cursor.getString(cursor.getColumnIndexOrThrow("action_type")),
                        entityType = cursor.getString(cursor.getColumnIndexOrThrow("entity_type")),
                        description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return list
    }

    override fun insertLog(log: ScheduleActivityLog) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("account_id", log.accountId)
            put("action_type", log.actionType)
            put("entity_type", log.entityType)
            put("description", log.description)
            put("timestamp", log.timestamp)
        }
        db.insert("activity_logs", null, cv)
    }

    override fun clearLogs(accountId: String) {
        val db = dbHelper.writableDatabase
        db.delete("activity_logs", "account_id = ?", arrayOf(accountId))
    }
}