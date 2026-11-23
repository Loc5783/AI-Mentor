package com.example.asm_final.learning

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ExamPackCloudCodecTest {
    private fun pack() = ExamPackFoundry.ExamPack(userId = 42, title = "Đề Vật lý",
        subjectId = "physics", grade = 10,
        sourceFiles = listOf(ExamPackFoundry.ExamPackFile("de.pdf", 2)))

    private fun response(source: String = "de.pdf", page: Int = 2): JSONObject = JSONObject()
        .put("questions", org.json.JSONArray().put(JSONObject()
            .put("sourceFileName", source).put("sourcePageIndex", page)
            .put("prompt", "Lực F bằng bao nhiêu?")
            .put("options", JSONObject().put("A", "1 N").put("B", "2 N")
                .put("C", "3 N").put("D", "4 N"))
            .put("correctOption", JSONObject.NULL).put("explanation", JSONObject.NULL)
            .put("topic", JSONObject.NULL)))

    @Test fun extractionCreatesAccountOwnedUnapprovedDraft() {
        val pack = pack()
        ExamPackCloudCodec.attachDrafts(pack, response())
        assertEquals(1, pack.questions.size)
        val draft = pack.questions.single()
        assertEquals(pack.id, draft.packId)
        assertEquals(42, draft.userId)
        assertEquals(2, draft.sourcePageIndex)
        assertNull(draft.correctOption)
        assertFalse(draft.isApproved)
        assertFalse(draft.isAnswerVerified)
    }

    @Test fun rejectsInventedSourceBeforeWritingDraft() {
        val pack = pack()
        assertThrows(IllegalArgumentException::class.java) {
            ExamPackCloudCodec.attachDrafts(pack, response(source = "other.pdf"))
        }
        assertTrue(pack.questions.isEmpty())
    }

    @Test fun rejectsOutOfRangePageBeforeWritingDraft() {
        val pack = pack()
        assertThrows(IllegalArgumentException::class.java) {
            ExamPackCloudCodec.attachDrafts(pack, response(page = 3))
        }
        assertTrue(pack.questions.isEmpty())
    }
}
