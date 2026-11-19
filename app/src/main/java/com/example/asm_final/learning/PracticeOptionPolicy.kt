package com.example.asm_final.learning

/** Preserve the question's real choices; older lesson quizzes may have only three. */
internal fun visiblePracticeOptions(question: PracticeQuestion): List<Pair<String, String>> =
    listOf("A" to question.optionA, "B" to question.optionB,
        "C" to question.optionC, "D" to question.optionD)
        .filter { (_, text) -> text.isNotBlank() }
