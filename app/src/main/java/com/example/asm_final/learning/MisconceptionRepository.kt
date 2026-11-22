package com.example.asm_final.learning

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.supabase.SupabaseAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

interface MisconceptionRepository {
    val ownerId: Int
    suspend fun getVerifiedReasons(subjectId: String, grade: Int): List<MisconceptionReasonItem>

    suspend fun startSession(
        id: String,
        sessionId: String,
        questionId: String,
        subjectId: String,
        grade: Int,
        questionPrompt: String,
        selectedOption: String?,
        reason: MisconceptionReasonItem
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

    init {
        removeLegacyAnswerKeys()
    }

    private fun draftKey(sessionId: String, questionId: String): String =
        "draft_${ownerId}_${sessionId}_$questionId"

    private fun sessionKey(id: String): String = "session_${ownerId}_$id"

    private fun catalogKey(subjectId: String, grade: Int): String =
        "catalog_${ownerId}_${subjectId}_$grade"

    private fun removeLegacyAnswerKeys() {
        val editor = prefs.edit()
        var changed = false
        prefs.all.forEach { (key, value) ->
            if (value is String && (key.startsWith("session_${ownerId}_") ||
                    key.startsWith("draft_${ownerId}_"))
            ) {
                val json = runCatching { JSONObject(value) }.getOrNull()
                if (json != null && (json.has("prop1Correct") || json.has("prop2Correct"))) {
                    json.remove("prop1Correct")
                    json.remove("prop2Correct")
                    editor.putString(key, json.toString())
                    changed = true
                }
            }
        }
        if (changed) editor.apply()
    }

    private fun request(body: JSONObject): JSONObject =
        accounts.authorized(ownerId) { token ->
            val payload = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.SUPABASE_URL + "/rest/v1/rpc/mentor_misconception_api")
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .post(payload)
                .build()

            http.newCall(request).execute().use { response ->
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

    private fun reasonFromJson(row: JSONObject) = MisconceptionReasonItem(
        reasonId = row.getString("reasonId"),
        subjectId = row.getString("subjectId"),
        grade = row.getInt("grade"),
        reasonText = row.getString("reasonText"),
        verifiedHint = row.getString("verifiedHint"),
        counterexample = row.getString("counterexample"),
        prop1Prompt = row.getString("prop1Prompt"),
        prop2Prompt = row.getString("prop2Prompt")
    )

    private fun reasonJson(reason: MisconceptionReasonItem) = JSONObject()
        .put("reasonId", reason.reasonId)
        .put("subjectId", reason.subjectId)
        .put("grade", reason.grade)
        .put("reasonText", reason.reasonText)
        .put("verifiedHint", reason.verifiedHint)
        .put("counterexample", reason.counterexample)
        .put("prop1Prompt", reason.prop1Prompt)
        .put("prop2Prompt", reason.prop2Prompt)

    private fun parseCachedReasons(subjectId: String, grade: Int): List<MisconceptionReasonItem>? {
        val raw = prefs.getString(catalogKey(subjectId, grade), null) ?: return null
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        return (0 until array.length()).map { reasonFromJson(array.getJSONObject(it)) }
    }

    private fun saveCachedReasons(subjectId: String, grade: Int, reasons: List<MisconceptionReasonItem>) {
        val array = JSONArray()
        reasons.forEach { array.put(reasonJson(it)) }
        prefs.edit().putString(catalogKey(subjectId, grade), array.toString()).apply()
    }

    override suspend fun getVerifiedReasons(subjectId: String, grade: Int): List<MisconceptionReasonItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = request(
                    JSONObject()
                        .put("p_action", "list_reasons")
                        .put("p_subject_id", subjectId)
                        .put("p_grade", grade)
                )
                val items = response.optJSONArray("items")
                    ?: throw IllegalStateException("Missing verified misconception catalog")
                val reasons = (0 until items.length()).map { reasonFromJson(items.getJSONObject(it)) }
                if (reasons.any { it.subjectId != subjectId || it.grade != grade }) {
                    throw IllegalStateException("Mismatched verified misconception catalog")
                }
                saveCachedReasons(subjectId, grade, reasons)
                reasons
            } catch (offline: IOException) {
                currentCoroutineContext().ensureActive()
                parseCachedReasons(subjectId, grade) ?: throw offline
            }
        }

    private fun saveLocalSession(session: MisconceptionBusterSession) {
        val json = JSONObject()
            .put("id", session.id)
            .put("ownerId", session.ownerId)
            .put("sessionId", session.sessionId)
            .put("questionId", session.questionId)
            .put("reasonId", session.reasonId)
            .put("subjectId", session.subjectId)
            .put("grade", session.grade)
            .put("questionPrompt", session.questionPrompt)
            .put("selectedOption", session.selectedOption ?: "")
            .put("misconceptionReason", session.misconceptionReason)
            .put("reasonType", session.reasonType.name)
            .put("aiHint", session.aiHint)
            .put("aiCounterexample", session.aiCounterexample)
            .put("prop1Prompt", session.prop1Prompt)
            .put("prop1UserAns", session.prop1UserAns?.toString() ?: "")
            .put("prop2Prompt", session.prop2Prompt)
            .put("prop2UserAns", session.prop2UserAns?.toString() ?: "")
            .put("verificationStatus", session.verificationStatus.name)
            .put("answeredAt", session.answeredAt)
            .put("serverUpdatedAt", session.serverUpdatedAt ?: JSONObject.NULL)
            .put("isDraft", session.isDraft)

        prefs.edit()
            .putString(sessionKey(session.id), json.toString())
            .putString(draftKey(session.sessionId, session.questionId), json.toString())
            .apply()
    }

    private fun parseLocalSession(raw: String?): MisconceptionBusterSession? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            if (obj.optInt("ownerId", -1) != ownerId) return null
            val isDraft = obj.optBoolean("isDraft", false)
            MisconceptionBusterSession(
                id = obj.getString("id"),
                ownerId = obj.getInt("ownerId"),
                sessionId = obj.getString("sessionId"),
                questionId = obj.getString("questionId"),
                reasonId = obj.optString("reasonId"),
                subjectId = obj.getString("subjectId"),
                grade = obj.getInt("grade"),
                questionPrompt = obj.getString("questionPrompt"),
                selectedOption = obj.optString("selectedOption").takeIf { it.isNotBlank() },
                misconceptionReason = obj.getString("misconceptionReason"),
                reasonType = MisconceptionReasonType.valueOf(obj.getString("reasonType")),
                aiHint = obj.getString("aiHint"),
                aiCounterexample = obj.getString("aiCounterexample"),
                prop1Prompt = obj.getString("prop1Prompt"),
                prop1UserAns = obj.optString("prop1UserAns").takeIf { it.isNotBlank() }?.toBooleanStrictOrNull(),
                prop2Prompt = obj.getString("prop2Prompt"),
                prop2UserAns = obj.optString("prop2UserAns").takeIf { it.isNotBlank() }?.toBooleanStrictOrNull(),
                verificationStatus = if (isDraft) MisconceptionVerificationStatus.NEEDS_REVIEW
                else MisconceptionVerificationStatus.valueOf(obj.getString("verificationStatus")),
                answeredAt = obj.optLong("answeredAt", System.currentTimeMillis()),
                serverUpdatedAt = obj.optLong("serverUpdatedAt").takeIf { it > 0L },
                isDraft = isDraft
            )
        }.getOrNull()
    }

    private fun parseServerSession(response: JSONObject): MisconceptionBusterSession {
        val updatedAt = response.optLong("updatedAtEpochMs").takeIf { it > 0L }
        return MisconceptionBusterSession(
            id = response.getString("id"),
            ownerId = ownerId,
            sessionId = response.getString("sessionId"),
            questionId = response.getString("questionId"),
            reasonId = response.optString("reasonId"),
            subjectId = response.getString("subjectId"),
            grade = response.getInt("grade"),
            questionPrompt = response.optString("questionPrompt"),
            selectedOption = response.optString("selectedOption").takeIf { it.isNotBlank() },
            misconceptionReason = response.getString("misconceptionReason"),
            reasonType = MisconceptionReasonType.valueOf(
                response.optString("reasonType", MisconceptionReasonType.PRE_AUTHORED.name)
            ),
            aiHint = response.optString("aiHint"),
            aiCounterexample = response.optString("aiCounterexample"),
            prop1Prompt = response.getString("prop1Prompt"),
            prop1UserAns = response.optBooleanValue("prop1UserAns"),
            prop2Prompt = response.getString("prop2Prompt"),
            prop2UserAns = response.optBooleanValue("prop2UserAns"),
            verificationStatus = MisconceptionVerificationStatus.valueOf(
                response.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)
            ),
            answeredAt = updatedAt ?: System.currentTimeMillis(),
            serverUpdatedAt = updatedAt,
            isDraft = false
        )
    }

    private fun JSONObject.optBooleanValue(name: String): Boolean? =
        if (has(name) && !isNull(name)) getBoolean(name) else null

    private suspend fun getServerSession(id: String): MisconceptionBusterSession? {
        val response = request(JSONObject().put("p_action", "get").put("p_id", id))
        return if (response.has("id") && !response.isNull("id")) parseServerSession(response) else null
    }

    override suspend fun startSession(
        id: String,
        sessionId: String,
        questionId: String,
        subjectId: String,
        grade: Int,
        questionPrompt: String,
        selectedOption: String?,
        reason: MisconceptionReasonItem
    ): MisconceptionBusterSession = withContext(Dispatchers.IO) {
        if (reason.subjectId != subjectId || reason.grade != grade) {
            throw BackendClient.Failure("REASON_SUBJECT_OR_GRADE_MISMATCH", 400)
        }
        val canonicalSessionId = UUID.fromString(sessionId).toString()
        val draft = MisconceptionBusterSession(
            id = id,
            ownerId = ownerId,
            sessionId = canonicalSessionId,
            questionId = questionId,
            reasonId = reason.reasonId,
            subjectId = subjectId,
            grade = grade,
            questionPrompt = questionPrompt,
            selectedOption = selectedOption,
            misconceptionReason = reason.reasonText,
            reasonType = MisconceptionReasonType.PRE_AUTHORED,
            aiHint = reason.verifiedHint,
            aiCounterexample = reason.counterexample,
            prop1Prompt = reason.prop1Prompt,
            prop2Prompt = reason.prop2Prompt,
            answeredAt = System.currentTimeMillis(),
            isDraft = true
        )

        try {
            val response = request(
                JSONObject()
                    .put("p_action", "start")
                    .put("p_id", id)
                    .put("p_session_id", canonicalSessionId)
                    .put("p_question_id", questionId)
                    .put("p_reason_id", reason.reasonId)
            )
            val serverSession = parseServerSession(response)
            saveLocalSession(serverSession)
            serverSession
        } catch (offline: IOException) {
            currentCoroutineContext().ensureActive()
            saveLocalSession(draft)
            draft
        }
    }

    override suspend fun verifySession(
        id: String,
        prop1Ans: Boolean,
        prop2Ans: Boolean
    ): MisconceptionBusterSession = withContext(Dispatchers.IO) {
        val local = parseLocalSession(prefs.getString(sessionKey(id), null))
        val current = try {
            getServerSession(id) ?: local
        } catch (offline: IOException) {
            currentCoroutineContext().ensureActive()
            local
        } ?: throw BackendClient.Failure("SESSION_NOT_FOUND", 404)

        if (current.verificationStatus == MisconceptionVerificationStatus.RESOLVED && !current.isDraft) {
            return@withContext current
        }

        val answeredAt = maxOf(System.currentTimeMillis(), current.answeredAt + 1L)
        try {
            val response = request(
                JSONObject()
                    .put("p_action", "verify")
                    .put("p_id", id)
                    .put("p_prop1_ans", prop1Ans)
                    .put("p_prop2_ans", prop2Ans)
            )
            val updated = current.copy(
                prop1UserAns = prop1Ans,
                prop2UserAns = prop2Ans,
                verificationStatus = MisconceptionVerificationStatus.valueOf(
                    response.optString("verificationStatus", MisconceptionVerificationStatus.NEEDS_REVIEW.name)
                ),
                answeredAt = answeredAt,
                serverUpdatedAt = response.optLong("updatedAtEpochMs").takeIf { it > 0L }
                    ?: current.serverUpdatedAt,
                isDraft = false
            )
            saveLocalSession(updated)
            updated
        } catch (offline: IOException) {
            currentCoroutineContext().ensureActive()
            val draft = current.copy(
                prop1UserAns = prop1Ans,
                prop2UserAns = prop2Ans,
                verificationStatus = MisconceptionVerificationStatus.NEEDS_REVIEW,
                answeredAt = answeredAt,
                isDraft = true
            )
            saveLocalSession(draft)
            draft
        }
    }

    override suspend fun getSession(id: String): MisconceptionBusterSession? = withContext(Dispatchers.IO) {
        val local = parseLocalSession(prefs.getString(sessionKey(id), null))
        val server = try {
            getServerSession(id)
        } catch (offline: IOException) {
            currentCoroutineContext().ensureActive()
            return@withContext local
        } ?: return@withContext local

        val localHasAnswers = local?.prop1UserAns != null && local.prop2UserAns != null
        val serverHasAnswers = server.prop1UserAns != null && server.prop2UserAns != null
        val localIsNewer = local?.isDraft == true && localHasAnswers &&
            server.verificationStatus != MisconceptionVerificationStatus.RESOLVED &&
            (!serverHasAnswers || (server.serverUpdatedAt != null && server.serverUpdatedAt < local.answeredAt))

        if (localIsNewer) return@withContext local
        saveLocalSession(server)
        server
    }

    override suspend fun getActiveDraft(sessionId: String, questionId: String): MisconceptionBusterSession? =
        withContext(Dispatchers.IO) {
            parseLocalSession(prefs.getString(draftKey(sessionId, questionId), null))
        }

    override suspend fun syncDraft(session: MisconceptionBusterSession): MisconceptionBusterSession =
        withContext(Dispatchers.IO) {
            if (!session.isDraft) return@withContext session

            val server = try {
                getServerSession(session.id)
            } catch (offline: IOException) {
                currentCoroutineContext().ensureActive()
                return@withContext session
            }
            if (server != null) {
                val serverHasAnswers = server.prop1UserAns != null && server.prop2UserAns != null
                val serverIsNewer = server.verificationStatus == MisconceptionVerificationStatus.RESOLVED ||
                    (server.serverUpdatedAt != null && server.serverUpdatedAt >= session.answeredAt &&
                        (!sessionHasAnswers(session) || serverHasAnswers)) ||
                    (serverHasAnswers && session.serverUpdatedAt == null)
                if (serverIsNewer) {
                    saveLocalSession(server)
                    return@withContext server
                }
            }

            val cachedReason = parseCachedReasons(session.subjectId, session.grade)?.firstOrNull {
                it.reasonId == session.reasonId
            }
            val reason = cachedReason ?: if (session.reasonId.isNotBlank()) {
                MisconceptionReasonItem(
                    reasonId = session.reasonId,
                    subjectId = session.subjectId,
                    grade = session.grade,
                    reasonText = session.misconceptionReason,
                    verifiedHint = session.aiHint,
                    counterexample = session.aiCounterexample,
                    prop1Prompt = session.prop1Prompt,
                    prop2Prompt = session.prop2Prompt
                )
            } else {
                getVerifiedReasons(session.subjectId, session.grade).firstOrNull {
                    it.reasonText == session.misconceptionReason
                }
            } ?: throw BackendClient.Failure("UNVERIFIED_REASON", 400)

            val started = startSession(
                id = session.id,
                sessionId = session.sessionId,
                questionId = session.questionId,
                subjectId = session.subjectId,
                grade = session.grade,
                questionPrompt = session.questionPrompt,
                selectedOption = session.selectedOption,
                reason = reason
            )

            val serverAfterStartIsNewer =
                started.verificationStatus == MisconceptionVerificationStatus.RESOLVED ||
                    (started.serverUpdatedAt != null && started.serverUpdatedAt >= session.answeredAt &&
                        (!sessionHasAnswers(session) || sessionHasAnswers(started)))
            if (serverAfterStartIsNewer) return@withContext started

            val answer1 = session.prop1UserAns
            val answer2 = session.prop2UserAns
            if (answer1 == null && answer2 == null) return@withContext started
            if (answer1 == null || answer2 == null) {
                throw BackendClient.Failure("ANSWERS_REQUIRED", 400)
            }
            verifySession(session.id, answer1, answer2)
        }

    private fun sessionHasAnswers(session: MisconceptionBusterSession): Boolean =
        session.prop1UserAns != null && session.prop2UserAns != null
}
