package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SupabaseAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Cloud implementation of the existing practice-library contract. */
class SupabaseLibraryRepository(
    context: Context,
    override val ownerId: Int
) : LibraryRepository {
    private val appContext = context.applicationContext
    override val endpoint: String get() = BuildConfig.SUPABASE_URL

    private fun request(method: String, path: String, body: JSONObject = JSONObject(), query: JSONObject = JSONObject()): JSONObject {
        val payload = JSONObject()
            .put("method", method)
            .put("path", path)
            .put("body", body)
            .put("query", query)
        return JSONObject(SupabaseAccounts(appContext).requestFor(
            ownerId, "POST", "/rest/v1/rpc/mentor_library_api", payload
        ))
    }

    override suspend fun loadQuizzes(subjectWireId: String, grade: Int): List<QuizSummary> = withContext(Dispatchers.IO) {
        if (grade !in 10..12) throw BackendClient.Failure("INVALID_GRADE", 400)
        val rows = request("GET", "/library/quizzes", query = JSONObject()
            .put("subject_id", subjectWireId).put("grade", grade)).optJSONArray("items") ?: JSONArray()
        (0 until rows.length()).map { i ->
            val row = rows.getJSONObject(i)
            QuizSummary(row.getString("id"), row.getString("title"), row.getString("subjectId"),
                row.getInt("grade"), row.optString("chapter", ""), row.optString("topic", ""),
                row.optString("difficulty", "MEDIUM"), row.optInt("questionCount", 0))
        }
    }

    override suspend fun listActiveSessions(subjectWireId: String, grade: Int): List<SessionSummary> = withContext(Dispatchers.IO) {
        val rows = request("GET", "/library/sessions", query = JSONObject()
            .put("subject_id", subjectWireId).put("grade", grade).put("state", "IN_PROGRESS"))
            .optJSONArray("items") ?: JSONArray()
        (0 until rows.length()).map { i ->
            val row = rows.getJSONObject(i)
            SessionSummary(row.getString("id"), row.getString("quizId"), row.getString("subjectId"),
                row.getInt("grade"), row.getString("state"), row.optInt("score", 0),
                row.optInt("correctCount", 0), row.optInt("totalCount", 0),
                row.optInt("answeredCount", 0), row.optInt("firstUnansweredIndex", 0),
                row.optString("updatedAt", ""))
        }
    }

    override suspend fun getSession(sessionId: String): PracticeSession = withContext(Dispatchers.IO) {
        BackendLibraryRepository.parseSession(request("GET", "/library/sessions/${safeId(sessionId)}"))
    }

    override suspend fun startSession(quizId: String): PracticeSession = withContext(Dispatchers.IO) {
        BackendLibraryRepository.parseSession(request("POST", "/library/sessions",
            JSONObject().put("quizId", safeText(quizId))))
    }

    override suspend fun submitAnswer(sessionId: String, questionId: String, selectedOption: String): PracticeSession = withContext(Dispatchers.IO) {
        BackendLibraryRepository.parseSession(request("POST", "/library/sessions/${safeId(sessionId)}/answers",
            JSONObject().put("questionId", safeText(questionId)).put("selectedOption", selectedOption)))
    }

    override suspend fun completeSession(sessionId: String): PracticeSession = withContext(Dispatchers.IO) {
        BackendLibraryRepository.parseSession(request("POST", "/library/sessions/${safeId(sessionId)}/complete"))
    }

    override suspend fun loadWrongQuestions(subjectWireId: String?, grade: Int?, state: String?): List<WrongQuestionEntry> = withContext(Dispatchers.IO) {
        val query = JSONObject()
        if (!subjectWireId.isNullOrBlank()) query.put("subject_id", subjectWireId)
        if (grade != null) query.put("grade", grade)
        if (!state.isNullOrBlank() && state != "ALL") query.put("state", state)
        val rows = request("GET", "/me/wrong-questions", query = query).optJSONArray("items") ?: JSONArray()
        (0 until rows.length()).map { i -> BackendLibraryRepository.parseWrongQuestion(rows.getJSONObject(i)) }
    }

    override suspend fun retryWrongQuestion(entryId: String, selectedOption: String): WrongQuestionRetryResult = withContext(Dispatchers.IO) {
        val row = request("POST", "/me/wrong-questions/${safeId(entryId)}/retry",
            JSONObject().put("selectedOption", selectedOption))
        WrongQuestionRetryResult(row.getString("entryId"), row.getString("questionId"),
            row.getString("selectedOption"), row.getString("correctOption"), row.getBoolean("isCorrect"),
            row.getString("state"), row.optString("explanation", ""), row.optString("updatedAt", ""))
    }

    private fun safeId(value: String): String {
        if (!value.matches(Regex("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")))
            throw IllegalArgumentException("Invalid session ID")
        return value
    }

    private fun safeText(value: String): String {
        if (value.isBlank() || value.length > 200 || value.any { it == '/' || it == '?' || it == '#' })
            throw IllegalArgumentException("Invalid library identifier")
        return value
    }
}
