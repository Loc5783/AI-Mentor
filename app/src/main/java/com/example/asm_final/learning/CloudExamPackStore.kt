package com.example.asm_final.learning

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.supabase.SupabaseAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Account-scoped cloud storage. Imported questions remain private drafts until reviewed. */
class CloudExamPackStore(context: Context, private val owner: Int) : ExamPackStore {
    private val app = context.applicationContext
    private val accounts = SupabaseAccounts(app)

    private fun remoteUserId(): String = RemoteAccountLinks(app).use { links ->
        links.remoteId(owner, BuildConfig.SUPABASE_URL).also { require(it.isNotBlank()) }
    }

    private fun call(method: String, path: String, body: JSONObject? = null): String =
        accounts.requestFor(owner, method, path, body)

    override suspend fun list(): List<ExamPackFoundry.ExamPack> = withContext(Dispatchers.IO) {
        val rows = JSONArray(call("GET", "/rest/v1/mentor_exam_packs?select=*&order=created_at.desc&limit=50"))
        (0 until rows.length()).map { parsePack(rows.getJSONObject(it)) }
    }

    override suspend fun load(id: String): ExamPackFoundry.ExamPack = withContext(Dispatchers.IO) {
        require(UUID.fromString(id).toString() == id)
        val packs = JSONArray(call("GET", "/rest/v1/mentor_exam_packs?select=*&id=eq.$id&limit=1"))
        require(packs.length() == 1) { "Exam pack unavailable" }
        val pack = parsePack(packs.getJSONObject(0))
        val rows = JSONArray(call("GET", "/rest/v1/mentor_exam_pack_questions?select=*&pack_id=eq.$id&order=created_at.asc&limit=100"))
        repeat(rows.length()) { pack.questions.add(parseQuestion(rows.getJSONObject(it), pack)) }
        ExamPackFoundry.finalizePack(pack)
    }

    override suspend fun importFile(title: String, subjectId: String, grade: Int, uri: Uri): ExamPackFoundry.ExamPack =
        withContext(Dispatchers.IO) {
            require(title.isNotBlank() && title.length <= 120 && subjectId.isNotBlank() && grade in 10..12)
            val resolver = app.contentResolver
            val mime = resolver.getType(uri).orEmpty()
            require(mime in listOf("image/jpeg", "image/png", "image/webp", "application/pdf")) {
                "Chỉ hỗ trợ PDF hoặc ảnh JPG, PNG, WebP"
            }
            val pages = if (mime == "application/pdf") {
                resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    android.graphics.pdf.PdfRenderer(descriptor).use { it.pageCount }
                } ?: error("Không đọc được PDF")
            } else 1
            require(pages in 1..50) { "Tệp phải có từ 1 đến 50 trang" }
            var name = "de-hoc-tap"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(0)?.take(180).orEmpty().ifBlank { name }
            }
            val bytes = resolver.openInputStream(uri)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val count = stream.read(chunk)
                    if (count < 0) break
                    require(output.size() + count <= 4 * 1024 * 1024) { "Tệp phải nhỏ hơn 4 MB" }
                    output.write(chunk, 0, count)
                }
                output.toByteArray().also { require(it.isNotEmpty()) }
            } ?: error("Không đọc được ảnh")
            val files = JSONArray().put(JSONObject().put("name", name).put("mime", mime)
                .put("pageCount", pages).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
            val turns = JSONArray().put(JSONObject().put("role", "user").put("text", "Trích câu hỏi trắc nghiệm từ tệp")
                .put("files", files))
            val response = JSONObject(call("POST", "/functions/v1/mentor-ai",
                JSONObject().put("mode", "exampack_extract").put("turns", turns)))
            val pack = ExamPackFoundry.ExamPack(userId = owner, title = title.trim(),
                subjectId = subjectId, grade = grade,
                sourceFiles = listOf(ExamPackFoundry.ExamPackFile(name, pages, mime)))
            ExamPackCloudCodec.attachDrafts(pack, response)
            val uid = remoteUserId()
            call("POST", "/rest/v1/mentor_exam_packs", JSONObject().put("id", pack.id)
                .put("user_id", uid).put("title", pack.title).put("subject_id", pack.subjectId)
                .put("grade", pack.grade).put("source_files", JSONArray().put(JSONObject()
                    .put("fileName", name).put("pageCount", pages).put("mimeType", mime)))
                .put("status", "DRAFT").put("question_count", 0))
            // A network failure leaves the parent draft visible for a later retry; never claim approval.
            pack.questions.forEach { q ->
                call("POST", "/rest/v1/mentor_exam_pack_questions", questionBody(q, uid))
            }
            call("PATCH", "/rest/v1/mentor_exam_packs?id=eq.${pack.id}", JSONObject()
                .put("question_count", pack.questions.size))
            pack
        }

    override suspend fun review(pack: ExamPackFoundry.ExamPack, questionId: String, key: String,
                       prompt: String, options: Map<String, String>, explanation: String,
                       topic: String): ExamPackFoundry.ExamPack = withContext(Dispatchers.IO) {
        val copy = load(pack.id)
        val reviewed = ExamPackFoundry.reviewQuestion(copy, questionId, key, prompt, options, explanation, topic)
        require(reviewed.isApproved) { "Điền đủ bốn đáp án khác nhau, lời giải, chủ đề và đáp án đúng" }
        call("PATCH", "/rest/v1/mentor_exam_pack_questions?id=eq.${reviewed.id}&pack_id=eq.${copy.id}",
            questionBody(reviewed, remoteUserId()))
        val approved = ExamPackFoundry.finalizePack(copy).isApproved
        call("PATCH", "/rest/v1/mentor_exam_packs?id=eq.${copy.id}", JSONObject()
            .put("status", if (approved) "APPROVED" else "DRAFT"))
        copy
    }

    private fun questionBody(q: ExamPackFoundry.ExtractedQuestion, uid: String) = JSONObject()
        .put("id", q.id).put("pack_id", q.packId).put("user_id", uid).put("prompt", q.prompt)
        .put("options", JSONObject(q.options)).put("correct_option", q.correctOption)
        .put("explanation", q.explanation).put("topic", q.topic).put("difficulty", q.difficulty)
        .put("source_file_name", q.sourceFileName).put("source_page_index", q.sourcePageIndex)
        .put("is_approved", q.isApproved).put("is_answer_verified", q.isAnswerVerified)

    private fun parsePack(row: JSONObject): ExamPackFoundry.ExamPack {
        val files = row.getJSONArray("source_files")
        return ExamPackFoundry.ExamPack(id = row.getString("id"), userId = owner,
            title = row.getString("title"), subjectId = row.getString("subject_id"), grade = row.getInt("grade"),
            sourceFiles = (0 until files.length()).map { i ->
                val f = files.getJSONObject(i)
                ExamPackFoundry.ExamPackFile(f.getString("fileName"), f.getInt("pageCount"), f.getString("mimeType"))
            })
    }

    private fun parseQuestion(row: JSONObject, pack: ExamPackFoundry.ExamPack): ExamPackFoundry.ExtractedQuestion {
        val options = row.getJSONObject("options")
        return ExamPackFoundry.ExtractedQuestion(id = row.getString("id"), packId = pack.id, userId = owner,
            prompt = row.getString("prompt"), options = (listOf("A", "B", "C", "D")).associateWith { options.optString(it) },
            correctOption = row.optNullable("correct_option"),
            explanation = row.optNullable("explanation"), topic = row.optNullable("topic"),
            difficulty = row.optString("difficulty", "MEDIUM"), sourceFileName = row.getString("source_file_name"),
            sourcePageIndex = row.getInt("source_page_index"), isApproved = row.optBoolean("is_approved"),
            isAnswerVerified = row.optBoolean("is_answer_verified"))
    }

    private fun JSONObject.optNullable(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
