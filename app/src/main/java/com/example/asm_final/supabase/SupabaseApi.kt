package com.example.asm_final.supabase

import android.content.Context
import android.net.Uri
import com.example.asm_final.backend.BackendClient
import org.json.JSONObject

/** Compatibility boundary lets existing lesson state/UI keep its established wire contract. */
class SupabaseApi(private val context: Context,private val owner:Int) {
    fun request(method:String,url:String,body:JSONObject?,key:String?,revision:Int?):JSONObject {
        val uri=Uri.parse(url)
        val path=uri.path ?: throw IllegalArgumentException("Invalid path")
        val query=JSONObject(); uri.queryParameterNames.forEach { query.put(it,uri.getQueryParameter(it)) }
        val api=SupabaseAccounts(context)
        val result=when {
            path.startsWith("/library/") || path.startsWith("/me/wrong-questions") ->
                api.requestFor(owner,"POST","/rest/v1/rpc/mentor_library_api",JSONObject().put("method",method).put("path",path).put("body",body ?: JSONObject()).put("query",query))
            path=="/me/dashboard" -> api.requestFor(owner,"POST","/rest/v1/rpc/mentor_overview",JSONObject().put("g",query.getInt("grade")))
            path.startsWith("/profiles/") || path.startsWith("/rankings") || path=="/friendships" || path.startsWith("/friendships/") || path in listOf("/me/public-profile","/me/account") ->
                api.requestFor(owner,"POST","/rest/v1/rpc/mentor_social_api",JSONObject().put("method",method).put("path",path).put("body",body ?: JSONObject()).put("query",query))
            else -> api.requestFor(owner,"POST","/rest/v1/rpc/mentor_learning_api",JSONObject().put("method",method).put("path",path).put("body",body ?: JSONObject()).put("request_key",key ?: JSONObject.NULL).put("expected_revision",revision ?: JSONObject.NULL).put("query",query))
        }
        return JSONObject(result)
    }
}
