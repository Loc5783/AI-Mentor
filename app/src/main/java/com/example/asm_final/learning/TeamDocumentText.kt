package com.example.asm_final.learning

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Bounded UTF-8 import for TeamOS text documents; does not silently truncate evidence. */
object TeamDocumentText {
    private const val MAX_BYTES = 12_000

    fun read(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            require(output.size() + read <= MAX_BYTES) { "Tệp quá dài để đối chiếu trong một lần." }
            output.write(buffer, 0, read)
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(output.toByteArray())).toString().trim()
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw IllegalArgumentException("Chỉ hỗ trợ tệp văn bản UTF-8.")
        }
        require(text.isNotBlank()) { "Tệp chưa có nội dung văn bản." }
        return text
    }
}
