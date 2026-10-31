package com.example.asm_final.supabase

interface SupabaseTransport {
    fun request(method: String, path: String, body: org.json.JSONObject? = null, token: String? = null): String
}
