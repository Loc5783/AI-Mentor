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
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Online AI practice. Generation uses the existing authenticated mentor-ai function;
 * sessions and the wrong-answer notebook are stored in the learner's Supabase account.
 */
class SupabaseAiPracticeRepository(context: Context, private val owner: Int) : AiPracticeRepository {
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

    private fun encoded(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun sessionPath(id: String): String {
        require(runCatching { UUID.fromString(id) }.isSuccess)
        return "/rest/v1/mentor_ai_practice_sessions?id=eq.$id&user_id=eq.${userId()}&select=*"
    }
    private fun pointerKey() = "${userId()}:active"

    private fun request(method: String, path: String, body: JSONObject? = null,
                        prefer: String? = null): String = accounts.authorized(owner) { token ->
        require(path.startsWith("/") && !path.contains(".."))
        val builder = Request.Builder().url(BuildConfig.SUPABASE_URL + path)
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
        if (prefer != null) builder.header("Prefer", prefer)
        val payload = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
        builder.method(method, payload)
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val code = runCatching { JSONObject(text).optString("error_code") }.getOrDefault("")
                throw BackendClient.Failure(if (code.isNotBlank()) code else "CLOUD_REQUEST_FAILED", response.code)
            }
            text
        }
    }

    override suspend fun getConfig(): AiPracticeConfig = withContext(Dispatchers.IO) {
        request("GET", "/auth/v1/user")
        AiPracticeConfig(true, subjects, listOf("EASY", "MEDIUM", "HARD"), 1, 10, 5, "supabase")
    }

    override suspend fun getResumableSession(): AiPracticeSession? = withContext(Dispatchers.IO) {
        val pointer = prefs.getString(pointerKey(), null)
        if (pointer != null) {
            val saved = runCatching { getSession(pointer) }.getOrNull()
            if (saved?.state == "IN_PROGRESS") return@withContext saved
            prefs.edit().remove(pointerKey()).apply()
        }
        val path = "/rest/v1/mentor_ai_practice_sessions?user_id=eq.${userId()}" +
            "&state=eq.IN_PROGRESS&select=*&order=updated_at.desc&limit=1"
        val rows = JSONArray(request("GET", path))
        if (rows.length() == 0) null else parseSession(rows.getJSONObject(0)).also {
            prefs.edit().putString(pointerKey(), it.id).apply()
        }
    }

    override suspend fun createSession(subjectId: String, grade: Int, topic: String,
                                       difficulty: String, questionCount: Int,
                                       idempotencyKey: String?): AiPracticeSession = withContext(Dispatchers.IO) {
        require(subjectId in subjects && grade in 10..12 && difficulty in listOf("EASY", "MEDIUM", "HARD"))
        require(topic.isNotBlank() && topic.length <= 100 && questionCount in 1..10)
        val id = idempotencyKey?.let { runCatching { UUID.fromString(it).toString() }.getOrNull() }
            ?: UUID.randomUUID().toString()
        val existing = JSONArray(request("GET", sessionPath(id)))
        if (existing.length() > 0) return@withContext parseSession(existing.getJSONObject(0)).also {
            require(it.subjectId == subjectId && it.grade == grade && it.topic == topic.trim() &&
                it.difficulty == difficulty && it.questionCount == questionCount)
        }
        val questions = JSONArray()
        val seen = mutableSetOf<String>()
        while (questions.length() < questionCount) {
            val count = minOf(5, questionCount - questions.length())
            val subjectName = names[subjects.indexOf(subjectId)]
            val instruction = "Tạo đúng $count câu hỏi trắc nghiệm độc lập cho môn $subjectName lớp $grade, " +
                "chủ đề '${topic.trim()}', mức ${when (difficulty) { "EASY" -> "dễ"; "HARD" -> "nâng cao"; else -> "trung bình" }}. " +
                "Mỗi câu phải có một đáp án đúng xác định, 4 phương án A/B/C/D khác nhau, " +
                "lời giải ngắn có thể kiểm tra, gợi ý không lộ đáp án. Tránh câu hỏi mơ hồ. " +
                "Chỉ trả về JSON hợp lệ, không Markdown, dạng " +
                "{\"questions\":[{\"prompt\":\"...\",\"options\":{\"A\":\"...\",\"B\":\"...\",\"C\":\"...\",\"D\":\"...\"}," +
                "\"correctOption\":\"A\",\"explanation\":\"...\",\"hint\":\"...\"}]}"
            val turns = JSONArray().put(JSONObject().put("role", "user").put("text", instruction)
                .put("files", JSONArray()))
            val ai = JSONObject(request("POST", "/functions/v1/mentor-ai",
                JSONObject().put("mode", "general").put("turns", turns)))
            if (ai.optBoolean("truncated")) throw BackendClient.Failure("AI_VALIDATION_FAILED", 502)
            val raw = ai.optString("text").trim().removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val generated = runCatching { JSONObject(raw).getJSONArray("questions") }
                .getOrElse { throw BackendClient.Failure("AI_VALIDATION_FAILED", 502) }
            if (generated.length() != count) throw BackendClient.Failure("AI_VALIDATION_FAILED", 502)
            for (i in 0 until generated.length()) {
                val input = generated.getJSONObject(i)
                val prompt = input.optString("prompt").trim()
                val options = input.optJSONObject("options") ?: throw BackendClient.Failure("AI_VALIDATION_FAILED", 502)
                val choices = listOf("A", "B", "C", "D").map { options.optString(it).trim() }
                val answer = input.optString("correctOption").trim().uppercase()
                val explanation = input.optString("explanation").trim()
                if (prompt.length !in 10..2000 || !seen.add(prompt.lowercase()) ||
                    choices.any { it.isEmpty() || it.length > 500 } || choices.distinct().size != 4 ||
                    answer !in listOf("A", "B", "C", "D") || explanation.isEmpty() || explanation.length > 2000)
                    throw BackendClient.Failure("AI_VALIDATION_FAILED", 502)
                questions.put(JSONObject().put("id", "ai_q_${id.take(8)}_${questions.length() + 1}")
                    .put("prompt", prompt).put("options", JSONObject().apply {
                        listOf("A", "B", "C", "D").forEachIndexed { index, key -> put(key, choices[index]) }
                    }).put("correctOption", answer).put("explanation", explanation)
                    .put("hint", input.optString("hint").take(500))
                    .put("order", questions.length() + 1))
            }
        }
        val row = JSONObject().put("id", id).put("user_id", userId())
            .put("subject_id", subjectId).put("grade", grade).put("topic", topic.trim())
            .put("difficulty", difficulty).put("question_count", questionCount)
            .put("questions_snapshot", questions).put("answers", JSONObject())
        try {
            request("POST", "/rest/v1/mentor_ai_practice_sessions", row)
        } catch (conflict: BackendClient.Failure) {
            if (conflict.status != 409) throw conflict
        }
        getSession(id).also { prefs.edit().putString(pointerKey(), it.id).apply() }
    }

    override suspend fun getSession(sessionId: String): AiPracticeSession = withContext(Dispatchers.IO) {
        val rows = JSONArray(request("GET", sessionPath(sessionId)))
        if (rows.length() == 0) throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
        parseSession(rows.getJSONObject(0))
    }

    override suspend fun submitAnswer(sessionId: String, questionId: String, option: String): AiAnswerResult =
        submitAnswerOperation(sessionId, questionId, option, UUID.randomUUID().toString())

    override suspend fun submitAnswerOperation(sessionId: String, questionId: String,
                                               option: String, submissionId: String): AiAnswerResult = withContext(Dispatchers.IO) {
        require(option in listOf("A", "B", "C", "D"))
        val row = loadRow(sessionId)
        require(row.getString("state") == "IN_PROGRESS")
        val question = findQuestion(row, questionId)
        val answers = row.getJSONObject("answers")
        val previous = answers.optJSONObject(questionId)
        if (previous?.optString("submissionId") == submissionId) {
            if (!previous.optBoolean("isCorrect")) recordWrong(row, question)
            return@withContext result(question, previous)
        }
        val correct = option == question.getString("correctOption")
        val record = JSONObject().put("selectedOption", option).put("isCorrect", correct)
            .put("firstTryCorrect", previous?.optBoolean("firstTryCorrect") ?: correct)
            .put("attemptsCount", (previous?.optInt("attemptsCount") ?: 0) + 1)
            .put("revealed", true).put("skipped", false).put("submissionId", submissionId)
        answers.put(questionId, record)
        updateRow(row, JSONObject().put("answers", answers)
            .put("current_index", maxOf(row.optInt("current_index"), question.getInt("order"))))
        if (!correct) recordWrong(row, question)
        result(question, record)
    }

    override suspend fun skipQuestion(sessionId: String, questionId: String): AiAnswerResult = withContext(Dispatchers.IO) {
        val row = loadRow(sessionId)
        require(row.getString("state") == "IN_PROGRESS")
        val question = findQuestion(row, questionId)
        val answers = row.getJSONObject("answers")
        val previous = answers.optJSONObject(questionId)
        if (previous != null && !previous.optBoolean("skipped"))
            throw BackendClient.Failure("ANSWER_ALREADY_RECORDED", 409)
        val record = previous ?: JSONObject().put("selectedOption", JSONObject.NULL)
            .put("isCorrect", false).put("firstTryCorrect", false).put("attemptsCount", 0)
            .put("revealed", true).put("skipped", true)
        if (previous == null) {
            answers.put(questionId, record)
            updateRow(row, JSONObject().put("answers", answers)
                .put("current_index", maxOf(row.optInt("current_index"), question.getInt("order"))))
            recordWrong(row, question)
        } else {
            recordWrong(row, question)
        }
        result(question, record)
    }

    override suspend fun completeSession(sessionId: String): AiPracticeSession = withContext(Dispatchers.IO) {
        val row = loadRow(sessionId)
        if (row.optString("state") != "COMPLETED") {
            val questions = row.getJSONArray("questions_snapshot")
            val answers = row.getJSONObject("answers")
            var first = 0; var assisted = 0; var incorrect = 0; var skipped = 0
            for (i in 0 until questions.length()) {
                val answer = answers.optJSONObject(questions.getJSONObject(i).getString("id"))
                when {
                    answer == null || answer.optBoolean("skipped") -> skipped++
                    answer.optBoolean("firstTryCorrect") -> first++
                    answer.optBoolean("isCorrect") -> assisted++
                    else -> incorrect++
                }
            }
            val summary = JSONObject().put("totalQuestions", questions.length())
                .put("firstTryCorrectCount", first).put("assistedCount", assisted)
                .put("incorrectCount", incorrect).put("skippedCount", skipped)
                .put("accuracyFirstTry", first * 100.0 / questions.length())
            updateRow(row, JSONObject().put("state", "COMPLETED").put("summary", summary.toString()))
        }
        prefs.edit().remove(pointerKey()).apply()
        getSession(sessionId)
    }

    override suspend fun reportQuestion(sessionId: String, questionId: String,
                                        reason: String, details: String): String = withContext(Dispatchers.IO) {
        val row = loadRow(sessionId); findQuestion(row, questionId)
        require(reason.isNotBlank() && reason.length <= 200 && details.length <= 2000)
        request("POST", "/rest/v1/mentor_ai_question_reports",
            JSONObject().put("user_id", userId()).put("session_id", sessionId)
                .put("question_id", questionId).put("reason", reason.trim()).put("details", details.trim()))
        "Cảm ơn bạn đã phản hồi chất lượng câu hỏi."
    }

    override suspend fun explainMore(sessionId: String, questionId: String, query: String): String = withContext(Dispatchers.IO) {
        require(query.isNotBlank() && query.length <= 500)
        val row = loadRow(sessionId); val question = findQuestion(row, questionId)
        val answer = row.getJSONObject("answers").optJSONObject(questionId)
        if (row.optString("state") != "COMPLETED" && answer?.optBoolean("revealed") != true)
            throw BackendClient.Failure("ANSWER_REQUIRED", 409)
        val prompt = "Giải thích thêm cho học sinh lớp ${row.getInt("grade")} về câu hỏi: " +
            "${question.getString("prompt")}. Đáp án đúng ${question.getString("correctOption")}. " +
            "Lời giải gốc: ${question.getString("explanation")}. Học sinh hỏi: ${query.trim()}. " +
            "Trả lời ngắn, đúng kiến thức; nếu lời giải gốc có vấn đề, hãy chỉ ra."
        val turns = JSONArray().put(JSONObject().put("role", "user").put("text", prompt).put("files", JSONArray()))
        JSONObject(request("POST", "/functions/v1/mentor-ai",
            JSONObject().put("mode", "general").put("turns", turns))).getString("text")
    }

    private fun loadRow(id: String): JSONObject {
        val rows = JSONArray(request("GET", sessionPath(id)))
        if (rows.length() == 0) throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
        return rows.getJSONObject(0)
    }

    private fun findQuestion(row: JSONObject, id: String): JSONObject {
        val questions = row.getJSONArray("questions_snapshot")
        for (i in 0 until questions.length()) {
            val question = questions.getJSONObject(i)
            if (question.getString("id") == id) return question
        }
        throw BackendClient.Failure("QUESTION_NOT_FOUND", 404)
    }

    private fun updateRow(row: JSONObject, change: JSONObject) {
        change.put("updated_at", Instant.now().toString())
        val path = sessionPath(row.getString("id")) + "&updated_at=eq.${encoded(row.getString("updated_at"))}"
        val updated = JSONArray(request("PATCH", path, change, "return=representation"))
        if (updated.length() != 1) throw BackendClient.Failure("SESSION_CHANGED", 409)
    }

    private fun recordWrong(row: JSONObject, question: JSONObject) {
        request("POST", "/rest/v1/rpc/mentor_ai_record_wrong",
            JSONObject().put("p_session_id", row.getString("id"))
                .put("p_question_id", question.getString("id")))
    }

    private fun result(question: JSONObject, record: JSONObject) = AiAnswerResult(
        question.getString("id"), record.optBoolean("isCorrect"),
        record.optString("selectedOption").takeUnless { it.isBlank() || it == "null" },
        question.getString("correctOption"), question.getString("explanation"),
        question.optString("hint"), record.optBoolean("firstTryCorrect"),
        record.optInt("attemptsCount"), record.optBoolean("skipped"))

    private fun parseSession(row: JSONObject): AiPracticeSession {
        val all = row.getJSONArray("questions_snapshot")
        val answersJson = row.getJSONObject("answers")
        val questions = (0 until all.length()).map { i ->
            val q = all.getJSONObject(i)
            val choices = q.getJSONObject("options")
            val revealed = row.optString("state") == "COMPLETED" ||
                answersJson.optJSONObject(q.getString("id"))?.optBoolean("revealed") == true
            AiQuestion(q.getString("id"), q.getString("prompt"),
                listOf("A", "B", "C", "D").associateWith { choices.getString(it) },
                q.optString("hint"), q.optInt("order", i + 1), row.getString("difficulty"),
                q.getString("correctOption").takeIf { revealed },
                q.getString("explanation").takeIf { revealed })
        }
        val answers = mutableMapOf<String, AiAnswerRecord>()
        val keys = answersJson.keys()
        while (keys.hasNext()) {
            val key = keys.next(); val a = answersJson.getJSONObject(key)
            answers[key] = AiAnswerRecord(a.optString("selectedOption").takeUnless { it.isBlank() || it == "null" },
                a.optBoolean("isCorrect"), a.optBoolean("firstTryCorrect"),
                a.optInt("attemptsCount"), a.optBoolean("revealed"), a.optBoolean("skipped"))
        }
        val summary = row.optString("summary").takeIf { it.isNotBlank() && it != "null" }
            ?.let { JSONObject(it) }?.let {
                AiPracticeSummary(it.getInt("totalQuestions"), it.getInt("firstTryCorrectCount"),
                    it.getInt("assistedCount"), it.getInt("incorrectCount"),
                    it.getInt("skippedCount"), it.getDouble("accuracyFirstTry"))
            }
        val subject = row.getString("subject_id")
        return AiPracticeSession(row.getString("id"), subject, names.getOrElse(subjects.indexOf(subject)) { subject },
            row.getInt("grade"), row.getString("topic"), row.getString("difficulty"),
            row.getInt("question_count"), row.optInt("current_index"), row.getString("state"),
            questions, answers, summary)
    }
}
