package com.example.asm_final.learning

import org.junit.Assert.assertEquals
import org.junit.Test
import org.json.JSONObject

class ExamQuestionKindTest {
    @Test fun recognizesEverySupportedFormat() {
        assertEquals(ExamQuestionKind.SINGLE_CHOICE, ExamQuestionKind.fromWire("SINGLE_CHOICE"))
        assertEquals(ExamQuestionKind.TRUE_FALSE_4, ExamQuestionKind.fromWire("TRUE_FALSE_4"))
        assertEquals(ExamQuestionKind.SHORT_ANSWER, ExamQuestionKind.fromWire("SHORT_ANSWER"))
        assertEquals(ExamQuestionKind.ESSAY, ExamQuestionKind.fromWire("ESSAY"))
    }

    @Test fun neverTreatsUnknownFormatAsMultipleChoice() {
        assertEquals(ExamQuestionKind.UNKNOWN, ExamQuestionKind.fromWire("MULTI_SELECT"))
        assertEquals(ExamQuestionKind.UNKNOWN, ExamQuestionKind.fromWire(null))
    }

    @Test fun trueFalseNeedsFourDistinctCompleteStatements() {
        val valid = JSONObject("""{"subItems":[{"key":"a","statement":"A"},{"key":"b","statement":"B"},{"key":"c","statement":"C"},{"key":"d","statement":"D"}]}""")
        val question = ExamQuestionSnapshot("q", "", "P", "", "", "", "",
            questionType = "TRUE_FALSE_4", contentData = valid)
        assertEquals(true, question.hasValidTrueFalseItems)
        assertEquals(false, question.copy(contentData = JSONObject("""{"subItems":[{"key":"a","statement":"A"}]}""")).hasValidTrueFalseItems)
    }
}
