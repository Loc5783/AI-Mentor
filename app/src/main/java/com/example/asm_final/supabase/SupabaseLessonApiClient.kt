package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.learning.LessonViewModel
import org.json.JSONObject

/** Adapts the shared lesson contract to the authenticated Supabase learning RPC. */
class SupabaseLessonApiClient(
    private val context: Context,
    private val owner: Int
) : LessonViewModel.LessonApiClient {
    override fun configured() = SupabaseHttp.enabled(context) && BuildConfig.SUPABASE_URL.isNotBlank()

    override fun signedIn() = SupabaseAccounts(context).hasSession(owner)

    override fun request(
        method: String,
        path: String,
        body: JSONObject?,
        key: String?,
        revision: Int?
    ): JSONObject {
        require(path.startsWith("/") && !path.startsWith("//") && !path.contains(".."))
        val payload = JSONObject()
            .put("method", method)
            .put("path", path)
            .put("body", body ?: JSONObject())
            .put("request_key", key ?: JSONObject.NULL)
            .put("expected_revision", revision ?: JSONObject.NULL)
            .put("query", JSONObject())
        return JSONObject(
            SupabaseAccounts(context).requestFor(
                owner,
                "POST",
                "/rest/v1/rpc/mentor_learning_api",
                payload
            )
        )
    }

    override fun signIn(identifier: String, password: String) {
        try {
            SupabaseAccounts(context).signIn(identifier, password)
        } catch (failure: BackendClient.Failure) {
            throw failure
        }
    }
}
