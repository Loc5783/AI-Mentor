package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.chat.ChatRecords
import com.example.asm_final.chat.LocalChatFiles
import com.example.asm_final.backend.BackendClient
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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
   if(code==204) return ByteArray(0)
   return c.inputStream.use { source -> val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192);while(true) {val n=source.read(buf);if(n<0)break;require(out.size()+n<=limit);out.write(buf,0,n)};out.toByteArray() }
  } finally { c.disconnect() }
 }
 fun upload(owner:Int,file:ChatRecords.FileItem) {
  val path=LocalChatFiles(context).resolve(owner,file)
  require(path.length()==file.size.toLong())
  SupabaseAccounts(context).authorized(owner) { transfer("POST",objectPath(owner,file),it,path.readBytes(),file.mime) }
 }
 fun removeDraft(owner:Int,file:ChatRecords.FileItem) {
  try {
   SupabaseAccounts(context).authorized(owner) {
    transfer("DELETE",objectPath(owner,file),it,limit=1024)
   }
  } finally {
   LocalChatFiles(context).delete(owner,file)
  }
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
  var mime=context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
  var bytes=context.contentResolver.openInputStream(uri)?.use { input ->
   val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true) {val count=input.read(buffer);if(count<0)break;require(out.size()+count<=16777216);out.write(buffer,0,count)}
   out.toByteArray()
  } ?: error("Avatar could not be read")
  if(bytes.size>2097152) {
   bytes=compressAvatar(bytes)
   mime="image/jpeg"
  }
  SupabaseAccounts(context).authorized(owner) { transfer("POST","/storage/v1/object/mentor-avatars/$uid/profile/avatar",it,bytes,mime,2097152) }
  cacheAvatarBytes(owner, bytes)
  return avatarPath(owner)
 }

 private fun compressAvatar(source:ByteArray):ByteArray {
  val bounds=android.graphics.BitmapFactory.Options().apply {inJustDecodeBounds=true}
  android.graphics.BitmapFactory.decodeByteArray(source,0,source.size,bounds)
  require(bounds.outWidth>0 && bounds.outHeight>0) {"Invalid avatar image"}
  var sample=1
  while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1600) sample*=2
  val options=android.graphics.BitmapFactory.Options().apply {inSampleSize=sample}
  val original=android.graphics.BitmapFactory.decodeByteArray(source,0,source.size,options)
      ?: error("Avatar image could not be decoded")
  var bitmap=original
  try {
   val orientation=runCatching {
    android.media.ExifInterface(java.io.ByteArrayInputStream(source))
        .getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,android.media.ExifInterface.ORIENTATION_NORMAL)
   }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)
   val rotation=when(orientation) {
    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
   }
   if(rotation!=0f) {
    bitmap=android.graphics.Bitmap.createBitmap(original,0,0,original.width,original.height,
        android.graphics.Matrix().apply {postRotate(rotation)},true)
   }
   for(quality in intArrayOf(85,70,55)) {
    val output=java.io.ByteArrayOutputStream()
    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,quality,output))
    if(output.size()<=2097152) return output.toByteArray()
   }
   error("Avatar image is too large")
  } finally {
   if(bitmap!==original) bitmap.recycle()
   original.recycle()
  }
 }
 fun cacheAvatar(owner:Int,path:String):String? {
  val uid=userId(owner);if(path!="$uid/profile/avatar")return null
  val name=context.getSharedPreferences("supabase_avatar_cache",0).getString(uid,null)
  val cached=name?.takeIf { it.startsWith("supabase-avatar-$uid-") }
      ?.let { java.io.File(context.cacheDir,it) }
  if(cached?.isFile==true) return android.net.Uri.fromFile(cached).toString()
  val bytes=SupabaseAccounts(context).authorized(owner) { transfer("GET","/storage/v1/object/mentor-avatars/$path",it,limit=2097152) }
  return cacheAvatarBytes(owner,bytes)
 }

 private fun cacheAvatarBytes(owner:Int,bytes:ByteArray):String {
  val uid=userId(owner)
  val digest=MessageDigest.getInstance("SHA-256").digest(bytes)
      .take(12).joinToString("") { "%02x".format(it.toInt() and 0xff) }
  val target=java.io.File(context.cacheDir,"supabase-avatar-$uid-$digest")
  if(!target.isFile) {
   val file=android.util.AtomicFile(target)
   val stream=file.startWrite()
   try {stream.write(bytes);file.finishWrite(stream)}
   catch(error:Exception) {file.failWrite(stream);throw error}
  }
  context.getSharedPreferences("supabase_avatar_cache",0).edit().putString(uid,target.name).apply()
  return android.net.Uri.fromFile(target).toString()
 }

 fun loadPublicAvatar(viewerOwner:Int,profileId:String,path:String):ByteArray {
  require(profileId.matches(Regex("[0-9a-fA-F-]{36}")) && path == "$profileId/profile/avatar")
  return SupabaseAccounts(context).authorized(viewerOwner) {
   transfer("GET","/storage/v1/object/mentor-avatars/$path",it,limit=2097152)
  }
 }
}
