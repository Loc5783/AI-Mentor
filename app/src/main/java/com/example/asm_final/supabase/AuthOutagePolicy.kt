package com.example.asm_final.supabase

/** Only transient server failures may fall back to a previously verified local session. */
internal fun recoverableAuthOutage(status: Int): Boolean =
    status == 408 || status == 429 || status in 500..599
