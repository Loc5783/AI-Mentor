package com.example.mentor.schedule.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mentor.schedule.domain.ScheduleEntry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleRepositoryTest {

    private lateinit var repository: ScheduleRepositoryImpl
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        repository = ScheduleRepositoryImpl(context)
        // clean up before
        context.deleteDatabase("AIMentorSchool.db")
    }

    @After
    fun teardown() {
        context.deleteDatabase("AIMentorSchool.db")
    }

    @Test
    fun testCrudAndIsolation() {
        val account1 = "acc1"
        val account2 = "acc2"
        
        val entry1 = ScheduleEntry(accountId = account1, title = "Math", dayOfWeek = 1, startTimeMin = 600, endTimeMin = 660)
        val id1 = repository.insertEntry(entry1)
        
        val entry2 = ScheduleEntry(accountId = account2, title = "Physics", dayOfWeek = 1, startTimeMin = 600, endTimeMin = 660)
        val id2 = repository.insertEntry(entry2)
        
        // Isolation
        val entries1 = repository.getEntries(account1, 1)
        assertEquals(1, entries1.size)
        assertEquals("Math", entries1[0].title)
        
        val entries2 = repository.getEntries(account2, 1)
        assertEquals(1, entries2.size)
        assertEquals("Physics", entries2[0].title)
        
        // Update
        repository.updateEntry(entries1[0].copy(title = "Advanced Math"))
        val updated = repository.getEntries(account1, 1)
        assertEquals("Advanced Math", updated[0].title)
        
        // Delete
        repository.deleteEntry(id1, account1)
        assertTrue(repository.getEntries(account1, 1).isEmpty())
        
        // Ensure account2 still has its entry
        assertEquals(1, repository.getEntries(account2, 1).size)
    }

    @Test
    fun testSorting() {
        val account1 = "acc1"
        repository.insertEntry(ScheduleEntry(accountId = account1, title = "Late", dayOfWeek = 1, startTimeMin = 800, endTimeMin = 860))
        repository.insertEntry(ScheduleEntry(accountId = account1, title = "Early", dayOfWeek = 1, startTimeMin = 600, endTimeMin = 660))
        
        val entries = repository.getEntries(account1, 1)
        assertEquals("Early", entries[0].title)
        assertEquals("Late", entries[1].title)
    }
}