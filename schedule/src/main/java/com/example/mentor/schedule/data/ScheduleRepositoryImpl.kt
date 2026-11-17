package com.example.mentor.schedule.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.mentor.schedule.domain.DuplicateScheduleEntryException
import com.example.mentor.data.ModuleDatabaseHelper
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository

import com.example.mentor.schedule.domain.TodoTask
import com.example.mentor.schedule.domain.TodoDates
import com.example.mentor.schedule.domain.ScheduleActivityLog

class ScheduleDatabaseHelper(context: Context) : ModuleDatabaseHelper(context, "schedule_schema", null, 4) {
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
                completed_at INTEGER,
                due_date TEXT,
                plan_start_date TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_account_day ON todo_tasks(account_id, day_of_week)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_account_due_date ON todo_tasks(account_id, due_date)")

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
        ensureUniqueScheduleIndexIfSafe(db)
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
        if (oldVersion < 3) ensureUniqueScheduleIndexIfSafe(db)
        if (oldVersion < 4) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS todo_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT, account_id TEXT NOT NULL,
                title TEXT NOT NULL, is_completed INTEGER NOT NULL DEFAULT 0,
                day_of_week INTEGER NOT NULL, priority INTEGER NOT NULL DEFAULT 1,
                linked_subject TEXT, created_at INTEGER NOT NULL, completed_at INTEGER
            )""".trimIndent())
            db.execSQL("ALTER TABLE todo_tasks ADD COLUMN due_date TEXT")
            db.execSQL("ALTER TABLE todo_tasks ADD COLUMN plan_start_date TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_account_due_date ON todo_tasks(account_id, due_date)")
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
            ensureUniqueScheduleIndexIfSafe(db)
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

    /** Replaces entries only on the days represented by the reviewed photo. */
    override fun replaceEntriesForImportedDays(
        accountId: String,
        entries: List<ScheduleEntry>
    ): com.example.mentor.schedule.domain.ScheduleReplaceResult {
        require(accountId.toIntOrNull()?.let { it > 0 } == true)
        require(entries.isNotEmpty() && entries.size <= 70)
        require(entries.all {
            it.accountId == accountId && it.dayOfWeek in 1..7 && it.period in 1..10 &&
                it.title.isNotBlank() && it.title.length <= 120
        })
        require(entries.distinctBy { it.dayOfWeek to it.period }.size == entries.size)
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            ensureUniqueScheduleIndexIfSafe(db)
            val days = entries.map { it.dayOfWeek }.distinct()
            val removed = days.sumOf { day ->
                db.delete(
                    "schedule_entries",
                    "account_id = ? AND day_of_week = ?",
                    arrayOf(accountId, day.toString())
                )
            }
            entries.forEach { entry ->
                val values = ContentValues().apply {
                    put("account_id", accountId)
                    put("title", entry.title.trim())
                    put("day_of_week", entry.dayOfWeek)
                    put("period", entry.period)
                }
                db.insertOrThrow("schedule_entries", null, values)
            }
            db.setTransactionSuccessful()
            return com.example.mentor.schedule.domain.ScheduleReplaceResult(removed, entries.size)
        } finally {
            db.endTransaction()
        }
    }

    override fun importTodos(accountId: String, tasks: List<TodoTask>): Int {
        require(accountId.toIntOrNull()?.let { it > 0 } == true)
        require(tasks.size in 1..21 && tasks.all {
            it.accountId == accountId && it.id == 0L && !it.isCompleted &&
                it.dayOfWeek in 1..7 && it.priority in 0..2 &&
                it.title.isNotBlank() && it.title.length <= 160 && it.title.none(Char::isISOControl) &&
                (it.linkedSubject?.length ?: 0) <= 80 && it.linkedSubject.orEmpty().none(Char::isISOControl)
        })
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var inserted = 0
            tasks.distinctBy { Triple(it.dueDate ?: "legacy-" + it.dayOfWeek,
                it.title.trim().lowercase(), it.linkedSubject?.trim()?.lowercase()) }
                .forEach { task ->
                    val title = task.title.trim()
                    val subject = task.linkedSubject?.trim()?.ifEmpty { null }
                    val exists = db.rawQuery(
                        "SELECT 1 FROM todo_tasks WHERE account_id=? AND coalesce(due_date, 'legacy-' || day_of_week)=? AND lower(trim(title))=? AND coalesce(lower(trim(linked_subject)),'')=? LIMIT 1",
                        arrayOf(accountId, task.dueDate ?: "legacy-" + task.dayOfWeek, title.lowercase(), subject?.lowercase() ?: "")
                    ).use { it.moveToFirst() }
                    if (!exists) {
                        val values = ContentValues().apply {
                            put("account_id", accountId)
                            put("title", title)
                            put("is_completed", 0)
                            put("day_of_week", task.dayOfWeek)
                            put("priority", task.priority)
                            put("linked_subject", subject)
                            put("created_at", System.currentTimeMillis())
                            put("due_date", task.dueDate)
                            put("plan_start_date", task.planStartDate)
                        }
                        db.insertOrThrow("todo_tasks", null, values)
                        inserted++
                    }
                }
            db.setTransactionSuccessful()
            return inserted
        } finally { db.endTransaction() }
    }

    override fun close() { dbHelper.close() }

    override fun insertEntry(entry: ScheduleEntry): Long =
        persistEntries(listOf(entry), isEdit = false).single()

    override fun updateEntry(entry: ScheduleEntry) {
        persistEntries(listOf(entry), isEdit = true)
    }

    override fun saveEntries(entries: List<ScheduleEntry>, isEdit: Boolean) {
        persistEntries(entries, isEdit)
    }

    private fun persistEntries(entries: List<ScheduleEntry>, isEdit: Boolean): List<Long> {
        require(entries.isNotEmpty())
        if (isEdit) require(entries.size == 1)
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            ensureUniqueScheduleIndexIfSafe(db)
            val duplicateInput = entries.groupBy { it.accountId to (it.dayOfWeek to it.period) }
                .values.firstOrNull { it.size > 1 }
            if (duplicateInput != null) {
                val first = duplicateInput.first()
                throw DuplicateScheduleEntryException(first.dayOfWeek, first.period, duplicateInput)
            }

            val conflicts = entries.flatMap { entry ->
                val rows = mutableListOf<ScheduleEntry>()
                db.rawQuery(
                    "SELECT id, account_id, title, day_of_week, period, teacher, notes " +
                        "FROM schedule_entries WHERE account_id=? AND day_of_week=? AND period=? " +
                        if (isEdit) "AND id<>?" else "",
                    if (isEdit) arrayOf(
                        entry.accountId,
                        entry.dayOfWeek.toString(),
                        entry.period.toString(),
                        entry.id.toString()
                    ) else arrayOf(
                        entry.accountId,
                        entry.dayOfWeek.toString(),
                        entry.period.toString()
                    )
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        rows.add(
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
                rows
            }
            conflicts.firstOrNull()?.let { duplicate ->
                throw DuplicateScheduleEntryException(
                    duplicate.dayOfWeek,
                    duplicate.period,
                    conflicts.filter { it.dayOfWeek == duplicate.dayOfWeek && it.period == duplicate.period }
                )
            }

            val ids = entries.map { entry ->
                val values = ContentValues().apply {
                    put("account_id", entry.accountId)
                    put("title", entry.title)
                    put("day_of_week", entry.dayOfWeek)
                    put("period", entry.period)
                    put("teacher", entry.teacher)
                    put("notes", entry.notes)
                }
                if (isEdit) {
                    db.update(
                        "schedule_entries",
                        values,
                        "id = ? AND account_id = ?",
                        arrayOf(entry.id.toString(), entry.accountId)
                    )
                    entry.id
                } else {
                    db.insertOrThrow("schedule_entries", null, values)
                }
            }
            db.setTransactionSuccessful()
            return ids
        } finally {
            db.endTransaction()
        }
    }

    override fun deleteEntry(id: Long, accountId: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("schedule_entries", "id = ? AND account_id = ?", arrayOf(id.toString(), accountId))
            ensureUniqueScheduleIndexIfSafe(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun getTodos(accountId: String, dayOfWeek: Int): List<TodoTask> {
        val list = mutableListOf<TodoTask>()
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM todo_tasks WHERE account_id = ? AND (due_date = ? OR (due_date IS NULL AND day_of_week = ?)) ORDER BY is_completed ASC, priority DESC",
            arrayOf(accountId, TodoDates.nextOccurrence(dayOfWeek), dayOfWeek.toString())
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
                        completedAt = completedAt,
                        dueDate = cursor.getColumnIndex("due_date").takeIf { it >= 0 && !cursor.isNull(it) }
                            ?.let(cursor::getString),
                        planStartDate = cursor.getColumnIndex("plan_start_date").takeIf { it >= 0 && !cursor.isNull(it) }
                            ?.let(cursor::getString)
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
            put("due_date", task.dueDate)
            put("plan_start_date", task.planStartDate)
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
            put("due_date", task.dueDate)
            put("plan_start_date", task.planStartDate)
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

private fun ensureUniqueScheduleIndexIfSafe(db: SQLiteDatabase) {
    val hasDuplicateSlots = db.rawQuery(
        "SELECT 1 FROM schedule_entries " +
            "GROUP BY account_id, day_of_week, period HAVING COUNT(*) > 1 LIMIT 1",
        null
    ).use { it.moveToFirst() }
    if (!hasDuplicateSlots) {
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_schedule_account_day_period " +
                "ON schedule_entries(account_id, day_of_week, period)"
        )
    }
}
