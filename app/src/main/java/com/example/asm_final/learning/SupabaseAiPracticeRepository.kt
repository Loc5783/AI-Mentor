package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SupabaseAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Supabase-backed AI practice. Question generation, answer keys, scoring and reports stay server-side. */
class SupabaseAiPracticeRepository(context: Context, private val owner: Int) : AiPracticeRepository {
    override val supportsAnswerConfidence = true
    private val app = context.applicationContext
    private val accounts = SupabaseAccounts(app)
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(65, TimeUnit.SECONDS).build()
    private val prefs = app.getSharedPreferences("ai_practice_cloud_sessions", Context.MODE_PRIVATE)
    private val subjects = listOf("math", "physics", "chemistry", "biology", "literature",
        "history", "geography", "english", "economics_law")
    private val names = listOf("Toán học", "Vật lí", "Hóa học", "Sinh học", "Ngữ văn",
        "Lịch sử", "Địa lí", "Tiếng Anh", "Giáo dục kinh tế và pháp luật")

    private fun userId(): String = RemoteAccountLinks(app).use {
        it.remoteId(owner, BuildConfig.SUPABASE_URL)
    }.also { require(it.matches(Regex("[0-9a-fA-F-]{36}"))) }

    private fun pointerKey() = "${userId()}:active"

    private fun request(method: String, path: String, body: JSONObject? = null): String =
        accounts.authorized(owner) { token ->
            require(path.startsWith("/") && !path.contains(".."))
            val builder = Request.Builder().url(BuildConfig.SUPABASE_URL + path)
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
            val payload = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
            builder.method(method, payload)
            http.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val error = runCatching { JSONObject(text) }.getOrNull()
                    val code = error?.optString("error_code")?.takeIf { it.isNotBlank() }
                        ?: error?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "CLOUD_REQUEST_FAILED"
                    throw BackendClient.Failure(code, response.code)
                }
                text
            }
        }

    private fun api(body: JSONObject): JSONObject = JSONObject(request(
        "POST", "/rest/v1/rpc/mentor_ai_practice_api", body))

    private fun sessionArgs(action: String, sessionId: String) = JSONObject()
        .put("p_action", action).put("p_session_id", UUID.fromString(sessionId).toString())

    override suspend fun getConfig(): AiPracticeConfig = withContext(Dispatchers.IO) {
        request("GET", "/auth/v1/user")
        AiPracticeConfig(true, subjects, listOf("EASY", "MEDIUM", "HARD"), 1, 10, 5, "supabase")
    }

    override suspend fun getResumableSession(): AiPracticeSession? = withContext(Dispatchers.IO) {
        val cachedId = prefs.getString(pointerKey(), null)
        if (cachedId != null) {
            val cached = runCatching { parseSession(api(sessionArgs("get", cachedId))) }.getOrNull()
            if (cached?.state == "IN_PROGRESS") return@withContext cached
            prefs.edit().remove(pointerKey()).apply()
        }
        val raw = request("POST", "/rest/v1/rpc/mentor_ai_practice_api",
            JSONObject().put("p_action", "resumable"))
        if (raw == "null" || raw.isBlank()) return@withContext null
        parseSession(JSONObject(raw)).also { prefs.edit().putString(pointerKey(), it.id).apply() }
    }

    override suspend fun createSession(subjectId: String, grade: Int, topic: String,
                                       difficulty: String, questionCount: Int,
                                       idempotencyKey: String?): AiPracticeSession = withContext(Dispatchers.IO) {
        require(subjectId in subjects && grade in 10..12 && difficulty in listOf("EASY", "MEDIUM", "HARD"))
        require(topic.isNotBlank() && topic.length <= 100 && questionCount in 1..10)
        val sessionId = idempotencyKey?.let { runCatching { UUID.fromString(it).toString() }.getOrNull() }
            ?: UUID.randomUUID().toString()
        val body = JSONObject().put("mode", "practice_generate").put("sessionId", sessionId)
            .put("subjectId", subjectId).put("grade", grade).put("topic", topic.trim())
            .put("difficulty", difficulty).put("questionCount", questionCount)
        val generated = JSONObject(request("POST", "/functions/v1/mentor-ai", body))
        parseSession(generated).also { prefs.edit().putString(pointerKey(), it.id).apply() }
    }

    override suspend fun getSession(sessionId: String): AiPracticeSession = withContext(Dispatchers.IO) {
        parseSession(api(sessionArgs("get", sessionId)))
    }

    override suspend fun submitAnswer(sessionId: String, questionId: String, option: String): AiAnswerResult =
        submitAnswerOperation(sessionId, questionId, option, UUID.randomUUID().toString())

    override suspend fun submitAnswerOperation(sessionId: String, questionId: String,
                                               option: String, submissionId: String): AiAnswerResult = withContext(Dispatchers.IO) {
        require(option in listOf("A", "B", "C", "D") && questionId.length <= 100)
        val response = api(sessionArgs("answer", sessionId).put("p_question_id", questionId)
            .put("p_option", option).put("p_submission_id", UUID.fromString(submissionId).toString()))
        parseAnswerResult(response)
    }

    override suspend fun skipQuestion(sessionId: String, questionId: String): AiAnswerResult = withContext(Dispatchers.IO) {
        parseAnswerResult(api(sessionArgs("skip", sessionId).put("p_question_id", questionId)))
    }

    override suspend fun completeSession(sessionId: String): AiPracticeSession = withContext(Dispatchers.IO) {
        parseSession(api(sessionArgs("complete", sessionId))).also {
            if (prefs.getString(pointerKey(), null) == sessionId) prefs.edit().remove(pointerKey()).apply()
        }
    }

    override suspend fun reportQuestion(sessionId: String, questionId: String,
                                        reason: String, details: String): String = withContext(Dispatchers.IO) {
        require(reason.isNotBlank() && reason.length <= 200 && details.length <= 2000)
        api(sessionArgs("report", sessionId).put("p_question_id", questionId)
            .put("p_reason", reason.trim()).put("p_details", details.trim()))
            .optString("message", "Đã gửi báo cáo.")
    }

    override suspend fun explainMore(sessionId: String, questionId: String, query: String): String = withContext(Dispatchers.IO) {
        require(query.isNotBlank() && query.length <= 500)
        JSONObject(request("POST", "/functions/v1/mentor-ai", JSONObject()
            .put("mode", "practice_explain").put("sessionId", UUID.fromString(sessionId).toString())
            .put("questionId", questionId).put("query", query.trim())))
            .optString("text", "")
    }

    override suspend fun getAnswerConfidence(sessionId: String, questionId: String): AnswerConfidence? =
        withContext(Dispatchers.IO) {
            val response = JSONObject(request("POST", "/rest/v1/rpc/mentor_ai_answer_confidence_api",
                sessionArgs("get", sessionId).put("p_question_id", questionId)))
            response.optString("confidence").takeIf { it.isNotBlank() && it != "null" }
                ?.let { AnswerConfidence.valueOf(it) }
        }

    override suspend fun setAnswerConfidence(sessionId: String, questionId: String,
                                             attemptCount: Int, confidence: AnswerConfidence): AnswerConfidence =
        withContext(Dispatchers.IO) {
            require(attemptCount > 0)
            val response = JSONObject(request("POST", "/rest/v1/rpc/mentor_ai_answer_confidence_api",
                sessionArgs("set", sessionId).put("p_question_id", questionId)
                    .put("p_attempt_count", attemptCount).put("p_confidence", confidence.name)))
            AnswerConfidence.valueOf(response.getString("confidence"))
        }

    private fun parseAnswerResult(obj: JSONObject) = AiAnswerResult(
        questionId = obj.getString("questionId"),
        isCorrect = obj.optBoolean("isCorrect"),
        selectedOption = if (obj.has("selectedOption") && !obj.isNull("selectedOption")) obj.getString("selectedOption") else null,
        correctOption = obj.optString("correctOption", ""),
        explanation = obj.optString("explanation", ""),
        hint = obj.optString("hint", ""),
        firstTryCorrect = obj.optBoolean("firstTryCorrect"),
        attemptsCount = obj.optInt("attemptsCount", 1),
        isSkipped = obj.optBoolean("isSkipped")
    )

    private fun parseSession(row: JSONObject): AiPracticeSession {
        val all = row.optJSONArray("questions_snapshot") ?: JSONArray()
        val answersJson = row.optJSONObject("answers") ?: JSONObject()
        val questions = (0 until all.length()).map { i ->
            val q = all.getJSONObject(i)
            val choices = q.optJSONObject("options") ?: JSONObject()
            val revealed = row.optString("state") == "COMPLETED" ||
                answersJson.optJSONObject(q.getString("id"))?.optBoolean("revealed") == true
            AiQuestion(q.getString("id"), q.getString("prompt"),
                listOf("A", "B", "C", "D").associateWith { choices.optString(it, "") },
                q.optString("hint"), q.optInt("order", i + 1), row.optString("difficulty", "MEDIUM"),
                q.optString("correctOption").takeIf { revealed && it.isNotBlank() },
                q.optString("explanation").takeIf { revealed && it.isNotBlank() })
        }
        val answers = mutableMapOf<String, AiAnswerRecord>()
        val keys = answersJson.keys()
        while (keys.hasNext()) {
            val key = keys.next(); val a = answersJson.getJSONObject(key)
            answers[key] = AiAnswerRecord(
                a.optString("selectedOption").takeUnless { it.isBlank() || it == "null" },
                a.optBoolean("isCorrect"), a.optBoolean("firstTryCorrect"),
                a.optInt("attemptsCount"), a.optBoolean("revealed"), a.optBoolean("skipped"))
        }
        val summary = row.optString("summary").takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }?.let {
                AiPracticeSummary(it.optInt("totalQuestions"), it.optInt("firstTryCorrectCount"),
                    it.optInt("assistedCount"), it.optInt("incorrectCount"),
                    it.optInt("skippedCount"), it.optDouble("accuracyFirstTry"))
            }
        val subject = row.optString("subject_id")
        return AiPracticeSession(row.getString("id"), subject,
            names.getOrElse(subjects.indexOf(subject)) { subject }, row.getInt("grade"),
            row.getString("topic"), row.getString("difficulty"), row.getInt("question_count"),
            row.optInt("current_index"), row.optString("state", "IN_PROGRESS"), questions, answers, summary)
    }
}
