package com.example.asm_final.learning

data class AiPracticeConfig(
    val configured: Boolean,
    val subjects: List<String>,
    val difficulties: List<String>,
    val minQuestions: Int,
    val maxQuestions: Int,
    val defaultQuestions: Int,
    val provider: String = ""
)

data class AiQuestion(
    val id: String,
    val prompt: String,
    val options: Map<String, String>,
    val hint: String = "",
    val order: Int = 1,
    val difficulty: String = "MEDIUM",
    val correctOption: String? = null,
    val explanation: String? = null
)

data class AiAnswerRecord(
    val selectedOption: String?,
    val isCorrect: Boolean,
    val firstTryCorrect: Boolean,
    val attemptsCount: Int,
    val revealed: Boolean,
    val skipped: Boolean = false
) {
    val isSkipped: Boolean get() = skipped
}

data class AiPracticeSummary(
    val totalQuestions: Int,
    val firstTryCorrectCount: Int,
    val assistedCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val accuracyFirstTry: Double
) {
    val accuracyPercent: Int
        get() = Math.round(accuracyFirstTry.coerceIn(0.0, 100.0)).toInt()
}

data class AiPracticeSession(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val grade: Int,
    val topic: String,
    val difficulty: String,
    val questionCount: Int,
    val currentIndex: Int,
    val state: String,
    val questions: List<AiQuestion>,
    val answers: Map<String, AiAnswerRecord>,
    val summary: AiPracticeSummary? = null
)

data class AiAnswerResult(
    val questionId: String,
    val isCorrect: Boolean,
    val selectedOption: String?,
    val correctOption: String,
    val explanation: String,
    val hint: String = "",
    val firstTryCorrect: Boolean,
    val attemptsCount: Int = 1,
    val isSkipped: Boolean = false
)
