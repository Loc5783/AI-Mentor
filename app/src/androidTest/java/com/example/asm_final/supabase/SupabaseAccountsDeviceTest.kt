package com.example.asm_final.supabase

import androidx.test.platform.app.InstrumentationRegistry
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.backend.BackendSession
import com.example.asm_final.BuildConfig
import com.example.asm_final.activities.database.DatabaseHelper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SupabaseAccountsDeviceTest {
    @Test fun confirmationDoesNotCreateLocalAuthenticatedAccount() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val api=SupabaseAccounts(context,object:SupabaseTransport {
            override fun request(method:String,path:String,body:JSONObject?,token:String?)="{}"
        })
        try { api.register("Test","test@example.invalid","test-password"); fail("Confirmation required") }
        catch(e:BackendClient.Failure) { assertEquals("EMAIL_CONFIRMATION_REQUIRED",e.code) }
    }
    @Test fun encryptedSessionRestoreAndIdentityMismatch() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val id=UUID.randomUUID().toString()
        var returnedId=id
        val transport=object:SupabaseTransport {
            override fun request(method:String,path:String,body:JSONObject?,token:String?):String = when {
                path.startsWith("/auth/v1/token") -> JSONObject().put("access_token","test-access-token").put("refresh_token","test-refresh-token").put("expires_in",3600).put("user",JSONObject().put("id",id).put("email","$id@example.invalid")).toString()
                path.startsWith("/rest/v1/mentor_preferences") -> "[{\"grade\":null,\"subjects\":[],\"onboarding_step\":0}]"
                path.startsWith("/rest/v1/mentor_profiles") -> "[{\"nickname\":\"Test learner\"}]"
                path=="/auth/v1/user" -> JSONObject().put("id",returnedId).toString()
                else -> "{}"
            }
        }
        val api=SupabaseAccounts(context,transport)
        val ticket=api.signIn("$id@example.invalid","not-stored")
        try {
            val stored=context.getSharedPreferences("supabase_sessions",0).getString("owner_${ticket.userId}","")!!
            assertFalse(stored.contains("test-access-token")); assertFalse(stored.contains("not-stored"))
            assertEquals(ticket.userId,SupabaseAccounts(context,transport).restore(ticket.username)!!.userId)
            returnedId=UUID.randomUUID().toString()
            assertNull(api.restore(ticket.username))
            assertNull(BackendSession(context,ticket.userId,BuildConfig.SUPABASE_URL,"supabase_sessions").load())
            api.signIn("$id@example.invalid","not-stored")
            val revoke=api.detach(ticket.userId)
            assertNull(BackendSession(context,ticket.userId,BuildConfig.SUPABASE_URL,"supabase_sessions").load())
            revoke.run()
        } finally {
            BackendSession(context,ticket.userId,BuildConfig.SUPABASE_URL,"supabase_sessions").clear()
            DatabaseHelper(context).use { it.writableDatabase.delete("users","id=?",arrayOf(ticket.userId.toString())) }
        }
    }
}
