package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.onboarding.*
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.BuildConfig
import org.json.JSONObject
import org.json.JSONArray

/** Local reads keep startup fast; setup writes must be accepted online before marking complete. */
class SupabaseLearnerProfiles(private val context:Context):LearnerProfileRepository {
 private val local=LocalLearnerProfiles(context)
 override fun load(userId:Int)=local.load(userId)
 private fun save(owner:Int,body:JSONObject) {
  try { SupabaseAccounts(context).requestFor(owner,"POST","/rest/v1/rpc/mentor_setup",JSONObject().put("data",body)) }
  catch(e:Exception) { throw IllegalStateException("Could not save learner setup",e) }
 }
 private fun body(p:LearnerProfile)=JSONObject().put("nickname",p.displayName).put("grade",p.grade).put("subjects",JSONArray(p.subjects.split(',').filter {it.isNotBlank()}.map { it.lowercase(java.util.Locale.ROOT) })).put("goal",p.goal).put("minutes",p.minutes)
 override fun advance(userId:Int,step:Int) {save(userId,JSONObject().put("step",step));local.advance(userId,step)}
 override fun saveDraft(profile:LearnerProfile) { if(profile.grade in 10..12) save(profile.userId,body(profile));local.saveDraft(profile) }
 override fun complete(profile:LearnerProfile) {require(OnboardingPolicy.valid(profile));save(profile.userId,body(profile).put("step",5));local.complete(profile)}
 override fun setGrade(userId:Int,grade:Int) {require(grade in 10..12);save(userId,JSONObject().put("grade",grade));local.setGrade(userId,grade)}
}
