package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.backend.BackendClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface AiPracticeRepository {
    suspend fun getConfig(): AiPracticeConfig
    suspend fun createSession(
        subjectId: String,
        grade: Int,
        topic: String,
        difficulty: String,
        questionCount: Int,
        idempotencyKey: String? = null
    ): AiPracticeSession
    suspend fun getSession(sessionId: String): AiPracticeSession
    suspend fun getResumableSession(): AiPracticeSession? = null
    suspend fun submitAnswer(sessionId: String, questionId: String, option: String): AiAnswerResult
    suspend fun submitAnswerOperation(sessionId: String, questionId: String, option: String, submissionId: String): AiAnswerResult =
        submitAnswer(sessionId, questionId, option)
    suspend fun skipQuestion(sessionId: String, questionId: String): AiAnswerResult
    suspend fun completeSession(sessionId: String): AiPracticeSession
    suspend fun reportQuestion(sessionId: String, questionId: String, reason: String, details: String): String
    suspend fun explainMore(sessionId: String, questionId: String, query: String): String
}

class BackendAiPracticeRepository(
    private val context: Context,
    private val ownerId: Int
) : AiPracticeRepository {

    private fun client(): BackendClient = BackendClient(context, ownerId)

    private val sessionPrefs = context.applicationContext.getSharedPreferences("ai_practice_sessions", Context.MODE_PRIVATE)
    private fun pointerKey(): String = SharedPreferencesSessionPointerStore.buildKey(
        BackendClient.endpoint(context), ownerId, "ai_practice", 0, "active")

    override suspend fun getResumableSession(): AiPracticeSession? {
        val id = sessionPrefs.getString(pointerKey(), null) ?: return null
        return try {
            val saved = getSession(id)
            if (saved.state == "IN_PROGRESS") saved else {
                sessionPrefs.edit().remove(pointerKey()).apply()
                null
            }
        } catch (failure: BackendClient.Failure) {
            if (failure.status == 404 || failure.status == 403) {
                sessionPrefs.edit().remove(pointerKey()).apply()
                null
            } else throw failure
        }
    }

    override suspend fun getConfig(): AiPracticeConfig = withContext(Dispatchers.IO) {
        val resp = client().request("GET", "/ai-practice/config", null, null, null)
        val subsArr = resp.optJSONArray("subjects") ?: JSONArray()
        val subs = mutableListOf<String>()
        for (i in 0 until subsArr.length()) subs.add(subsArr.getString(i))

        val diffsArr = resp.optJSONArray("difficulties") ?: JSONArray()
        val diffs = mutableListOf<String>()
        for (i in 0 until diffsArr.length()) diffs.add(diffsArr.getString(i))

        AiPracticeConfig(
            configured = resp.optBoolean("configured", false),
            subjects = subs,
            difficulties = diffs,
            minQuestions = resp.optInt("minQuestions", 1),
            maxQuestions = resp.optInt("maxQuestions", 10),
            defaultQuestions = resp.optInt("defaultQuestions", 5),
            provider = resp.optString("provider", "")
        )
    }

    override suspend fun createSession(
        subjectId: String,
        grade: Int,
        topic: String,
        difficulty: String,
        questionCount: Int,
        idempotencyKey: String?
    ): AiPracticeSession = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("subjectId", subjectId)
            .put("grade", grade)
            .put("topic", topic)
            .put("difficulty", difficulty)
            .put("questionCount", questionCount)

        val resp = client().request("POST", "/ai-practice/sessions", body, idempotencyKey, null)
        parseSession(resp).also { sessionPrefs.edit().putString(pointerKey(), it.id).apply() }
    }

    override suspend fun getSession(sessionId: String): AiPracticeSession = withContext(Dispatchers.IO) {
        val path = "/ai-practice/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8")
        val resp = client().request("GET", path, null, null, null)
        parseSession(resp)
    }

    override suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        option: String
    ): AiAnswerResult = submitAnswerOperation(sessionId, questionId, option, java.util.UUID.randomUUID().toString())

    override suspend fun submitAnswerOperation(
        sessionId: String, questionId: String, option: String, submissionId: String
    ): AiAnswerResult = withContext(Dispatchers.IO) {
        val path = "/ai-practice/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/answer"
        val body = JSONObject()
            .put("questionId", questionId)
            .put("selectedOption", option)
            .put("submissionId", submissionId)
        val resp = client().request("POST", path, body, null, null)
        parseAnswerResult(resp)
    }

    override suspend fun skipQuestion(
        sessionId: String,
        questionId: String
    ): AiAnswerResult = withContext(Dispatchers.IO) {
        val path = "/ai-practice/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/skip"
        val body = JSONObject().put("questionId", questionId)
        val resp = client().request("POST", path, body, null, null)
        parseAnswerResult(resp)
    }

    override suspend fun completeSession(sessionId: String): AiPracticeSession = withContext(Dispatchers.IO) {
        val path = "/ai-practice/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/complete"
        val resp = client().request("POST", path, JSONObject(), null, null)
        parseSession(resp).also {
            if (sessionPrefs.getString(pointerKey(), null) == sessionId) {
                sessionPrefs.edit().remove(pointerKey()).apply()
            }
        }
    }

    override suspend fun reportQuestion(
        sessionId: String,
        questionId: String,
        reason: String,
        details: String
    ): String = withContext(Dispatchers.IO) {
        val path = "/ai-practice/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/report"
        val body = JSONObject()
            .put("questionId", questionId)
            .put("reason", reason)
            .put("details", details)
        val resp = client().request("POST", path, body, null, null)
        resp.optString("message", "Đã gửi báo cáo.")
    }

    override suspend fun explainMore(
        sessionId: String,
        questionId: String,
        query: String
    ): String = withContext(Dispatchers.IO) {
        val path = "/ai-practice/sessions/" + java.net.URLEncoder.encode(sessionId, "UTF-8") + "/explain-more"
        val body = JSONObject()
            .put("questionId", questionId)
            .put("query", query)
        val resp = client().request("POST", path, body, null, null)
        resp.optString("explanation", "")
    }

    private fun parseSession(obj: JSONObject): AiPracticeSession {
        val questionsArr = obj.optJSONArray("questions") ?: JSONArray()
        val questions = mutableListOf<AiQuestion>()
        for (i in 0 until questionsArr.length()) {
            val q = questionsArr.getJSONObject(i)
            val optsObj = q.optJSONObject("options") ?: JSONObject()
            val optsMap = mutableMapOf<String, String>()
            for (key in listOf("A", "B", "C", "D")) {
                optsMap[key] = optsObj.optString(key, "")
            }
            questions.add(
                AiQuestion(
                    id = q.getString("id"),
                    prompt = q.getString("prompt"),
                    options = optsMap,
                    hint = q.optString("hint", ""),
                    order = q.optInt("order", i + 1),
                    difficulty = q.optString("difficulty", "MEDIUM"),
                    correctOption = if (q.has("correctOption")) q.getString("correctOption") else null,
                    explanation = if (q.has("explanation")) q.getString("explanation") else null
                )
            )
        }

        val answersObj = obj.optJSONObject("answers") ?: JSONObject()
        val answersMap = mutableMapOf<String, AiAnswerRecord>()
        val ansKeys = answersObj.keys()
        while (ansKeys.hasNext()) {
            val k = ansKeys.next()
            val a = answersObj.getJSONObject(k)
            answersMap[k] = AiAnswerRecord(
                selectedOption = if (a.has("selectedOption") && !a.isNull("selectedOption")) a.getString("selectedOption") else null,
                isCorrect = a.optBoolean("isCorrect", false),
                firstTryCorrect = a.optBoolean("firstTryCorrect", false),
                attemptsCount = a.optInt("attemptsCount", 1),
                revealed = a.optBoolean("revealed", false),
                skipped = a.optBoolean("skipped", false)
            )
        }

        val summaryObj = obj.optJSONObject("summary")
        val summary = if (summaryObj != null) {
            AiPracticeSummary(
                totalQuestions = summaryObj.optInt("totalQuestions", questions.size),
                firstTryCorrectCount = summaryObj.optInt("firstTryCorrectCount", 0),
                assistedCount = summaryObj.optInt("assistedCount", 0),
                incorrectCount = summaryObj.optInt("incorrectCount", 0),
                skippedCount = summaryObj.optInt("skippedCount", 0),
                accuracyFirstTry = summaryObj.optDouble("accuracyFirstTry", 0.0)
            )
        } else null

        return AiPracticeSession(
            id = obj.getString("id"),
            subjectId = obj.getString("subjectId"),
            subjectName = obj.optString("subjectName", obj.getString("subjectId")),
            grade = obj.getInt("grade"),
            topic = obj.getString("topic"),
            difficulty = obj.getString("difficulty"),
            questionCount = obj.getInt("questionCount"),
            currentIndex = obj.optInt("currentIndex", 0),
            state = obj.optString("state", "IN_PROGRESS"),
            questions = questions,
            answers = answersMap,
            summary = summary
        )
    }

    private fun parseAnswerResult(obj: JSONObject): AiAnswerResult {
        return AiAnswerResult(
            questionId = obj.getString("questionId"),
            isCorrect = obj.optBoolean("isCorrect", false),
            selectedOption = if (obj.has("selectedOption") && !obj.isNull("selectedOption")) obj.getString("selectedOption") else null,
            correctOption = obj.optString("correctOption", ""),
            explanation = obj.optString("explanation", ""),
            hint = obj.optString("hint", ""),
            firstTryCorrect = obj.optBoolean("firstTryCorrect", false),
            attemptsCount = obj.optInt("attemptsCount", 1),
            isSkipped = obj.optBoolean("isSkipped", false) || obj.optBoolean("skipped", false)
        )
    }
}
