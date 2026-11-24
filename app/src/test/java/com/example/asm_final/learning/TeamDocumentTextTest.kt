package com.example.asm_final.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class TeamDocumentTextTest {
    @Test fun readsVietnameseUtf8WithoutChangingEvidence() {
        val content = "  Yêu cầu: giải thích định luật Newton.  "
        assertEquals(content.trim(), TeamDocumentText.read(ByteArrayInputStream(content.toByteArray())))
    }

    @Test fun refusesBinaryAndOversizedFiles() {
        assertThrows(IllegalArgumentException::class.java) {
            TeamDocumentText.read(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TeamDocumentText.read(ByteArrayInputStream(ByteArray(12_001) { 'a'.code.toByte() }))
        }
    }
}
