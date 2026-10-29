package com.example.asm_final.chat

import androidx.test.platform.app.InstrumentationRegistry
import com.example.mentor.schedule.data.ScheduleRepositoryImpl
import com.example.mentor.schedule.domain.ScheduleEntry
import org.junit.Assert.*
import org.junit.Test

class ScheduleImportDeviceTest {
    @Test fun additiveImportIsIdempotentAndIsolatedAfterReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = "1900000001"
        val other = "1900000002"
        var repository = ScheduleRepositoryImpl(context)
        fun clean() { for (owner in listOf(account,other)) for(day in 1..7) repository.getEntries(owner,day).forEach { repository.deleteEntry(it.id,owner) } }
        try {
            clean()
            val old = ScheduleEntry(accountId=account,title="Existing",dayOfWeek=1,period=1)
            repository.insertEntry(old)
            val rows = listOf(old.copy(title="Must not replace"), old.copy(period=2,title="Physics"))
            assertEquals(1,repository.importVacantEntries(account,rows))
            assertEquals(0,repository.importVacantEntries(account,rows))
            assertEquals(1,repository.importVacantEntries(other,listOf(old.copy(accountId=other,title="Other"))))
            repository.close()
            repository=ScheduleRepositoryImpl(context)
            assertEquals(listOf("Existing","Physics"),repository.getEntries(account,1).sortedBy { it.period }.map { it.title })
            assertEquals("Other",repository.getEntries(other,1).single().title)
            try {
                repository.importVacantEntries(account,listOf(old.copy(period=3),old.copy(period=11)))
                fail("Invalid batch accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(2,repository.getEntries(account,1).size)
        } finally { clean(); repository.close() }
    }
}
