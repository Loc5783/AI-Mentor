package com.example.asm_final.supabase

import android.content.Context
import com.example.asm_final.BuildConfig
import com.example.asm_final.accounts.*
import com.example.asm_final.activities.database.DatabaseHelper
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.backend.BackendSession
import com.example.asm_final.onboarding.LocalLearnerProfiles
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Supabase identity is linked by verified UUID, never by an email or nickname match. */
class SupabaseAccounts @JvmOverloads constructor(context: Context, private val http: SupabaseTransport = SupabaseHttp()) {
    private val context = context.applicationContext
    companion object {
        private val lock = Any()
        private val refreshLocks = java.util.concurrent.ConcurrentHashMap<Int,Any>()
        private val generations = java.util.concurrent.ConcurrentHashMap<Int,java.util.concurrent.atomic.AtomicLong>()
        private fun generation(owner:Int)=generations.computeIfAbsent(owner) { java.util.concurrent.atomic.AtomicLong() }
    }
    private fun sessions(owner: Int) = BackendSession(context, owner, BuildConfig.SUPABASE_URL, "supabase_sessions")

    @Throws(Exception::class)
    fun signIn(email: String, password: String): AuthTicket = synchronized(lock) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) throw BackendClient.Failure("EMAIL_REQUIRED",400)
        val session = JSONObject(http.request("POST", "/auth/v1/token?grant_type=password",
            JSONObject().put("email",email.trim()).put("password",password)))
        normalize(session)
        val id = UUID.fromString(session.getJSONObject("user").getString("id")).toString()
        val profile = JSONArray(http.request("GET","/rest/v1/mentor_profiles?id=eq.$id&select=nickname",token=session.getString("access_token")))
        check(profile.length()==1) { "Missing Supabase profile" }
        val nickname = profile.getJSONObject(0).getString("nickname")
        val preferences=JSONArray(http.request("GET","/rest/v1/mentor_preferences?user_id=eq.$id&select=*",token=session.getString("access_token"))).getJSONObject(0)
        RemoteAccountLinks(context).use { links ->
            DatabaseHelper(context).use { db ->
                var owner=links.owner(BuildConfig.SUPABASE_URL,id)
                if(owner==0) {
                    owner=db.addRemoteAccount("sb_$id",session.getJSONObject("user").getString("email"),nickname,0).toInt()
                    check(owner>0)
                    links.bind(owner,BuildConfig.SUPABASE_URL,id)
                }
                sessions(owner).save(session)
                val values=android.content.ContentValues().apply {
                    put("display_name",nickname)
                    put("grade",preferences.optInt("grade",0))
                    put("step",preferences.optInt("onboarding_step",0))
                    val subjects=preferences.optJSONArray("subjects") ?: JSONArray()
                    put("subjects",(0 until subjects.length()).joinToString(",") { subjects.getString(it).uppercase(java.util.Locale.ROOT) })
                    put("goal",preferences.optInt("goal",0));put("minutes",preferences.optInt("minutes",0))
                }
                db.writableDatabase.update("learner_profiles",values,"user_id=?",arrayOf(owner.toString()))
                AuthTicket("sb_$id",owner,LocalLearnerProfiles(context).load(owner))
            }
        }
    }

    @Throws(Exception::class)
    fun register(name: String,email: String,password: String): AuthTicket {
        val result=JSONObject(http.request("POST",SupabaseAuthRedirect.withCallback("/auth/v1/signup"),JSONObject().put("email",email.trim()).put("password",password)
            .put("data",JSONObject().put("nickname",name.trim()))))
        // Supabase may return an obfuscated user with no identities for an existing email
        // when email confirmation is enabled. It has no access token, but it is not a
        // pending confirmation: no new account or confirmation message was created.
        val user = result.optJSONObject("user") ?: result
        val identities = user.optJSONArray("identities")
        if (identities != null && identities.length() == 0) {
            throw BackendClient.Failure("ACCOUNT_ALREADY_EXISTS", 409)
        }
        if(result.optString("access_token").isEmpty()) throw BackendClient.Failure("EMAIL_CONFIRMATION_REQUIRED",202)
        // Never authenticate by local password; verify the server identity through the normal path.
        return signIn(email,password)
    }

    private fun normalize(session: JSONObject) {
        require(session.getString("access_token").isNotBlank() && session.getString("refresh_token").isNotBlank())
        if(!session.has("expires_at")) session.put("expires_at",System.currentTimeMillis()/1000+session.optLong("expires_in",3600))
    }

    @Throws(Exception::class)
    fun restore(username: String): AuthTicket? = synchronized(lock) {
        val local=LocalAccountRepository(context)
        val owner=local.userId(username)
        if(owner<=0) return@synchronized null
        val session=sessions(owner).load() ?: return@synchronized null
        val expected=RemoteAccountLinks(context).use { it.remoteId(owner,BuildConfig.SUPABASE_URL) }
        if(expected.isEmpty()) return@synchronized null
        try {
            val current = if(session.optLong("expires_at") <= System.currentTimeMillis()/1000+60) {
                JSONObject(http.request("POST","/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",session.getString("refresh_token")))).also {
                    normalize(it)
                    check(it.getJSONObject("user").getString("id")==expected)
                    sessions(owner).save(it)
                }
            } else session
            val user=JSONObject(http.request("GET","/auth/v1/user",token=current.getString("access_token")))
            if(user.getString("id")!=expected) { sessions(owner).clear(); return@synchronized null }
        } catch(failure: BackendClient.Failure) {
            if(failure.status in listOf(400,401,403)) { sessions(owner).clear(); return@synchronized null }
            throw failure
        } catch(offline: java.io.IOException) {
            // A previously verified session may access its own encrypted local cache offline.
        }
        AuthTicket(username,owner,LocalLearnerProfiles(context).load(owner))
    }

    @Throws(Exception::class)
    fun hasSession(owner: Int): Boolean = synchronized(lock) {
        owner>0 && com.example.mentor.data.ActiveUser.get(context)==owner && sessions(owner).load()!=null
    }

    fun requestFor(owner: Int, method: String, path: String, body: JSONObject? = null): String =
        authorized(owner) { token -> http.request(method,path,body,token) }

    internal fun <T> authorized(owner:Int, action:(String)->T):T {
        check(owner > 0 && com.example.mentor.data.ActiveUser.get(context) == owner)
        val epoch=generation(owner).get()
        val token=synchronized(refreshLocks.computeIfAbsent(owner) { Any() }) {
            var session = sessions(owner).load() ?: throw BackendClient.Failure("AUTHENTICATION_REQUIRED",401)
            val expected = RemoteAccountLinks(context).use { it.remoteId(owner,BuildConfig.SUPABASE_URL) }
            check(expected.isNotEmpty())
            if(session.optLong("expires_at") <= System.currentTimeMillis()/1000+60) {
                session = JSONObject(http.request("POST","/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",session.getString("refresh_token"))))
                normalize(session)
                check(session.getJSONObject("user").getString("id")==expected)
                synchronized(lock) {
                    check(epoch==generation(owner).get() && com.example.mentor.data.ActiveUser.get(context)==owner)
                    sessions(owner).save(session)
                }
            }
            session.getString("access_token")
        }
        val response=action(token)
        check(epoch==generation(owner).get() && com.example.mentor.data.ActiveUser.get(context)==owner)
        return response
    }

    /** Remove credentials before returning; remote revocation may run on the app executor. */
    fun detach(owner: Int): Runnable = synchronized(lock) {
        generation(owner).incrementAndGet()
        val stored=sessions(owner).take()
        Runnable {
            runCatching {
                val old=sessions(owner).decode(stored) ?: return@runCatching
                http.request("POST","/auth/v1/logout?scope=local",token=old.getString("access_token"))
            }
        }
    }
}
