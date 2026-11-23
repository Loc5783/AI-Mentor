package com.example.asm_final.learning

import org.json.JSONObject

/** Treat AI extraction as untrusted draft data; identity and approval belong to the app. */
internal object ExamPackCloudCodec {
    fun attachDrafts(pack: ExamPackFoundry.ExamPack, response: JSONObject) {
        val rows = response.getJSONArray("questions")
        require(rows.length() in 1..40) { "AI chưa đọc được câu hỏi trong tệp" }
        val drafts = (0 until rows.length()).map { i ->
            val row = rows.getJSONObject(i)
            val source = pack.sourceFiles.singleOrNull { it.fileName == row.getString("sourceFileName") }
                ?: throw IllegalArgumentException("Nguồn câu hỏi không hợp lệ")
            val page = row.getInt("sourcePageIndex")
            require(page in 1..source.pageCount) { "Trang nguồn không hợp lệ" }
            val options = row.getJSONObject("options")
            ExamPackFoundry.ExtractedQuestion(packId = pack.id, userId = pack.userId,
                prompt = row.getString("prompt"),
                options = listOf("A", "B", "C", "D").associateWith { options.getString(it) },
                correctOption = row.optNullable("correctOption"),
                explanation = row.optNullable("explanation"), topic = row.optNullable("topic"),
                sourceFileName = source.fileName, sourcePageIndex = page)
        }
        ExamPackFoundry.processExtraction(pack, drafts)
        require(pack.questions.isNotEmpty()) { "Không tìm thấy câu hỏi hợp lệ" }
    }

    private fun JSONObject.optNullable(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
