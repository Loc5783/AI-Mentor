package com.example.asm_final.supabase

import android.content.Context
import org.json.JSONObject

/** Cloud API for bounded, idempotent study-time events and fact-derived badge state. */
class SupabaseStudyRepository(context: Context) {
    private val appContext = context.applicationContext
    private fun rpc(owner: Int, name: String, body: JSONObject = JSONObject()): JSONObject =
        JSONObject(SupabaseAccounts(appContext).requestFor(
            owner, "POST", "/rest/v1/rpc/$name", body
        ))

    fun start(owner: Int, sessionId: String, subjectId: String, grade: Int): JSONObject =
        rpc(owner, "mentor_study_action", JSONObject()
            .put("op", "start")
            .put("data", JSONObject()
                .put("sessionId", sessionId)
                .put("subjectId", subjectId)
                .put("grade", grade)))

    fun heartbeat(owner: Int, sessionId: String, eventKey: String): JSONObject =
        action(owner, "heartbeat", sessionId, eventKey)

    fun stop(owner: Int, sessionId: String, eventKey: String): JSONObject =
        action(owner, "stop", sessionId, eventKey)

    private fun action(owner: Int, op: String, sessionId: String, eventKey: String): JSONObject {
        require(sessionId.matches(Regex("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")))
        require(eventKey.matches(Regex("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")))
        return rpc(owner, "mentor_study_action", JSONObject()
            .put("op", op)
            .put("data", JSONObject().put("sessionId", sessionId).put("eventKey", eventKey)))
    }

    fun summary(owner: Int): JSONObject = rpc(owner, "mentor_study_summary")

    fun badges(owner: Int): JSONObject = rpc(owner, "mentor_badges")
}
