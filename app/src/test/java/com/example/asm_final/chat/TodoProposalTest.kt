package com.example.asm_final.chat

import org.junit.Assert.*
import org.junit.Test

class TodoProposalTest {
    private fun block(rows: String) = "Draft\n```ai_todo\n{\"tasks\":[$rows]}\n```"

    @Test fun parsesPlanAndHidesMachineBlock() {
        val text = block("""{"day":2,"title":"Ôn định luật Newton","subject":"Vật lý","priority":2}""")
        assertEquals(TodoProposal.Task(2, "Ôn định luật Newton", "Vật lý", 2), TodoProposal.parse(text)!!.single())
        assertEquals("Draft", TodoProposal.displayText(text))
        val withoutSubject = block("""{"day":1,"title":"Đọc lại bài","priority":1}""")
        assertNull(TodoProposal.parse(withoutSubject)!!.single().subject)
    }

    @Test fun rejectsUnsafeOrInvalidTasks() {
        for (row in listOf(
            """{"day":0,"title":"A","priority":1}""",
            """{"day":"2","title":"A","priority":1}""",
            """{"day":2,"title":" ","priority":1}""",
            """{"day":2,"title":"A","priority":3}""",
            """{"day":2,"title":"A","priority":1.5}""",
            """{"day":2,"title":"A","subject":4,"priority":1}""",
        )) assertNull(TodoProposal.parse(block(row)))
    }

    @Test fun rejectsDuplicateAndMultipleBlocks() {
        val row = """{"day":3,"title":"Read","subject":"English","priority":1}"""
        assertNull(TodoProposal.parse(block("$row,$row")))
        assertNull(TodoProposal.parse(block(row) + block(row)))
    }

    @Test fun ignoresOrdinaryTextAndMalformedJson() {
        assertNull(TodoProposal.parse("Hãy ôn tập đều mỗi ngày"))
        assertNull(TodoProposal.parse("```ai_todo\ninvalid```"))
        assertNull(TodoProposal.parse(block("")))
    }
}
