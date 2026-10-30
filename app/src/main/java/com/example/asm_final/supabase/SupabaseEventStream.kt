package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.EventStream
import com.example.asm_final.accounts.RemoteAccountLinks
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Account scoped Supabase Postgres change subscription. It only emits cache invalidations. */
class SupabaseEventStream(context:Context,private val owner:Int):EventStream {
 private val context=context.applicationContext
 @Volatile private var stopped=false
 @Volatile private var socket:WebSocket?=null
 @Volatile private var activeLatch:CountDownLatch?=null
 override fun run(listener:EventStream.Listener) {
  stopped=false
  val uid=RemoteAccountLinks(context).use { it.remoteId(owner,BuildConfig.SUPABASE_URL) }
  require(uid.matches(Regex("[0-9a-fA-F-]{36}")))
  SupabaseAccounts(context).authorized(owner) { token ->
   val project=BuildConfig.SUPABASE_URL.removePrefix("https://")
   val url="wss://$project/realtime/v1/websocket?apikey=${enc(BuildConfig.SUPABASE_PUBLISHABLE_KEY)}&vsn=1.0.0"
   val ended=CountDownLatch(1);activeLatch=ended;var failure:Throwable?=null
   val client=OkHttpClient.Builder().pingInterval(30,TimeUnit.SECONDS).build()
   val request=Request.Builder().url(url).build()
   val ws=client.newWebSocket(request,object:WebSocketListener() {
    override fun onOpen(webSocket:WebSocket,response:Response) {
     socket=webSocket
     val filters=org.json.JSONArray()
      .put(filter("mentor_awards","user_id=eq.$uid"))
      .put(filter("mentor_learning_days","user_id=eq.$uid"))
      .put(filter("mentor_study_sessions","user_id=eq.$uid"))
      .put(filter("mentor_study_events","user_id=eq.$uid"))
      .put(filter("mentor_badge_awards","user_id=eq.$uid"))
      .put(filter("mentor_practice_sessions","user_id=eq.$uid"))
      .put(filter("mentor_wrong_questions","user_id=eq.$uid"))
      .put(filter("mentor_profiles","id=eq.$uid"))
      .put(filter("mentor_preferences","user_id=eq.$uid"))
      .put(filter("mentor_enrollments","user_id=eq.$uid"))
      .put(filter("mentor_attempts","user_id=eq.$uid"))
      .put(filter("mentor_friendships","requester=eq.$uid"))
      .put(filter("mentor_friendships","recipient=eq.$uid"))
      .put(filter("mentor_chat_threads","user_id=eq.$uid"))
      .put(filter("mentor_chat_messages","user_id=eq.$uid"))
      .put(filter("mentor_timetable","user_id=eq.$uid"))
      .put(filter("mentor_todos","user_id=eq.$uid"))
      .put(filter("mentor_schedule_logs","user_id=eq.$uid"))
     val config=JSONObject().put("broadcast",JSONObject().put("ack",false).put("self",false))
      .put("presence",JSONObject().put("key","" )).put("postgres_changes",filters)
     val payload=JSONObject().put("access_token",token).put("config",config)
     webSocket.send(JSONObject().put("topic","realtime:public:mentor_live_$uid").put("event","phx_join").put("payload",payload).put("ref","1").put("join_ref","1").toString())
    }
    override fun onMessage(webSocket:WebSocket,text:String) {
     try {
      val msg=JSONObject(text);when(msg.optString("event")) {
       "phx_reply" -> { if(msg.optJSONObject("payload")?.optString("status")=="ok") listener.ready() else {failure=IllegalStateException("Realtime join failed");ended.countDown()} }
       "postgres_changes" -> {
        val data=msg.optJSONObject("payload")?.optJSONObject("data") ?: return
        val table=data.optString("table");val record=data.optJSONObject("record") ?: data.optJSONObject("old_record") ?: JSONObject()
        val type=when(table) {
         "mentor_awards","mentor_learning_days","mentor_attempts"->"COMPLETION"
         "mentor_study_sessions","mentor_study_events"->"LEARNING_PROFILE"
         "mentor_badge_awards"->"PROFILE"
         "mentor_practice_sessions","mentor_wrong_questions"->"COMPLETION"
         "mentor_enrollments"->"ENROLLMENT"
         "mentor_profiles"->"PROFILE"
         "mentor_preferences"->"LEARNING_PROFILE"
         "mentor_friendships"->"FRIENDSHIP"
         "mentor_chat_threads","mentor_chat_messages"->"CHAT"
         "mentor_timetable","mentor_todos","mentor_schedule_logs"->"SCHEDULE"
         else->"OTHER"
        }
        val id=when(table) {
         "mentor_profiles"->record.optString("id")
         "mentor_chat_messages"->record.optString("thread_id")
         "mentor_badge_awards"->record.optString("user_id",uid)
         else->record.optString("user_id",uid)
        }
        listener.changed(type,id)
       }
       "phx_error","phx_close" -> {failure=IllegalStateException("Realtime channel closed");ended.countDown()}
      }
     } catch(e:Exception) { failure=e;ended.countDown() }
    }
    override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?) {failure=t;ended.countDown()}
   })
   socket=ws
   try {if(!ended.await(90,TimeUnit.SECONDS) && !stopped) throw java.io.IOException("Realtime heartbeat timed out");failure?.let {throw java.io.IOException("Realtime disconnected",it)}}
   finally {activeLatch=null;socket=null;ws.close(1000,"scope ended");client.dispatcher.executorService.shutdown()}
  }
 }
 private fun filter(table:String,where:String)=JSONObject().put("event","*").put("schema","public").put("table",table).put("filter",where)
 private fun enc(value:String)=URLEncoder.encode(value,"UTF-8")
 override fun close() {stopped=true;socket?.close(1000,"subscription closed");socket=null;activeLatch?.countDown()}
}
