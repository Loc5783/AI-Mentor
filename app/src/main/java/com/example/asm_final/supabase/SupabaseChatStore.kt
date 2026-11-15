package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.chat.*
import com.example.mentor.chat.*
import com.example.mentor.learning.LearningContext
import org.json.JSONObject
import org.json.JSONArray

/** Every chat/folder/message is stored online under the verified Supabase identity. */
class SupabaseChatStore(private val context:Context):ChatStore {
 private val localFiles=LocalChatFiles(context)
 private val storage=SupabaseStorage(context)
 private val device:String = synchronized(SupabaseChatStore::class.java) {
  val p=context.getSharedPreferences("supabase_device",0)
  p.getString("id",null) ?: java.util.UUID.randomUUID().toString().also { check(p.edit().putString("id",it).commit()) }
 }
 private fun call(owner:Int,op:String,data:JSONObject=JSONObject()):JSONObject = try {
  data.put("device_id",device)
  JSONObject(SupabaseAccounts(context).requestFor(owner,"POST","/rest/v1/rpc/mentor_chat_action",JSONObject().put("op",op).put("data",data)))
 } catch(e:Exception) {throw IllegalStateException("Chat cloud operation failed",e)}
 private fun data(thread:String)=JSONObject().put("threadId",thread)
 private fun nullable(j:JSONObject,k:String)=if(j.isNull(k))null else j.getString(k)
 private fun thread(j:JSONObject)=ChatRecords.Thread(j.getString("id"),j.getString("title"),nullable(j,"folderId"),j.getString("draft"),j.getBoolean("automaticTitle"),j.getLong("updatedAt"))
 private fun files(rows:JSONArray):List<ChatRecords.FileItem> = (0 until rows.length()).map { val r=rows.getJSONObject(it);ChatRecords.FileItem(r.getString("id"),r.getString("name"),r.getString("mime"),r.getString("storageKey"),r.getInt("size")) }
 override fun create(owner:Int,folderId:String?)=thread(call(owner,"create",JSONObject().put("folderId",folderId ?: JSONObject.NULL)))
 override fun thread(owner:Int,id:String)=thread(call(owner,"thread",data(id)))
 override fun threads(owner:Int,query:String?):List<ChatRecords.Thread> {val rows=call(owner,"threads",JSONObject().put("query",query ?: "")).getJSONArray("items");return (0 until rows.length()).map {thread(rows.getJSONObject(it))}}
 override fun folders(owner:Int):List<ChatRecords.Folder> {val rows=call(owner,"folders").getJSONArray("items");return (0 until rows.length()).map {val r=rows.getJSONObject(it);ChatRecords.Folder(r.getString("id"),r.getString("title"))}}
 override fun snapshot(owner:Int,id:String):ChatRecords.Snapshot {
  val r=call(owner,"snapshot",data(id));val rows=r.getJSONArray("messages")
  val messages=(0 until rows.length()).map {val m=rows.getJSONObject(it);ChatRecords.Message(m.getString("id"),m.getString("text"),m.getString("role"),m.getString("status"),m.getString("error"),m.getBoolean("truncated"),files(m.getJSONArray("files")))}
  val drafts=files(r.getJSONArray("draftFiles"))
  return ChatRecords.Snapshot(thread(r.getJSONObject("thread")),messages,drafts)
 }
 override fun createFolder(owner:Int,title:String)=call(owner,"create_folder",JSONObject().put("title",title)).getString("id")
 override fun renameFolder(owner:Int,id:String,title:String) {call(owner,"rename_folder",JSONObject().put("folderId",id).put("title",title))}
 override fun deleteFolder(owner:Int,id:String,deleteChats:Boolean) {call(owner,"delete_folder",JSONObject().put("folderId",id).put("deleteChats",deleteChats))}
 override fun renameThread(owner:Int,id:String,title:String) {call(owner,"rename_thread",data(id).put("title",title))}
 override fun moveThread(owner:Int,id:String,folderId:String?) {call(owner,"move_thread",data(id).put("folderId",folderId ?: JSONObject.NULL))}
 override fun deleteThread(owner:Int,id:String) {call(owner,"delete_thread",data(id))}
 override fun saveDraft(owner:Int,id:String,text:String) {call(owner,"draft",data(id).put("text",text))}
 override fun addDraftFile(owner:Int,threadId:String,file:ChatRecords.FileItem) {
  storage.upload(owner,file)
  try {
   if(Thread.currentThread().isInterrupted) throw java.util.concurrent.CancellationException("Upload cancelled")
   call(owner,"add_file",data(threadId).put("file",JSONObject().put("id",file.id).put("name",file.name).put("mime",file.mime).put("storageKey",file.storageKey).put("size",file.size)))
  } catch(error:Exception) {
   runCatching {storage.removeDraft(owner,file)}
   throw error
  }
 }
 override fun removeDraftFile(owner:Int,threadId:String,fileId:String) {
  val file=snapshot(owner,threadId).draftFiles.firstOrNull {it.id==fileId}
  call(owner,"remove_file",data(threadId).put("fileId",fileId))
  if(file!=null) runCatching {storage.removeDraft(owner,file)}
 }
 override fun stageMessage(owner:Int,threadId:String,text:String)=call(owner,"stage",data(threadId).put("text",text)).getString("id")
 override fun markPending(owner:Int,threadId:String,messageId:String) {call(owner,"retry",data(threadId).put("messageId",messageId))}
 override fun finish(owner:Int,threadId:String,messageId:String,reply:TutorReply?,error:String?):Boolean {
  val input=data(threadId).put("messageId",messageId).put("error",error ?: "")
  if(reply!=null) input.put("reply",reply.text).put("truncated",reply.truncated)
  return call(owner,"finish",input).getBoolean("changed")
 }
 override fun recoverInterrupted(owner:Int) {call(owner,"recover")}
 override fun request(owner:Int,threadId:String,messageId:String,context:LearningContext):TutorRequest {
  require(context.userId==owner)
  val snap=snapshot(owner,threadId)
  val recent=mutableListOf<ChatRecords.Message>()
  snap.messages.forEachIndexed { index,m ->
   if(m.role=="USER") {
    if(m.id==messageId) {
     recent.add(m)
     // Only materialize attachment bytes for turns that will actually be sent.
     val selected=recent.takeLast(21)
     val turns=selected.map { turn ->
      if(turn.role=="USER") TutorTurn(owner,TutorTurn.Role.USER,turn.text,
       turn.files.map { storage.ensureCached(owner,it);localFiles.payload(owner,it) })
      else TutorTurn(owner,TutorTurn.Role.ASSISTANT,turn.text,emptyList())
     }
     return TutorRequest(context,turns,null)
    }
    val next=snap.messages.getOrNull(index+1)
    if(m.status=="SENT" && next?.role=="ASSISTANT") {
     recent.add(m)
     recent.add(next)
     while(recent.size>20) {recent.removeAt(0);recent.removeAt(0)}
    }
   }
  }
  throw IllegalArgumentException("Missing owned question")
 }
}
