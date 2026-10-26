package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class QuizSummary(
    val id: String,
    val title: String,
    val subjectId: String,
    val grade: Int,
    val chapter: String,
    val topic: String,
    val difficulty: String,
    val questionCount: Int
)

data class SessionSummary(
    val id: String,
    val quizId: String,
    val subjectId: String,
    val grade: Int,
    val state: String,
    val score: Int,
    val correctCount: Int,
    val totalCount: Int,
    val answeredCount: Int,
    val firstUnansweredIndex: Int,
    val updatedAt: String
)

data class PracticeQuestion(
    val id: String,
    val code: String,
    val chapter: String,
    val topic: String,
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val points: Int = 10,
    val userAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val correctOption: String? = null,
    val explanation: String? = null
)

data class PracticeSession(
    val id: String,
    val quizId: String,
    val subjectId: String,
    val grade: Int,
    val state: String,
    val score: Int,
    val maxScore: Int,
    val correctCount: Int,
    val totalCount: Int,
    val questions: List<PracticeQuestion>,
    val answers: Map<String, Any> = emptyMap(),
    val answeredCount: Int = 0,
    val firstUnansweredIndex: Int = 0
)

data class WrongQuestionOption(
    val id: String,
    val text: String
)

data class WrongQuestionEntry(
    val id: String,
    val questionId: String,
    val subjectId: String,
    val grade: Int,
    val chapter: String,
    val topic: String,
    val prompt: String,
    val options: List<WrongQuestionOption>,
    val selectedOption: String,
    val correctOption: String,
    val explanation: String,
    val difficulty: String,
    val state: String, // NEEDS_REVIEW, RESOLVED
    val hasSnapshot: Boolean,
    val canRetry: Boolean,
    val createdAt: String,
    val updatedAt: String?
)

data class WrongQuestionRetryResult(
    val entryId: String,
    val questionId: String,
    val selectedOption: String,
    val correctOption: String,
    val isCorrect: Boolean,
    val state: String,
    val explanation: String,
    val updatedAt: String
)

interface LibraryRepository {
    val endpoint: String
    val ownerId: Int
    suspend fun loadQuizzes(subjectWireId: String, grade: Int): List<QuizSummary>
    suspend fun listActiveSessions(subjectWireId: String, grade: Int): List<SessionSummary>
    suspend fun getSession(sessionId: String): PracticeSession
    suspend fun startSession(quizId: String): PracticeSession
    suspend fun submitAnswer(sessionId: String, questionId: String, selectedOption: String): PracticeSession
    suspend fun completeSession(sessionId: String): PracticeSession
    suspend fun loadWrongQuestions(subjectWireId: String?, grade: Int?, state: String? = null): List<WrongQuestionEntry>
    suspend fun retryWrongQuestion(entryId: String, selectedOption: String): WrongQuestionRetryResult
}

class BackendLibraryRepository(
    private val context: Context,
    private val owner: Int
) : LibraryRepository {

    override val endpoint: String
        get() = BackendClient.endpoint(context)

    override val ownerId: Int
        get() = owner

    private fun client(): BackendClient = BackendClient(context, owner)

    override suspend fun loadQuizzes(subjectWireId: String, grade: Int): List<QuizSummary> = withContext(Dispatchers.IO) {
        val ep = BackendClient.endpoint(context)
        if (ep.isBlank()) {
            throw BackendClient.Failure("SERVER_NOT_CONFIGURED", 0)
        }
        val resp = client().request("GET", "/library/quizzes?subject_id=$subjectWireId&grade=$grade", null, null, null)
        val items = resp.optJSONArray("items") ?: JSONArray()
        val list = mutableListOf<QuizSummary>()
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            list.add(
                QuizSummary(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    subjectId = obj.getString("subjectId"),
                    grade = obj.getInt("grade"),
                    chapter = obj.optString("chapter", ""),
                    topic = obj.optString("topic", ""),
                    difficulty = obj.optString("difficulty", ""),
                    questionCount = obj.optInt("questionCount", 0)
                )
            )
        }
        list
    }

    override suspend fun listActiveSessions(subjectWireId: String, grade: Int): List<SessionSummary> = withContext(Dispatchers.IO) {
        val resp = client().request("GET", "/library/sessions?subject_id=$subjectWireId&grade=$grade&state=IN_PROGRESS", null, null, null)
        val items = resp.optJSONArray("items") ?: JSONArray()
        val list = mutableListOf<SessionSummary>()
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            list.add(
                SessionSummary(
                    id = obj.getString("id"),
                    quizId = obj.getString("quizId"),
                    subjectId = obj.getString("subjectId"),
                    grade = obj.getInt("grade"),
                    state = obj.getString("state"),
                    score = obj.optInt("score", 0),
                    correctCount = obj.optInt("correctCount", 0),
                    totalCount = obj.optInt("totalCount", 0),
                    answeredCount = obj.optInt("answeredCount", 0),
                    firstUnansweredIndex = obj.optInt("firstUnansweredIndex", 0),
                    updatedAt = obj.optString("updatedAt", "")
                )
            )
        }
        list
    }

    override suspend fun getSession(sessionId: String): PracticeSession = withContext(Dispatchers.IO) {
        val resp = client().request("GET", "/library/sessions/$sessionId", null, null, null)
        parseSession(resp)
    }

    override suspend fun startSession(quizId: String): PracticeSession = withContext(Dispatchers.IO) {
        val resp = client().request("POST", "/library/sessions", JSONObject().put("quizId", quizId), null, null)
        parseSession(resp)
    }

    override suspend fun submitAnswer(sessionId: String, questionId: String, selectedOption: String): PracticeSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("questionId", questionId).put("selectedOption", selectedOption)
        val resp = client().request("POST", "/library/sessions/$sessionId/answers", body, null, null)
        parseSession(resp)
    }

    override suspend fun completeSession(sessionId: String): PracticeSession = withContext(Dispatchers.IO) {
        val resp = client().request("POST", "/library/sessions/$sessionId/complete", JSONObject(), null, null)
        parseSession(resp)
    }

    override suspend fun loadWrongQuestions(
        subjectWireId: String?,
        grade: Int?,
        state: String?
    ): List<WrongQuestionEntry> = withContext(Dispatchers.IO) {
        val params = mutableListOf<String>()
        if (!subjectWireId.isNullOrBlank()) params.add("subject_id=$subjectWireId")
        if (grade != null) params.add("grade=$grade")
        if (!state.isNullOrBlank() && state != "ALL") params.add("state=$state")
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        val resp = client().request("GET", "/me/wrong-questions$qs", null, null, null)
        val items = resp.optJSONArray("items") ?: JSONArray()
        val list = mutableListOf<WrongQuestionEntry>()
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            list.add(parseWrongQuestion(obj))
        }
        list
    }

    override suspend fun retryWrongQuestion(
        entryId: String,
        selectedOption: String
    ): WrongQuestionRetryResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("selectedOption", selectedOption)
        val resp = client().request("POST", "/me/wrong-questions/$entryId/retry", body, null, null)
        WrongQuestionRetryResult(
            entryId = resp.getString("entryId"),
            questionId = resp.getString("questionId"),
            selectedOption = resp.getString("selectedOption"),
            correctOption = resp.getString("correctOption"),
            isCorrect = resp.getBoolean("isCorrect"),
            state = resp.getString("state"),
            explanation = resp.optString("explanation", ""),
            updatedAt = resp.optString("updatedAt", "")
        )
    }

    companion object {
        fun parseSession(obj: JSONObject): PracticeSession {
            val qArray = obj.optJSONArray("questions") ?: JSONArray()
            val questions = mutableListOf<PracticeQuestion>()
            for (i in 0 until qArray.length()) {
                val qObj = qArray.getJSONObject(i)
                val opts = qObj.optJSONArray("options")
                var optA = ""
                var optB = ""
                var optC = ""
                var optD = ""
                if (opts != null) {
                    for (j in 0 until opts.length()) {
                        val o = opts.getJSONObject(j)
                        when (o.optString("id")) {
                            "A" -> optA = o.optString("text", "")
                            "B" -> optB = o.optString("text", "")
                            "C" -> optC = o.optString("text", "")
                            "D" -> optD = o.optString("text", "")
                        }
                    }
                }
                questions.add(
                    PracticeQuestion(
                        id = qObj.getString("id"),
                        code = qObj.optString("code", ""),
                        chapter = qObj.optString("chapter", ""),
                        topic = qObj.optString("topic", ""),
                        prompt = qObj.getString("prompt"),
                        optionA = optA,
                        optionB = optB,
                        optionC = optC,
                        optionD = optD,
                        points = qObj.optInt("points", 10),
                        userAnswer = if (qObj.has("userAnswer") && !qObj.isNull("userAnswer")) qObj.getString("userAnswer") else null,
                        isCorrect = if (qObj.has("isCorrect") && !qObj.isNull("isCorrect")) qObj.getBoolean("isCorrect") else null,
                        correctOption = if (qObj.has("correctOption") && !qObj.isNull("correctOption")) qObj.getString("correctOption") else null,
                        explanation = if (qObj.has("explanation") && !qObj.isNull("explanation")) qObj.getString("explanation") else null
                    )
                )
            }
            val answeredCount = obj.optInt("answeredCount", questions.count { it.userAnswer != null })
            val firstUnansweredIndex = obj.optInt(
                "firstUnansweredIndex",
                questions.indexOfFirst { it.userAnswer == null }.let { if (it >= 0) it else 0 }
            )

            return PracticeSession(
                id = obj.getString("id"),
                quizId = obj.getString("quizId"),
                subjectId = obj.getString("subjectId"),
                grade = obj.getInt("grade"),
                state = obj.getString("state"),
                score = obj.optInt("score", 0),
                maxScore = obj.optInt("maxScore", questions.size * 10),
                correctCount = obj.optInt("correctCount", 0),
                totalCount = obj.optInt("totalCount", questions.size),
                questions = questions,
                answeredCount = answeredCount,
                firstUnansweredIndex = firstUnansweredIndex
            )
        }

        fun parseWrongQuestion(obj: JSONObject): WrongQuestionEntry {
            val optArr = obj.optJSONArray("options") ?: JSONArray()
            val options = mutableListOf<WrongQuestionOption>()
            for (i in 0 until optArr.length()) {
                val o = optArr.getJSONObject(i)
                options.add(
                    WrongQuestionOption(
                        id = o.optString("id", ""),
                        text = o.optString("text", "")
                    )
                )
            }
            return WrongQuestionEntry(
                id = obj.getString("id"),
                questionId = obj.getString("questionId"),
                subjectId = obj.getString("subjectId"),
                grade = obj.getInt("grade"),
                chapter = obj.optString("chapter", "Chưa phân loại"),
                topic = obj.optString("topic", "Chủ đề chung"),
                prompt = obj.optString("prompt", ""),
                options = options,
                selectedOption = obj.optString("selectedOption", ""),
                correctOption = obj.optString("correctOption", ""),
                explanation = obj.optString("explanation", ""),
                difficulty = obj.optString("difficulty", "MEDIUM"),
                state = obj.optString("state", "NEEDS_REVIEW"),
                hasSnapshot = obj.optBoolean("hasSnapshot", true),
                canRetry = obj.optBoolean("canRetry", true),
                createdAt = obj.optString("createdAt", ""),
                updatedAt = if (obj.has("updatedAt") && !obj.isNull("updatedAt")) obj.getString("updatedAt") else null
            )
        }
    }
}
