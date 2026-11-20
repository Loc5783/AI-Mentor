package com.example.asm_final.supabase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthOutagePolicyTest {
    @Test fun temporaryFailuresKeepPreviouslyVerifiedOfflineAccess() {
        listOf(408, 429, 500, 502, 503).forEach { assertTrue(recoverableAuthOutage(it)) }
    }

    @Test fun revokedOrInvalidSessionsNeverGetOfflineAccess() {
        listOf(400, 401, 403, 404, 422).forEach { assertFalse(recoverableAuthOutage(it)) }
    }
}
