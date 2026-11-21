package com.example.asm_final.learning

/** Supported exam question formats. Unknown server values stay unsupported. */
enum class ExamQuestionKind(val wireValue: String) {
    SINGLE_CHOICE("SINGLE_CHOICE"),
    TRUE_FALSE_4("TRUE_FALSE_4"),
    SHORT_ANSWER("SHORT_ANSWER"),
    ESSAY("ESSAY"),
    UNKNOWN("");

    companion object {
        fun fromWire(value: String?): ExamQuestionKind =
            entries.firstOrNull { it != UNKNOWN && it.wireValue == value } ?: UNKNOWN
    }
}
