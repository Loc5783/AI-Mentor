package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SupabaseAccounts
import kotlinx.coroutines.Dispatchers
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
        call("GET", "/exams/sessions/${safeSessionId(sessionId)}").let(parser::parseExamSession)
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
        call("POST", "/exams/sessions/${safeSessionId(sessionId)}/submit").let(parser::parseExamSession)
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
