package com.example.asm_final.supabase

import android.content.Context
import com.example.mentor.learning.SubjectId
import org.json.JSONObject
import org.json.JSONArray

object SupabaseEnrollmentSync {
 @JvmStatic fun selected(context:Context,owner:Int,grade:Int):Set<SubjectId> {
  val rows=JSONObject(SupabaseAccounts(context).requestFor(owner,"POST","/rest/v1/rpc/mentor_selected_subjects",JSONObject().put("g",grade))).getJSONArray("items")
  return (0 until rows.length()).map { SubjectId.fromWireId(rows.getString(it)) }.toSet()
 }
 @JvmStatic fun save(context:Context,owner:Int,grade:Int,subjects:Set<SubjectId>) {
  SupabaseAccounts(context).requestFor(owner,"POST","/rest/v1/rpc/mentor_select_subjects",JSONObject().put("g",grade).put("subjects",JSONArray(subjects.map { it.wireId })))
 }
}
