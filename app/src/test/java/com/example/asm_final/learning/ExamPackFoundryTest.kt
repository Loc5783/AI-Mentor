package com.example.asm_final.learning

import org.junit.Assert.*
import org.junit.Test

class ExamPackFoundryTest {

    @Test
    fun reImportingSameFileDeduplicatesExactQuestions() {
        val pack = ExamPackFoundry.ExamPack(
            userId = 101,
            title = "Bộ đề ôn tập Vật lý 11",
            subjectId = "physics",
            grade = 11,
            sourceFiles = listOf(ExamPackFoundry.ExamPackFile("De_Thi_Lý_11.pdf", pageCount = 5))
        )

        val q1 = ExamPackFoundry.ExtractedQuestion(
            packId = pack.id,
            userId = 101,
            prompt = "Phương trình dao động điều hòa x = A cos(ωt + φ). Biên độ là gì?",
            options = mapOf("A" to "A", "B" to "ω", "C" to "φ", "D" to "x"),
            correctOption = "A",
            explanation = "A là biên độ dao động.",
            topic = "Dao động",
            sourceFileName = "De_Thi_Lý_11.pdf",
            sourcePageIndex = 1
        )

        // First import -> added
        val added1 = ExamPackFoundry.processExtraction(pack, listOf(q1))
        assertEquals(1, added1.size)
        assertEquals(1, pack.questions.size)

        // Second import of same question -> deduplicated (0 added)
        val added2 = ExamPackFoundry.processExtraction(pack, listOf(q1))
        assertEquals(0, added2.size)
        assertEquals(1, pack.questions.size)
    }

    @Test
    fun numericalVariationsWithDifferentAnswersAreKeptSeparate() {
        val pack = ExamPackFoundry.ExamPack(
            userId = 101,
            title = "Bộ đề Toán 11",
            subjectId = "math",
            grade = 11,
            sourceFiles = listOf(ExamPackFoundry.ExamPackFile("De_Toan_11.pdf"))
        )

        // Q1 with number 5 -> answer B (10)
        val q1 = ExamPackFoundry.ExtractedQuestion(
            packId = pack.id,
            userId = 101,
            prompt = "Tính giá trị 2 * 5",
            options = mapOf("A" to "8", "B" to "10", "C" to "12", "D" to "14"),
            correctOption = "B",
            explanation = "2 * 5 = 10",
            topic = "Phép tính",
            sourceFileName = "De_Toan_11.pdf",
            sourcePageIndex = 1
        )

        // Q2 with number 6 -> answer D (12)
        val q2 = ExamPackFoundry.ExtractedQuestion(
            packId = pack.id,
            userId = 101,
            prompt = "Tính giá trị 2 * 6",
            options = mapOf("A" to "8", "B" to "10", "C" to "14", "D" to "12"),
            correctOption = "D",
            explanation = "2 * 6 = 12",
            topic = "Phép tính",
            sourceFileName = "De_Toan_11.pdf",
            sourcePageIndex = 2
        )

        val added = ExamPackFoundry.processExtraction(pack, listOf(q1, q2))
        assertEquals(2, added.size)
        assertEquals(2, pack.questions.size)
    }

    @Test
    fun aiOutputCannotSelfGrantAnswerVerifiedStatus() {
        val pack = ExamPackFoundry.ExamPack(
            userId = 101,
            title = "Bộ đề Hóa 11",
            subjectId = "chemistry",
            grade = 11,
            sourceFiles = listOf(ExamPackFoundry.ExamPackFile("De_Hoa_11.pdf"))
        )

        val aiQuestion = ExamPackFoundry.ExtractedQuestion(
            packId = pack.id,
            userId = 101,
            prompt = "pH của dung dịch HCl 0.001M là?",
            options = mapOf("A" to "1", "B" to "2", "C" to "3", "D" to "4"),
            correctOption = "C",
            explanation = "pH = -log(10^-3) = 3",
            topic = "pH",
            sourceFileName = "De_Hoa_11.pdf",
            sourcePageIndex = 3,
            isApproved = true, // AI attempted to self-approve
            isAnswerVerified = true // AI attempted to self-verify answer
        )

        val added = ExamPackFoundry.processExtraction(pack, listOf(aiQuestion))
        assertEquals(1, added.size)

        // Verification check: AI extraction output is forced to false until human review
        assertFalse(added[0].isApproved)
        assertFalse(added[0].isAnswerVerified)
    }

    @Test
    fun studentManualApprovalPreservesProvenanceAndSetsVerifiedStatus() {
        val pack = ExamPackFoundry.ExamPack(
            userId = 101,
            title = "Bộ đề Sinh 11",
            subjectId = "biology",
            grade = 11,
            sourceFiles = listOf(ExamPackFoundry.ExamPackFile("De_Sinh_11.pdf", pageCount = 4))
        )

        val raw = ExamPackFoundry.ExtractedQuestion(
            packId = pack.id,
            userId = 101,
            prompt = "Pha sáng diễn ra ở đâu?",
            options = mapOf("A" to "Stroma", "B" to "Thylakoid", "C" to "Màng ngoài", "D" to "Nhân"),
            correctOption = null, // Missing answer
            explanation = "Màng thylakoid",
            topic = "Quang hợp",
            sourceFileName = "De_Sinh_11.pdf",
            sourcePageIndex = 2
        )

        ExamPackFoundry.processExtraction(pack, listOf(raw))

        // Human student approves and verifies answer key = "B"
        val approved = ExamPackFoundry.approveQuestion(
            question = pack.questions[0],
            verifiedAnswerKey = "B"
        )

        assertTrue(approved.isApproved)
        assertTrue(approved.isAnswerVerified)
        assertEquals("B", approved.correctOption)
        assertEquals("De_Sinh_11.pdf", approved.sourceFileName)
        assertEquals(2, approved.sourcePageIndex)

        ExamPackFoundry.finalizePack(pack)
        assertTrue(pack.isApproved)
    }

    @Test
    fun questionWithConflictingOrMissingAnswerCannotBeVerified() {
        val pack = ExamPackFoundry.ExamPack(
            userId = 101,
            title = "Bộ đề Anh 11",
            subjectId = "english",
            grade = 11,
            sourceFiles = listOf(ExamPackFoundry.ExamPackFile("De_Anh_11.pdf"))
        )

        val raw = ExamPackFoundry.ExtractedQuestion(
            packId = pack.id,
            userId = 101,
            prompt = "Choose the best answer",
            options = mapOf("A" to "Go", "B" to "Went", "C" to "Gone", "D" to "Going"),
            correctOption = null,
            explanation = "Invalid key",
            topic = "Grammar",
            sourceFileName = "De_Anh_11.pdf"
        )

        ExamPackFoundry.processExtraction(pack, listOf(raw))

        // Student approves with invalid answer option "Z"
        val approved = ExamPackFoundry.approveQuestion(
            question = pack.questions[0],
            verifiedAnswerKey = "Z" // Invalid key not in A, B, C, D
        )

        assertTrue(approved.isApproved)
        assertFalse(approved.isAnswerVerified) // Cannot be verified with invalid key
    }
}
