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
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

interface MisconceptionRepository {
    val ownerId: Int
    suspend fun startSession(
        id: String,
        sessionId: String?,
        questionId: String,
        subjectId: String,
        grade: Int,
        questionPrompt: String,
        selectedOption: String?,
        misconceptionReason: String,
        reasonType: MisconceptionReasonType,
        aiHint: String,
        aiCounterexample: String,
        prop1Prompt: String,
        prop1Correct: Boolean,
        prop2Prompt: String,
        prop2Correct: Boolean
    ): MisconceptionBusterSession

    suspend fun verifySession(
        id: String,
        prop1Ans: Boolean,
        prop2Ans: Boolean
    ): MisconceptionBusterSession

    suspend fun getSession(id: String): MisconceptionBusterSession?
    suspend fun getActiveDraft(sessionId: String?, questionId: String): MisconceptionBusterSession?
    suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession
}

class DefaultMisconceptionRepository(
    private val context: Context,
    override val ownerId: Int
) : MisconceptionRepository {

    private val app = context.applicationContext
    private val accounts = SupabaseAccounts(app)
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs = app.getSharedPreferences("misconception_drafts", Context.MODE_PRIVATE)

    private fun draftKey(sessionId: String?, questionId: String): String =
        "draft_${ownerId}_${sessionId.orEmpty()}_$questionId"

    private fun sessionKey(id: String): String =
        "session_${ownerId}_$id"

    private fun userId(): String? = runCatching {
        RemoteAccountLinks(app).use {
            it.remoteId(ownerId, BuildConfig.SUPABASE_URL)
        }.takeIf { it.matches(Regex("[0-9a-fA-F-]{36}")) }
    }.getOrNull()

    private fun request(path: String, body: JSONObject): JSONObject =
        accounts.authorized(ownerId) { token ->
            val builder = Request.Builder().url(BuildConfig.SUPABASE_URL + path)
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
            val payload = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            builder.post(payload)
            http.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val error = runCatching { JSONObject(text) }.getOrNull()
                    val code = error?.optString("error_code")?.takeIf { it.isNotBlank() }
                        ?: error?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "CLOUD_REQUEST_FAILED"
                    throw BackendClient.Failure(code, response.code)
                }
                if (text.isBlank() || text == "null") JSONObject() else JSONObject(text)
            }
        }

    private fun saveLocalSession(s: MisconceptionBusterSession) {
        val json = JSONObject()
            .put("id", s.id)
            .put("ownerId", s.ownerId)
            .put("sessionId", s.sessionId ?: "")
            .put("questionId", s.questionId)
            .put("subjectId", s.subjectId)
            .put("grade", s.grade)
            .put("questionPrompt", s.questionPrompt)
            .put("selectedOption", s.selectedOption ?: "")
            .put("misconceptionReason", s.misconceptionReason)
            .put("reasonType", s.reasonType.name)
            .put("aiHint", s.aiHint)
            .put("aiCounterexample", s.aiCounterexample)
            .put("prop1Prompt", s.prop1Prompt)
            .put("prop1Correct", s.prop1Correct)
            .put("prop1UserAns", s.prop1UserAns?.toString() ?: "")
            .put("prop2Prompt", s.prop2Prompt)
            .put("prop2Correct", s.prop2Correct)
            .put("prop2UserAns", s.prop2UserAns?.toString() ?: "")
            .put("verificationStatus", s.verificationStatus.name)
            .put("answeredAt", s.answeredAt)
            .put("isDraft", s.isDraft)

        prefs.edit()
            .putString(sessionKey(s.id), json.toString())
            .putString(draftKey(s.sessionId, s.questionId), json.toString())
            .apply()
    }

    private fun parseLocalSession(raw: String?): MisconceptionBusterSession? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            if (obj.optInt("ownerId", -1) != ownerId) return null
            MisconceptionBusterSession(
                id = obj.getString("id"),
                ownerId = obj.getInt("ownerId"),
                sessionId = obj.optString("sessionId").takeIf { it.isNotBlank() },
                questionId = obj.getString("questionId"),
                subjectId = obj.getString("subjectId"),
                grade = obj.getInt("grade"),
                questionPrompt = obj.getString("questionPrompt"),
                selectedOption = obj.optString("selectedOption").takeIf { it.isNotBlank() },
                misconceptionReason = obj.getString("misconceptionReason"),
                reasonType = MisconceptionReasonType.valueOf(obj.getString("reasonType")),
                aiHint = obj.getString("aiHint"),
                aiCounterexample = obj.getString("aiCounterexample"),
                prop1Prompt = obj.getString("prop1Prompt"),
                prop1Correct = obj.getBoolean("prop1Correct"),
                prop1UserAns = obj.optString("prop1UserAns").takeIf { it.isNotBlank() }?.toBooleanStrictOrNull(),
                prop2Prompt = obj.getString("prop2Prompt"),
                prop2Correct = obj.getBoolean("prop2Correct"),
                prop2UserAns = obj.optString("prop2UserAns").takeIf { it.isNotBlank() }?.toBooleanStrictOrNull(),
                verificationStatus = MisconceptionVerificationStatus.valueOf(obj.getString("verificationStatus")),
                answeredAt = obj.optLong("answeredAt", System.currentTimeMillis()),
                isDraft = obj.optBoolean("isDraft", false)
            )
        }.getOrNull()
    }

    override suspend fun startSession(
        id: String,
        sessionId: String?,
        questionId: String,
        subjectId: String,
        grade: Int,
        questionPrompt: String,
        selectedOption: String?,
        misconceptionReason: String,
        reasonType: MisconceptionReasonType,
        aiHint: String,
        aiCounterexample: String,
        prop1Prompt: String,
        prop1Correct: Boolean,
        prop2Prompt: String,
        prop2Correct: Boolean
    ): MisconceptionBusterSession = withContext(Dispatchers.IO) {
        val draft = MisconceptionBusterSession(
            id = id,
            ownerId = ownerId,
            sessionId = sessionId,
            questionId = questionId,
            subjectId = subjectId,
            grade = grade,
            questionPrompt = questionPrompt,
            selectedOption = selectedOption,
            misconceptionReason = misconceptionReason,
            reasonType = reasonType,
            aiHint = aiHint,
            aiCounterexample = aiCounterexample,
            prop1Prompt = prop1Prompt,
            prop1Correct = prop1Correct,
            prop2Prompt = prop2Prompt,
            prop2Correct = prop2Correct,
            verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW,
            isDraft = true
        )
        saveLocalSession(draft)

        // Try pushing to Supabase if online
        try {
            val body = JSONObject()
                .put("p_action", "start")
                .put("p_id", id)
                .put("p_question_id", questionId)
                .put("p_subject_id", subjectId)
                .put("p_grade", grade)
                .put("p_question_prompt", questionPrompt)
                .put("p_misconception_reason", misconceptionReason)
                .put("p_reason_type", reasonType.name)
                .put("p_ai_hint", aiHint)
                .put("p_ai_counterexample", aiCounterexample)
                .put("p_prop1_prompt", prop1Prompt)
                .put("p_prop1_correct", prop1Correct)
                .put("p_prop2_prompt", prop2Prompt)
                .put("p_prop2_correct", prop2Correct)

            if (!sessionId.isNullOrBlank()) {
                runCatching { UUID.fromString(sessionId) }.getOrNull()?.let {
                    body.put("p_session_id", it.toString())
                }
            }
            if (!selectedOption.isNullOrBlank()) {
                body.put("p_selected_option", selectedOption)
            }

            val resp = request("/rest/v1/rpc/mentor_misconception_api", body)
            val serverStatus = MisconceptionVerificationStatus.valueOf(
                resp.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)
            )
            val synced = draft.copy(
                verificationStatus = serverStatus,
                isDraft = false
            )
            saveLocalSession(synced)
            synced
        } catch (e: Exception) {
            // Keep local draft if offline or server not ready
            draft
        }
    }

    override suspend fun verifySession(
        id: String,
        prop1Ans: Boolean,
        prop2Ans: Boolean
    ): MisconceptionBusterSession = withContext(Dispatchers.IO) {
        val current = getSession(id) ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)
        current.prop1UserAns = prop1Ans
        current.prop2UserAns = prop2Ans

        // Try server-side verification
        try {
            val body = JSONObject()
                .put("p_action", "verify")
                .put("p_id", id)
                .put("p_prop1_ans", prop1Ans)
                .put("p_prop2_ans", prop2Ans)

            val resp = request("/rest/v1/rpc/mentor_misconception_api", body)
            val serverStatus = MisconceptionVerificationStatus.valueOf(
                resp.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)
            )
            val synced = current.copy(
                prop1UserAns = prop1Ans,
                prop2UserAns = prop2Ans,
                verificationStatus = serverStatus,
                isDraft = false
            )
            saveLocalSession(synced)
            synced
        } catch (e: Exception) {
            // Offline: do NOT claim RESOLVED! Keep as draft with NEEDS_REVIEW
            val offlineDraft = current.copy(
                prop1UserAns = prop1Ans,
                prop2UserAns = prop2Ans,
                verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW,
                isDraft = true
            )
            saveLocalSession(offlineDraft)
            offlineDraft
        }
    }

    override suspend fun getSession(id: String): MisconceptionBusterSession? = withContext(Dispatchers.IO) {
        val local = parseLocalSession(prefs.getString(sessionKey(id), null))

        // Try checking server for freshest status
        try {
            val body = JSONObject()
                .put("p_action", "get")
                .put("p_id", id)
            val resp = request("/rest/v1/rpc/mentor_misconception_api", body)
            if (resp.length() > 0 && resp.has("id")) {
                val serverSession = MisconceptionBusterSession(
                    id = resp.getString("id"),
                    ownerId = ownerId,
                    sessionId = resp.optString("sessionId").takeIf { it.isNotBlank() },
                    questionId = resp.getString("questionId"),
                    subjectId = resp.getString("subjectId"),
                    grade = resp.getInt("grade"),
                    questionPrompt = resp.optString("questionPrompt"),
                    selectedOption = resp.optString("selectedOption").takeIf { it.isNotBlank() },
                    misconceptionReason = resp.getString("misconceptionReason"),
                    reasonType = MisconceptionReasonType.valueOf(resp.optString("reasonType", MisconceptionReasonType.PRE_AUTHORED.name)),
                    aiHint = resp.optString("aiHint"),
                    aiCounterexample = resp.optString("aiCounterexample"),
                    prop1Prompt = resp.getString("prop1Prompt"),
                    prop1Correct = local?.prop1Correct ?: true,
                    prop1UserAns = if (resp.has("prop1UserAns") && !resp.isNull("prop1UserAns")) resp.getBoolean("prop1UserAns") else local?.prop1UserAns,
                    prop2Prompt = resp.getString("prop2Prompt"),
                    prop2Correct = local?.prop2Correct ?: true,
                    prop2UserAns = if (resp.has("prop2UserAns") && !resp.isNull("prop2UserAns")) resp.getBoolean("prop2UserAns") else local?.prop2UserAns,
                    verificationStatus = MisconceptionVerificationStatus.valueOf(resp.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)),
                    isDraft = false
                )
                saveLocalSession(serverSession)
                return@withContext serverSession
            }
        } catch (_: Exception) {}

        local
    }

    override suspend fun getActiveDraft(sessionId: String?, questionId: String): MisconceptionBusterSession? =
        withContext(Dispatchers.IO) {
            parseLocalSession(prefs.getString(draftKey(sessionId, questionId), null))
        }

    override suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession =
        withContext(Dispatchers.IO) {
            if (!session.isDraft) return@withContext session
            // Try start first
            val started = startSession(
                id = session.id,
                sessionId = session.sessionId,
                questionId = session.questionId,
                subjectId = session.subjectId,
                grade = session.grade,
                questionPrompt = session.questionPrompt,
                selectedOption = session.selectedOption,
                misconceptionReason = session.misconceptionReason,
                reasonType = session.reasonType,
                aiHint = session.aiHint,
                aiCounterexample = session.aiCounterexample,
                prop1Prompt = session.prop1Prompt,
                prop1Correct = session.prop1Correct,
                prop2Prompt = session.prop2Prompt,
                prop2Correct = session.prop2Correct
            )
            if (session.prop1UserAns != null && session.prop2UserAns != null) {
                verifySession(session.id, session.prop1UserAns!!, session.prop2UserAns!!)
            } else started
        }
}
