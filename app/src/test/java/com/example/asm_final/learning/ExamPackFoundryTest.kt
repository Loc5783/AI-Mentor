package com.example.asm_final.learning

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ExamPackFoundryTest {
    private fun pack() = ExamPackFoundry.ExamPack(
        userId = 101, title = "Đề Sinh 11", subjectId = "biology", grade = 11,
        sourceFiles = listOf(ExamPackFoundry.ExamPackFile("source.pdf", pageCount = 4))
    )
    private fun question(pack: ExamPackFoundry.ExamPack, prompt: String = "2 × 5 = ?") =
        ExamPackFoundry.ExtractedQuestion(
            packId = pack.id, userId = pack.userId, prompt = prompt,
            options = linkedMapOf("A" to "8", "B" to "10", "C" to "12", "D" to "14"),
            correctOption = "A", explanation = "2 × 5 = 10", topic = "Topic",
            sourceFileName = "source.pdf", sourcePageIndex = 2
        )
    private fun import(pack: ExamPackFoundry.ExamPack) =
        ExamPackFoundry.processExtraction(pack, listOf(question(pack))).single()

    @Test fun reorderedMapsAndWhitespaceAreExactDuplicates() {
        val p = pack()
        val q = question(p)
        ExamPackFoundry.processExtraction(p, listOf(q))
        val reordered = q.copy(prompt = "  2  × 5 = ? ", options = q.options.entries.reversed().associate { it.toPair() })
        assertTrue(ExamPackFoundry.processExtraction(p, listOf(reordered)).isEmpty())
    }

    @Test fun numbersCaseAndOptionLabelsAreNotDiscarded() {
        val p = pack()
        val q = question(p)
        assertEquals(4, ExamPackFoundry.processExtraction(p, listOf(
            q, q.copy(prompt = "2 × 6 = ?"), q.copy(prompt = "mM"),
            q.copy(prompt = "mm")
        )).size)
        assertNotEquals(q.exactFingerprint, q.copy(options = q.options + ("A" to "10") + ("B" to "8")).exactFingerprint)
    }

    @Test fun fingerprintsCannotCollideThroughSeparators() {
        val p = pack()
        val q = question(p)
        assertNotEquals(q.copy(prompt = "x||A", options = mapOf("A" to "B")).exactFingerprint,
            q.copy(prompt = "x", options = mapOf("A" to "A||B")).exactFingerprint)
    }

    @Test fun aiCannotApproveOrAssignIdentity() {
        val p = pack()
        val raw = question(p).copy(userId = 999, packId = "foreign", isApproved = true, isAnswerVerified = true)
        val stored = ExamPackFoundry.processExtraction(p, listOf(raw)).single()
        assertFalse(stored.isApproved)
        assertFalse(stored.isAnswerVerified)
        assertEquals(p.userId, stored.userId)
        assertEquals(p.id, stored.packId)
        assertNotEquals(raw.id, stored.id)
        assertFalse(ExamPackFoundry.finalizePack(p).isApproved)
    }

    @Test fun reviewReplacesStoredAnswerExplanationAndPreservesSource() {
        val p = pack()
        val raw = import(p)
        val reviewed = ExamPackFoundry.reviewQuestion(p, raw.id, "B", editedExplanation = "Reviewed explanation")
        assertEquals(reviewed, p.questions.single())
        assertEquals("B", p.questions.single().correctOption)
        assertEquals("Reviewed explanation", reviewed.explanation)
        assertEquals(raw.sourceFileName, reviewed.sourceFileName)
        assertEquals(raw.sourcePageIndex, reviewed.sourcePageIndex)
        assertFalse(raw.isApproved)
        assertTrue(ExamPackFoundry.finalizePack(p).isApproved)
        assertEquals(listOf(reviewed), ExamPackFoundry.playableQuestions(p))
    }

    @Test fun missingInvalidOrImplicitAnswerCannotBeApproved() {
        val q = question(pack())
        listOf(null, "", "Z").forEach {
            val result = ExamPackFoundry.approveQuestion(q, it)
            assertFalse(result.isApproved)
            assertFalse(result.isAnswerVerified)
        }
    }

    @Test fun incompleteAndDuplicateOptionsCannotBeApproved() {
        val q = question(pack())
        listOf(
            q.copy(prompt = ""), q.copy(explanation = ""), q.copy(topic = ""),
            q.copy(sourceFileName = ""), q.copy(sourcePageIndex = 0),
            q.copy(options = q.options - "D"), q.copy(options = q.options + ("D" to "")),
            q.copy(options = q.options + ("D" to " 10 ")),
            q.copy(options = q.options + ("E" to "16"))
        ).forEach { assertFalse(ExamPackFoundry.approveQuestion(it, "B").isApproved) }
        assertFalse(ExamPackFoundry.approveQuestion(q, "B", editedPrompt = "").isApproved)
        assertFalse(ExamPackFoundry.approveQuestion(q, "B", editedExplanation = "").isApproved)
    }

    @Test fun mixedPackAndNewImportInvalidatePublication() {
        val p = pack()
        val q = import(p)
        ExamPackFoundry.reviewQuestion(p, q.id, "B")
        assertTrue(ExamPackFoundry.finalizePack(p).isApproved)
        ExamPackFoundry.processExtraction(p, listOf(question(p, "2 × 6 = ?")))
        assertFalse(p.isApproved)
        assertThrows(IllegalStateException::class.java) { ExamPackFoundry.playableQuestions(p) }
    }

    @Test fun emptyStaleForeignAndInvalidProvenancePacksFailClosed() {
        val p = pack()
        p.isApproved = true
        assertFalse(ExamPackFoundry.finalizePack(p).isApproved)
        val q = ExamPackFoundry.approveQuestion(question(p), "B")
        listOf(q.copy(userId = 99), q.copy(packId = "foreign"),
            q.copy(sourcePageIndex = 5), q.copy(sourceFileName = "missing.pdf"),
            q.copy(isAnswerVerified = false)).forEach {
            p.questions.clear()
            p.questions.add(it)
            p.isApproved = true
            assertThrows(IllegalStateException::class.java) { ExamPackFoundry.playableQuestions(p) }
        }
    }

    @Test fun editingCannotCreateDuplicateOrReviewForeignQuestion() {
        val p = pack()
        val rows = ExamPackFoundry.processExtraction(p, listOf(question(p), question(p, "Other")))
        assertThrows(IllegalArgumentException::class.java) {
            ExamPackFoundry.reviewQuestion(p, rows[1].id, "B", editedPrompt = rows[0].prompt)
        }
        assertEquals("Other", p.questions[1].prompt)
        assertThrows(IllegalArgumentException::class.java) { ExamPackFoundry.reviewQuestion(p, "unknown", "B") }
    }

    @Test fun editsInvalidatePackAndDoNotRetainApprovalOnInvalidContent() {
        val p = pack()
        val q = import(p)
        ExamPackFoundry.reviewQuestion(p, q.id, "B")
        ExamPackFoundry.finalizePack(p)
        ExamPackFoundry.reviewQuestion(p, q.id, "B", editedExplanation = "")
        assertFalse(p.isApproved)
        assertFalse(p.questions.single().isApproved)
    }

    @Test fun fingerprintDoesNotDependOnDeviceLocale() {
        val q = question(pack(), "Intensity I")
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val fingerprint = q.exactFingerprint
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(fingerprint, q.exactFingerprint)
        } finally { Locale.setDefault(previous) }
    }
}
