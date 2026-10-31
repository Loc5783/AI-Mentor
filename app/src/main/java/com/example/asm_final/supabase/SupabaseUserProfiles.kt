package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.*
import org.json.JSONObject
import org.json.JSONArray

/** Profile reads remain offline-capable; authenticated writes go to Supabase first. */
class SupabaseUserProfiles(context: Context): UserProfileRepository {
    private val context=context.applicationContext
    private val local=LocalUserProfileRepository(context)
    private val accounts=LocalAccountRepository(context)
    private fun remoteId(owner:Int)=RemoteAccountLinks(context).use { it.remoteId(owner,BuildConfig.SUPABASE_URL) }
    override fun load(username:String):UserProfile? {
        val cached=local.load(username) ?: return null
        val owner=accounts.userId(username)
        return try {
            val id=remoteId(owner)
            val api=SupabaseAccounts(context)
            val rows=JSONArray(api.requestFor(owner,"GET","/rest/v1/mentor_profiles?id=eq.$id&select=nickname,bio,avatar_path"))
            val preferences=JSONArray(api.requestFor(owner,"GET","/rest/v1/mentor_preferences?user_id=eq.$id&select=address,phone"))
            val remotePreferences=preferences.optJSONObject(0) ?: JSONObject()
            val avatarPath=rows.getJSONObject(0).optString("avatar_path").takeIf { it.isNotBlank() }
            val avatar=avatarPath?.let { SupabaseStorage(context).cacheAvatar(owner,it) } ?: cached.avatar
            val profile=UserProfile(rows.getJSONObject(0).getString("nickname"),
                remotePreferences.optString("address",cached.address),remotePreferences.optString("phone",cached.phone),
                cached.email,avatar,rows.getJSONObject(0).optString("bio"))
            local.update(username,profile,"")
            profile
        } catch (_:Exception) { cached }
    }
    override fun update(username:String,profile:UserProfile,newPassword:String):Boolean {
        return try {
            val owner=accounts.userId(username)
            val old=local.load(username) ?: return false
            // Changing the auth email needs a separate confirmation flow; do not alter it locally.
            if(!old.email.equals(profile.email,true)) return false
            val id=remoteId(owner)
            val api=SupabaseAccounts(context)
            val changes=JSONObject().put("nickname",profile.fullName).put("bio",profile.bio.take(500))
            if(profile.avatar!=old.avatar && profile.avatar?.startsWith("content://")==true) {
                changes.put("avatar_path",SupabaseStorage(context).uploadAvatar(owner,android.net.Uri.parse(profile.avatar)))
            }
            if(newPassword.isNotEmpty()) api.requestFor(owner,"PUT","/auth/v1/user",JSONObject().put("password",newPassword))
            api.requestFor(owner,"POST","/rest/v1/rpc/mentor_update_profile",changes
                .put("address",profile.address).put("phone",profile.phone))
            val cachedAvatar=if(profile.avatar?.startsWith("content://")==true)
                SupabaseStorage(context).cacheAvatar(owner,"${id}/profile/avatar") ?: old.avatar else profile.avatar
            local.update(username,UserProfile(profile.fullName,profile.address,profile.phone,profile.email,cachedAvatar,profile.bio),"")
        } catch (_:Exception) { false }
    }
}
