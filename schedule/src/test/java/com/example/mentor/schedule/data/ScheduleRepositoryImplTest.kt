package com.example.mentor.schedule.data

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import com.example.mentor.schedule.domain.DuplicateScheduleEntryException
import com.example.mentor.schedule.domain.ScheduleEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScheduleRepositoryImplTest {
    private lateinit var databaseFile: File
    private lateinit var repository: ScheduleRepositoryImpl
    private lateinit var databaseContext: Context

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        databaseFile = File(application.cacheDir, "schedule-test-${UUID.randomUUID()}.db")
        databaseContext = object : ContextWrapper(application) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = databaseFile
        }
        repository = ScheduleRepositoryImpl(databaseContext)
    }

    @After
    fun tearDown() {
        repository.close()
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
        databaseFile.delete()
    }

    @Test
    fun rejectsDuplicateInsertAndAllowsEditingSameEntry() {
        val original = entry("Math", day = 2, period = 4)
        val id = repository.insertEntry(original)

        val duplicate = assertThrows(DuplicateScheduleEntryException::class.java) {
            repository.insertEntry(entry("Physics", day = 2, period = 4))
        }
        assertEquals(2, duplicate.dayOfWeek)
        assertEquals(4, duplicate.period)
        assertEquals(listOf("Math"), duplicate.existingEntries.map { it.title })

        repository.updateEntry(original.copy(id = id, title = "Advanced Math"))
        assertEquals(listOf("Advanced Math"), repository.getEntries("account", 2).map { it.title })
    }

    @Test
    fun rejectsBatchWithOccupiedSlotWithoutPartiallySavingOtherDays() {
        repository.insertEntry(entry("Existing", day = 3, period = 5))

        assertThrows(DuplicateScheduleEntryException::class.java) {
            repository.saveEntries(
                listOf(
                    entry("New Monday class", day = 1, period = 2),
                    entry("Conflicting class", day = 3, period = 5)
                ),
                isEdit = false
            )
        }

        assertTrue(repository.getEntries("account", 1).isEmpty())
        assertEquals(1, repository.getEntries("account", 3).size)
    }

    @Test
    fun duplicateSlotsAreScopedByAccountAndDistinctPeriodsRemainAllowed() {
        repository.insertEntry(entry("Account one", day = 2, period = 1))
        repository.insertEntry(entry("Account two", day = 2, period = 1).copy(accountId = "other"))

        (2..7).forEach { period ->
            repository.insertEntry(entry("Period $period", day = 2, period = period))
        }

        assertEquals((1..7).toList(), repository.getEntries("account", 2).map { it.period })
        assertEquals(listOf("Account two"), repository.getEntries("other", 2).map { it.title })
    }

    @Test
    fun concurrentInsertsForSameSlotPersistOnlyOneEntry() {
        val secondRepository = ScheduleRepositoryImpl(databaseContext)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attempts = listOf(repository, secondRepository).mapIndexed { index, repo ->
                executor.submit<Boolean> {
                    start.await()
                    try {
                        repo.insertEntry(entry("Subject $index", day = 4, period = 6))
                        true
                    } catch (_: DuplicateScheduleEntryException) {
                        false
                    }
                }
            }
            start.countDown()
            val saved = attempts.count { it.get(10, TimeUnit.SECONDS) }

            assertEquals(1, saved)
            assertEquals(1, repository.getEntries("account", 4).size)
        } finally {
            executor.shutdownNow()
            secondRepository.close()
        }
    }

    @Test
    fun upgradePreservesLegacyDuplicatesAndDoesNotFailCreatingUniqueIndex() {
        createLegacyVersionTwoDatabaseWithDuplicates()

        assertEquals(2, repository.getEntries("account", 5).size)
        assertFalse(hasUniqueScheduleIndex())
        repository.insertEntry(entry("Other period", day = 5, period = 7))
        assertEquals(3, repository.getEntries("account", 5).size)
        val legacyConflict = assertThrows(DuplicateScheduleEntryException::class.java) {
            repository.insertEntry(entry("Would be duplicate", day = 5, period = 6))
        }
        assertEquals(listOf("Legacy A", "Legacy B"), legacyConflict.existingEntries.map { it.title }.sorted())

        val duplicateEntries = repository.getEntries("account", 5).filter { it.period == 6 }
        val removedTitle = duplicateEntries.first().title
        repository.deleteEntry(duplicateEntries.first().id, "account")
        assertTrue(hasUniqueScheduleIndex())
        assertThrows(DuplicateScheduleEntryException::class.java) {
            repository.insertEntry(entry("No replacement", day = 5, period = 6))
        }
        val remaining = repository.getEntries("account", 5).filter { it.period == 6 }.map { it.title }
        assertEquals(1, remaining.size)
        assertTrue(remaining.single() != removedTitle)
    }

    private fun entry(title: String, day: Int, period: Int) = ScheduleEntry(
        accountId = "account",
        title = title,
        dayOfWeek = day,
        period = period
    )

    private fun createLegacyVersionTwoDatabaseWithDuplicates() {
        databaseFile.parentFile?.mkdirs()
        val database = SQLiteDatabase.openDatabase(
            databaseFile.path,
            null,
            SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.OPEN_READWRITE
        )
        try {
            database.execSQL(
                "CREATE TABLE mentor_schema_versions(module TEXT PRIMARY KEY, version INTEGER NOT NULL)"
            )
            database.execSQL(
                "INSERT INTO mentor_schema_versions(module, version) VALUES('schedule_schema', 2)"
            )
            database.execSQL(
                """
                CREATE TABLE schedule_entries (
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
            database.execSQL(
                "INSERT INTO schedule_entries(account_id,title,day_of_week,period) VALUES('account','Legacy A',5,6)"
            )
            database.execSQL(
                "INSERT INTO schedule_entries(account_id,title,day_of_week,period) VALUES('account','Legacy B',5,6)"
            )
        } finally {
            database.close()
        }
    }

    private fun hasUniqueScheduleIndex(): Boolean {
        val helper = ScheduleDatabaseHelper(databaseContext)
        try {
            return helper.readableDatabase.rawQuery("PRAGMA index_list(schedule_entries)", null).use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                val uniqueColumn = cursor.getColumnIndexOrThrow("unique")
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameColumn) == "idx_schedule_account_day_period" &&
                        cursor.getInt(uniqueColumn) == 1
                    ) found = true
                }
                found
            }
        } finally {
            helper.close()
        }
    }
}
