package com.example.asm_final.learning

import androidx.test.platform.app.InstrumentationRegistry
import com.example.mentor.learning.SubjectId
import org.junit.Assert.*
import org.junit.Test

class PathChoiceStoreDeviceTest {
    @Test fun choiceIsIsolatedByAccountGradeAndSubject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PathChoiceStore(context)
        val owner = 1_987_654_321
        assertTrue(store.markStartedFromBeginning(owner, 10, SubjectId.LITERATURE))
        assertTrue(store.startedFromBeginning(owner, 10, SubjectId.LITERATURE))
        assertFalse(store.startedFromBeginning(owner + 1, 10, SubjectId.LITERATURE))
        for (grade in 10..12) for (subject in SubjectId.values()) {
            if (grade != 10 || subject != SubjectId.LITERATURE)
                assertFalse("$grade/${subject.wireId} inherited another path's choice",
                    store.startedFromBeginning(owner, grade, subject))
        }
    }
}
