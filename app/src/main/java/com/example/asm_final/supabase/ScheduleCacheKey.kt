package com.example.asm_final.supabase

import java.security.MessageDigest

internal object ScheduleCacheKey {
    fun of(endpoint: String, remoteId: String, owner: Int, op: String, day: Int, date: String): String {
        require(endpoint.isNotBlank() && remoteId.isNotBlank() && owner > 0)
        require(op == "entries" || op == "todos")
        val identity = "$endpoint|$remoteId|$owner|$op|$day|$date"
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
