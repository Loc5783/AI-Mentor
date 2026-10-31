package com.example.asm_final.supabase

import android.content.Context
import android.util.Base64
import com.example.mentor.chat.*
import com.example.asm_final.backend.BackendClient
import org.json.JSONObject
import org.json.JSONArray

/** Android only sends an authenticated request. Provider keys and system policy live in the Edge Function. */
class SupabaseTutorGateway(private val context:Context):TutorGateway {
 override fun configured()=SupabaseHttp.enabled(context)
 @Throws(TutorFailure::class)
 override fun reply(request:TutorRequest):TutorReply {
  try {
   val turns=JSONArray()
   request.turns.forEach { turn ->
    val files=JSONArray();turn.attachments.forEach { f -> files.put(JSONObject().put("mime",f.mimeType).put("data",Base64.encodeToString(f.bytes(),Base64.NO_WRAP))) }
    turns.put(JSONObject().put("role",if(turn.role==TutorTurn.Role.USER)"user" else "model").put("text",turn.text).put("files",files))
   }
   val result=JSONObject(SupabaseAccounts(context).requestFor(request.context.userId,"POST","/functions/v1/mentor-ai",JSONObject().put("turns",turns)))
   return TutorReply(result.getString("text"),result.optBoolean("truncated"))
  } catch(e:BackendClient.Failure) {
   throw TutorFailure(when(e.code) {
    "NOT_CONFIGURED" -> TutorFailure.Code.NOT_CONFIGURED
    "RATE_LIMITED" -> TutorFailure.Code.RATE_LIMITED
    "INVALID_REQUEST" -> TutorFailure.Code.INVALID_REQUEST
    "EMPTY_RESPONSE" -> TutorFailure.Code.EMPTY_RESPONSE
    else -> if(e.status==401)TutorFailure.Code.UNAUTHORIZED else TutorFailure.Code.PROVIDER
   })
  } catch(e:java.io.IOException) {throw TutorFailure(if(Thread.currentThread().isInterrupted)TutorFailure.Code.CANCELLED else TutorFailure.Code.NETWORK)}
  catch(e:Exception) {throw TutorFailure(TutorFailure.Code.INVALID_REQUEST)}
 }
}
