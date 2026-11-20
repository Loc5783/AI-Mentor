package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SupabaseAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Exam state and grading are authoritative in Supabase; this class only transports and maps data. */
class SupabaseExamRepository(
    context: Context,
    override val ownerId: Int
) : ExamRepository {
    private val app = context.applicationContext
    private val parser = BackendExamRepository(app, ownerId)
    override val endpoint: String get() = BuildConfig.SUPABASE_URL

    private fun call(method: String, path: String, body: JSONObject = JSONObject(), query: JSONObject = JSONObject()): JSONObject {
        require(path.startsWith("/") && !path.startsWith("//") && !path.contains(".."))
        val payload = JSONObject().put("method", method).put("path", path).put("body", body).put("query", query)
        return JSONObject(SupabaseAccounts(app).requestFor(
            ownerId, "POST", "/rest/v1/rpc/mentor_exam_api", payload
        ))
    }

    override suspend fun listExams(subjectId: String?, grade: Int?, examType: String?, semester: Int?): List<ExamSummary> =
        withContext(Dispatchers.IO) {
            val query = JSONObject()
            subjectId?.takeIf { it.isNotBlank() }?.let { query.put("subject_id", it) }
            grade?.takeIf { it in 10..12 }?.let { query.put("grade", it) }
            examType?.takeIf { it.isNotBlank() }?.let { query.put("exam_type", it.uppercase()) }
            semester?.takeIf { it in 1..2 }?.let { query.put("semester", it) }
            val rows = call("GET", "/exams", query = query).optJSONArray("items") ?: JSONArray()
            (0 until rows.length()).map { parser.parseExamSummary(rows.getJSONObject(it)) }
        }

    override suspend fun getExam(examId: String): ExamSummary = withContext(Dispatchers.IO) {
        call("GET", "/exams/${safeExamId(examId)}").let(parser::parseExamSummary)
    }

    override suspend fun listUserSessions(subjectId: String?, grade: Int?): List<ExamSession> = withContext(Dispatchers.IO) {
        val query = JSONObject()
        subjectId?.takeIf { it.isNotBlank() }?.let { query.put("subject_id", it) }
        grade?.takeIf { it in 10..12 }?.let { query.put("grade", it) }
        val rows = call("GET", "/exams/sessions", query = query).optJSONArray("items") ?: JSONArray()
        (0 until rows.length()).map { parser.parseExamSession(rows.getJSONObject(it)) }
    }

    override suspend fun startSession(examId: String): ExamSession = withContext(Dispatchers.IO) {
        call("POST", "/exams/sessions", JSONObject().put("examId", safeExamId(examId)))
            .let(parser::parseExamSession)
    }

    override suspend fun getSession(sessionId: String): ExamSession = withContext(Dispatchers.IO) {
        val id = safeSessionId(sessionId)
        gradeIfPending(call("GET", "/exams/sessions/$id").let(parser::parseExamSession))
    }

    override suspend fun saveAnswer(sessionId: String, questionId: String, selectedOption: String?, revision: Int): ExamSession =
        withContext(Dispatchers.IO) {
            require(questionId.isNotBlank() && questionId.length <= 200 && revision > 0)
            val body = JSONObject().put("questionId", questionId)
                .put("selectedOption", selectedOption ?: JSONObject.NULL).put("revision", revision)
            call("POST", "/exams/sessions/${safeSessionId(sessionId)}/answers", body)
                .let(parser::parseExamSession)
        }

    override suspend fun submitSession(sessionId: String): ExamSession = withContext(Dispatchers.IO) {
        val id = safeSessionId(sessionId)
        call("POST", "/exams/sessions/$id/submit").let(parser::parseExamSession)
    }

    override suspend fun explainReviewQuestion(session: ExamSession, question: ExamQuestionSnapshot): String =
        withContext(Dispatchers.IO) {
            require(session.state == "SUBMITTED" || session.state == "TIMED_OUT")
            require(session.questions.any { it.id == question.id })
            val prompt = buildString {
                append("Giải thích thêm câu hỏi trong bài thi lớp ${session.grade}, môn ${session.subjectId}. ")
                append("Câu hỏi: ${question.prompt}. ")
                append("Các lựa chọn: A. ${question.optionA}; B. ${question.optionB}; C. ${question.optionC}; D. ${question.optionD}. ")
                append("Học sinh trả lời: ${question.selectedOption ?: "chưa trả lời"}. ")
                append("Đáp án đúng: ${question.correctOption ?: "xem lời giải"}. ")
                append("Lời giải gốc: ${question.explanation.orEmpty()}. ")
                append("Hãy giải thích ngắn gọn từng bước và chỉ rõ vì sao đáp án của học sinh đúng hoặc sai. ")
                append("Nếu đáp án hoặc lời giải gốc có vấn đề, hãy nêu rõ.")
            }.take(7000)
            val turn = JSONObject().put("role", "user").put("text", prompt).put("files", JSONArray())
            val payload = JSONObject().put("mode", "general")
                .put("turns", JSONArray().put(turn))
            val answer = JSONObject(SupabaseAccounts(app).requestFor(
                ownerId, "POST", "/functions/v1/mentor-ai", payload
            )).optString("text", "")
            if (answer.isBlank()) throw BackendClient.Failure("EMPTY_RESPONSE", 502)
            answer
        }

    private fun gradeIfPending(session: ExamSession): ExamSession {
        if (session.gradingStatus != "PENDING") return session
        // Submission is already durable. A temporary AI failure leaves it pending for a later retry.
        return try {
            SupabaseAccounts(app).requestFor(ownerId, "POST", "/functions/v1/mentor-ai",
                JSONObject().put("mode", "exam_grade_essay").put("sessionId", session.id))
            call("GET", "/exams/sessions/${safeSessionId(session.id)}").let(parser::parseExamSession)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            session.copy(gradingError = if (e is BackendClient.Failure && e.message?.contains("RATE_LIMITED") == true)
                "Dịch vụ chấm đang bận. Bài làm đã lưu, hãy thử chấm lại sau."
            else "Chưa kết nối được dịch vụ chấm. Bài làm đã lưu, hãy thử lại khi có mạng.")
        }
    }

    private fun safeExamId(id: String): String {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,160}"))) throw BackendClient.Failure("INVALID_EXAM_ID", 400)
        return id
    }

    private fun safeSessionId(id: String): String {
        if (!runCatching { java.util.UUID.fromString(id) }.isSuccess) throw BackendClient.Failure("INVALID_SESSION_ID", 400)
        return id
    }
}
