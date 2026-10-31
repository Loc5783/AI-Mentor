package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.chat.ChatRecords
import com.example.asm_final.chat.LocalChatFiles
import com.example.asm_final.backend.BackendClient
import java.net.HttpURLConnection
import java.net.URL

class SupabaseStorage(private val context:Context) {
 private fun userId(owner:Int)=RemoteAccountLinks(context).use { it.remoteId(owner,BuildConfig.SUPABASE_URL) }
 private fun avatarPath(owner:Int)="${userId(owner)}/profile/avatar"
 private fun objectPath(owner:Int,file:ChatRecords.FileItem):String {
  val uid=RemoteAccountLinks(context).use { it.remoteId(owner,BuildConfig.SUPABASE_URL) }
  require(uid.matches(Regex("[0-9a-fA-F-]{36}")) && file.id.matches(Regex("[0-9a-fA-F-]{36}")))
  return "/storage/v1/object/mentor-chat-files/$uid/${file.id}"
 }
 private fun transfer(method:String,path:String,token:String,bytes:ByteArray?=null,mime:String="application/octet-stream",limit:Int=8388608):ByteArray {
  val c=URL(BuildConfig.SUPABASE_URL+path).openConnection() as HttpURLConnection
  try {
   c.requestMethod=method;c.connectTimeout=12000;c.readTimeout=30000;c.instanceFollowRedirects=false
   c.setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY);c.setRequestProperty("Authorization","Bearer $token")
   if(bytes!=null) {require(bytes.size<=limit);c.doOutput=true;c.setRequestProperty("Content-Type",mime);c.setRequestProperty("x-upsert","true");c.setFixedLengthStreamingMode(bytes.size);c.outputStream.use {it.write(bytes)}}
   val code=c.responseCode
   if(code !in 200..299) throw BackendClient.Failure("STORAGE_UNAVAILABLE",code)
   return c.inputStream.use { source -> val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192);while(true) {val n=source.read(buf);if(n<0)break;require(out.size()+n<=limit);out.write(buf,0,n)};out.toByteArray() }
  } finally { c.disconnect() }
 }
 fun upload(owner:Int,file:ChatRecords.FileItem) {
  val path=LocalChatFiles(context).resolve(owner,file)
  require(path.length()==file.size.toLong())
  SupabaseAccounts(context).authorized(owner) { transfer("POST",objectPath(owner,file),it,path.readBytes(),file.mime) }
 }
 fun ensureCached(owner:Int,file:ChatRecords.FileItem) {
  val path=LocalChatFiles(context).resolve(owner,file)
  if(path.isFile && path.length()==file.size.toLong()) return
  val bytes=SupabaseAccounts(context).authorized(owner) { transfer("GET",objectPath(owner,file),it) }
  require(bytes.size==file.size)
  path.parentFile?.mkdirs()
  val temp=java.io.File(path.parentFile,path.name+".download")
  try {temp.writeBytes(bytes);check(temp.renameTo(path))} finally {temp.delete()}
 }
 fun uploadAvatar(owner:Int,uri:android.net.Uri):String {
  val uid=userId(owner);require(uid.matches(Regex("[0-9a-fA-F-]{36}")))
  val mime=context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
  val bytes=context.contentResolver.openInputStream(uri)?.use { input ->
   val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true) {val count=input.read(buffer);if(count<0)break;require(out.size()+count<=2097152);out.write(buffer,0,count)}
   out.toByteArray()
  } ?: error("Avatar could not be read")
  SupabaseAccounts(context).authorized(owner) { transfer("POST","/storage/v1/object/mentor-avatars/$uid/profile/avatar",it,bytes,mime,2097152) }
  return avatarPath(owner)
 }
 fun cacheAvatar(owner:Int,path:String):String? {
  val uid=userId(owner);if(path!="$uid/profile/avatar")return null
  val target=java.io.File(context.cacheDir,"supabase-avatar-$uid")
  if(!target.isFile) {
   val bytes=SupabaseAccounts(context).authorized(owner) { transfer("GET","/storage/v1/object/mentor-avatars/$path",it,limit=2097152) }
   val temp=java.io.File(target.parentFile,target.name+".tmp")
   try {temp.writeBytes(bytes);check(temp.renameTo(target))} finally {temp.delete()}
  }
  return android.net.Uri.fromFile(target).toString()
 }

 fun loadPublicAvatar(viewerOwner:Int,profileId:String,path:String):ByteArray {
  require(profileId.matches(Regex("[0-9a-fA-F-]{36}")) && path == "$profileId/profile/avatar")
  return SupabaseAccounts(context).authorized(viewerOwner) {
   transfer("GET","/storage/v1/object/mentor-avatars/$path",it,limit=2097152)
  }
 }
}
