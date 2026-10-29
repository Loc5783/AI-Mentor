package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ExamSummary(
    val id: String,
    val title: String,
    val subjectId: String,
    val grade: Int,
    val examType: String, // MIDTERM, FINAL
    val semester: Int, // 1, 2
    val durationMinutes: Int,
    val questionCount: Int,
    val totalPoints: Int,
    val status: String, // DRAFT, PUBLISHED
    val version: Int
)

data class ExamQuestionSnapshot(
    val id: String,
    val code: String,
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val points: Int = 10,
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val correctOption: String? = null,
    val explanation: String? = null
)

data class ExamSession(
    val id: String,
    val examId: String,
    val examTitle: String,
    val subjectId: String,
    val grade: Int,
    val examType: String,
    val semester: Int,
    val durationMinutes: Int,
    val state: String, // IN_PROGRESS, SUBMITTED, TIMED_OUT
    val score: Double = 0.0,
    val score10: Double = 0.0,
    val rawScore: Int = 0,
    val totalPoints: Int = 0,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val serverNow: Long = 0L,
    val startedAt: Long = 0L,
    val deadlineAt: Long = 0L,
    val submittedAt: Long? = null,
    val revision: Int = 0,
    val questions: List<ExamQuestionSnapshot> = emptyList(),
    val answers: Map<String, String> = emptyMap(),
    val questionRevisions: Map<String, Int> = emptyMap()
)

interface ExamRepository {
    val endpoint: String
    val ownerId: Int
    suspend fun listExams(subjectId: String? = null, grade: Int? = null, examType: String? = null, semester: Int? = null): List<ExamSummary>
    suspend fun getExam(examId: String): ExamSummary
    suspend fun listUserSessions(subjectId: String? = null, grade: Int? = null): List<ExamSession>
    suspend fun startSession(examId: String): ExamSession
    suspend fun getSession(sessionId: String): ExamSession
    suspend fun saveAnswer(sessionId: String, questionId: String, selectedOption: String?, revision: Int): ExamSession
    suspend fun submitSession(sessionId: String): ExamSession
}

class BackendExamRepository(
    private val context: Context,
    private val owner: Int
) : ExamRepository {

    override val endpoint: String
        get() = BackendClient.endpoint(context)

    override val ownerId: Int
        get() = owner

    private fun client(): BackendClient = BackendClient(context, owner)

    override suspend fun listExams(
        subjectId: String?,
        grade: Int?,
        examType: String?,
        semester: Int?
    ): List<ExamSummary> = withContext(Dispatchers.IO) {
        val ep = BackendClient.endpoint(context)
        if (ep.isBlank()) {
            throw BackendClient.Failure("SERVER_NOT_CONFIGURED", 0)
        }
        val queryParts = mutableListOf<String>()
        if (!subjectId.isNullOrBlank()) queryParts.add("subject_id=" + java.net.URLEncoder.encode(subjectId, "UTF-8"))
        if (grade != null && grade > 0) queryParts.add("grade=$grade")
        if (!examType.isNullOrBlank()) queryParts.add("exam_type=" + java.net.URLEncoder.encode(examType, "UTF-8"))
        if (semester != null && semester > 0) queryParts.add("semester=$semester")

        val path = if (queryParts.isEmpty()) "/exams" else "/exams?" + queryParts.joinToString("&")
        val resp = client().request("GET", path, null, null, null)
        val items = resp.optJSONArray("items") ?: JSONArray()
        val list = mutableListOf<ExamSummary>()
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            list.add(parseExamSummary(obj))
        }
        list
    }

    override suspend fun getExam(examId: String): ExamSummary = withContext(Dispatchers.IO) {
        val resp = client().request("GET", "/exams/" + java.net.URLEncoder.encode(examId, "UTF-8"), null, null, null)
        parseExamSummary(resp)
    }

    override suspend fun listUserSessions(subjectId: String?, grade: Int?): List<ExamSession> = withContext(Dispatchers.IO) {
        val queryParts = mutableListOf<String>()
        if (!subjectId.isNullOrBlank()) queryParts.add("subject_id=" + java.net.URLEncoder.encode(subjectId, "UTF-8"))
        if (grade != null && grade > 0) queryParts.add("grade=$grade")
        val path = if (queryParts.isEmpty()) "/exams/sessions" else "/exams/sessions?" + queryParts.joinToString("&")
        val resp = client().request("GET", path, null, null, null)
        val items = resp.optJSONArray("items") ?: JSONArray()
        val list = mutableListOf<ExamSession>()
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            list.add(parseExamSession(obj))
        }
        list
    }

    override suspend fun startSession(examId: String): ExamSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("examId", examId)
        val resp = client().request("POST", "/exams/sessions", body, null, null)
        parseExamSession(resp)
    }

    override suspend fun getSession(sessionId: String): ExamSession = withContext(Dispatchers.IO) {
        val resp = client().request("GET", "/exams/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8"), null, null, null)
        parseExamSession(resp)
    }

    override suspend fun saveAnswer(
        sessionId: String,
        questionId: String,
        selectedOption: String?,
        revision: Int
    ): ExamSession = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("questionId", questionId)
            .put("selectedOption", selectedOption ?: JSONObject.NULL)
            .put("revision", revision)
        val resp = client().request(
            "POST",
            "/exams/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/answers",
            body,
            null,
            revision
        )
        parseExamSession(resp)
    }

    override suspend fun submitSession(sessionId: String): ExamSession = withContext(Dispatchers.IO) {
        val resp = client().request(
            "POST",
            "/exams/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/submit",
            JSONObject(),
            null,
            null
        )
        parseExamSession(resp)
    }

    private fun parseExamSummary(obj: JSONObject): ExamSummary {
        return ExamSummary(
            id = obj.getString("id"),
            title = obj.getString("title"),
            subjectId = obj.getString("subjectId"),
            grade = obj.getInt("grade"),
            examType = obj.optString("examType", "MIDTERM"),
            semester = obj.optInt("semester", 1),
            durationMinutes = obj.optInt("durationMinutes", 45),
            questionCount = obj.optInt("questionCount", 0),
            totalPoints = obj.optInt("totalPoints", 0),
            status = obj.optString("status", "PUBLISHED"),
            version = obj.optInt("version", 1)
        )
    }

    private fun parseExamSession(obj: JSONObject): ExamSession {
        val rawQuestions = obj.optJSONArray("questions") ?: JSONArray()
        val questions = mutableListOf<ExamQuestionSnapshot>()
        for (i in 0 until rawQuestions.length()) {
            val q = rawQuestions.getJSONObject(i)
            questions.add(
                ExamQuestionSnapshot(
                    id = q.getString("id"),
                    code = q.optString("code", ""),
                    prompt = q.getString("prompt"),
                    optionA = q.optString("optionA", ""),
                    optionB = q.optString("optionB", ""),
                    optionC = q.optString("optionC", ""),
                    optionD = q.optString("optionD", ""),
                    points = q.optInt("points", 10),
                    selectedOption = if (q.isNull("selectedOption")) null else q.optString("selectedOption"),
                    isCorrect = if (q.isNull("isCorrect")) null else q.optBoolean("isCorrect"),
                    correctOption = if (q.isNull("correctOption")) null else q.optString("correctOption"),
                    explanation = if (q.isNull("explanation")) null else q.optString("explanation")
                )
            )
        }

        val rawAnswers = obj.optJSONObject("answers") ?: JSONObject()
        val answersMap = mutableMapOf<String, String>()
        val keys = rawAnswers.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = rawAnswers.optString(k, "")
            if (v.isNotBlank()) {
                answersMap[k] = v
            }
        }

        val rawQRevs = obj.optJSONObject("questionRevisions") ?: JSONObject()
        val qRevsMap = mutableMapOf<String, Int>()
        val revKeys = rawQRevs.keys()
        while (revKeys.hasNext()) {
            val k = revKeys.next()
            qRevsMap[k] = rawQRevs.optInt(k, 0)
        }

        val serverNowVal = if (obj.has("serverNow")) obj.optLong("serverNow", 0L) else obj.optLong("startedAt", 0L)

        return ExamSession(
            id = obj.getString("id"),
            examId = obj.getString("examId"),
            examTitle = obj.optString("examTitle", ""),
            subjectId = obj.getString("subjectId"),
            grade = obj.getInt("grade"),
            examType = obj.optString("examType", "MIDTERM"),
            semester = obj.optInt("semester", 1),
            durationMinutes = obj.optInt("durationMinutes", 45),
            state = obj.optString("state", "IN_PROGRESS"),
            score = obj.optDouble("score", 0.0),
            score10 = obj.optDouble("score10", 0.0),
            rawScore = obj.optInt("rawScore", 0),
            totalPoints = obj.optInt("totalPoints", 0),
            correctCount = obj.optInt("correctCount", 0),
            totalCount = obj.optInt("totalCount", 0),
            serverNow = serverNowVal,
            startedAt = obj.optLong("startedAt", 0L),
            deadlineAt = obj.optLong("deadlineAt", 0L),
            submittedAt = if (obj.isNull("submittedAt")) null else obj.optLong("submittedAt"),
            revision = obj.optInt("revision", 0),
            questions = questions,
            answers = answersMap,
            questionRevisions = qRevsMap
        )
    }
}
