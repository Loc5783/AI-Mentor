package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.BuildConfig
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
        sessionId: String,
        questionId: String,
        reasonId: String
    ): MisconceptionBusterSession

    suspend fun verifySession(
        id: String,
        prop1Ans: Boolean,
        prop2Ans: Boolean
    ): MisconceptionBusterSession

    suspend fun getSession(id: String): MisconceptionBusterSession?
    suspend fun getActiveDraft(sessionId: String, questionId: String): MisconceptionBusterSession?
    suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession
}

class DefaultMisconceptionRepository(
    context: Context,
    override val ownerId: Int
) : MisconceptionRepository {

    private val app = context.applicationContext
    private val accounts = SupabaseAccounts(app)
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs = app.getSharedPreferences("misconception_drafts", Context.MODE_PRIVATE)

    private fun draftKey(sessionId: String, questionId: String): String =
        "draft_${ownerId}_${sessionId}_$questionId"

    private fun sessionKey(id: String): String =
        "session_${ownerId}_$id"

    private fun request(body: JSONObject): JSONObject =
        accounts.authorized(ownerId) { token ->
            val builder = Request.Builder().url(BuildConfig.SUPABASE_URL + "/rest/v1/rpc/mentor_misconception_api")
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
            .put("sessionId", s.sessionId)
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
                sessionId = obj.getString("sessionId"),
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
        sessionId: String,
        questionId: String,
        reasonId: String
    ): MisconceptionBusterSession = withContext(Dispatchers.IO) {
        val catalogItem = MisconceptionBuster.MATH_CATALOG.find { it.id == reasonId }
            ?: throw BackendClient.Failure("UNVERIFIED_REASON", 400)

        val draft = MisconceptionBusterSession(
            id = id,
            ownerId = ownerId,
            sessionId = sessionId,
            questionId = questionId,
            subjectId = "math",
            grade = 11,
            questionPrompt = "",
            selectedOption = null,
            misconceptionReason = catalogItem.text,
            reasonType = MisconceptionReasonType.PRE_AUTHORED,
            aiHint = catalogItem.verifiedHint,
            aiCounterexample = catalogItem.counterexample,
            prop1Prompt = catalogItem.prop1Prompt,
            prop1Correct = catalogItem.prop1Correct,
            prop2Prompt = catalogItem.prop2Prompt,
            prop2Correct = catalogItem.prop2Correct,
            verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW,
            isDraft = true
        )
        saveLocalSession(draft)

        try {
            val body = JSONObject()
                .put("p_action", "start")
                .put("p_id", id)
                .put("p_session_id", UUID.fromString(sessionId).toString())
                .put("p_question_id", questionId)
                .put("p_reason_id", reasonId)

            val resp = request(body)
            val serverStatus = MisconceptionVerificationStatus.valueOf(
                resp.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)
            )
            val synced = draft.copy(
                questionPrompt = resp.optString("questionPrompt", draft.questionPrompt),
                selectedOption = resp.optString("selectedOption").takeIf { it.isNotBlank() },
                verificationStatus = serverStatus,
                isDraft = false
            )
            saveLocalSession(synced)
            synced
        } catch (_: Exception) {
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

        try {
            val body = JSONObject()
                .put("p_action", "verify")
                .put("p_id", id)
                .put("p_prop1_ans", prop1Ans)
                .put("p_prop2_ans", prop2Ans)

            val resp = request(body)
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
        } catch (_: Exception) {
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

        try {
            val body = JSONObject()
                .put("p_action", "get")
                .put("p_id", id)
            val resp = request(body)
            if (resp.length() > 0 && resp.has("id")) {
                val serverStatus = MisconceptionVerificationStatus.valueOf(
                    resp.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)
                )
                val serverSession = MisconceptionBusterSession(
                    id = resp.getString("id"),
                    ownerId = ownerId,
                    sessionId = resp.getString("sessionId"),
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
                    prop1Correct = local?.prop1Correct ?: false,
                    prop1UserAns = if (resp.has("prop1UserAns") && !resp.isNull("prop1UserAns")) resp.getBoolean("prop1UserAns") else local?.prop1UserAns,
                    prop2Prompt = resp.getString("prop2Prompt"),
                    prop2Correct = local?.prop2Correct ?: false,
                    prop2UserAns = if (resp.has("prop2UserAns") && !resp.isNull("prop2UserAns")) resp.getBoolean("prop2UserAns") else local?.prop2UserAns,
                    verificationStatus = serverStatus,
                    isDraft = false
                )
                saveLocalSession(serverSession)
                return@withContext serverSession
            }
        } catch (_: Exception) {}

        local
    }

    override suspend fun getActiveDraft(sessionId: String, questionId: String): MisconceptionBusterSession? =
        withContext(Dispatchers.IO) {
            parseLocalSession(prefs.getString(draftKey(sessionId, questionId), null))
        }

    override suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession =
        withContext(Dispatchers.IO) {
            // Priority 3: Check server state first. Do NOT overwrite already RESOLVED state!
            val serverVersion = runCatching { getSession(session.id) }.getOrNull()
            if (serverVersion != null && serverVersion.verificationStatus == MisconceptionVerificationStatus.RESOLVED) {
                saveLocalSession(serverVersion)
                return@withContext serverVersion
            }

            if (!session.isDraft) return@withContext session

            val catalogItem = MisconceptionBuster.MATH_CATALOG.find { it.text == session.misconceptionReason }
            val reasonId = catalogItem?.id ?: "math_reason_01"

            val started = startSession(
                id = session.id,
                sessionId = session.sessionId,
                questionId = session.questionId,
                reasonId = reasonId
            )
            if (session.prop1UserAns != null && session.prop2UserAns != null) {
                verifySession(session.id, session.prop1UserAns!!, session.prop2UserAns!!)
            } else started
        }
}
