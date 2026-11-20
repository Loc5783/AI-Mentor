package com.example.asm_final.supabase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScheduleCacheKeyTest {
    @Test fun isolatesProjectAccountAndDay() {
        val base = ScheduleCacheKey.of("project-a", "user-a", 1, "todos", 1, "2026-11-23")
        assertEquals(base, ScheduleCacheKey.of("project-a", "user-a", 1, "todos", 1, "2026-11-23"))
        assertNotEquals(base, ScheduleCacheKey.of("project-b", "user-a", 1, "todos", 1, "2026-11-23"))
        assertNotEquals(base, ScheduleCacheKey.of("project-a", "user-b", 1, "todos", 1, "2026-11-23"))
        assertNotEquals(base, ScheduleCacheKey.of("project-a", "user-a", 1, "todos", 1, "2026-11-30"))
        assertNotEquals(base, ScheduleCacheKey.of("project-a", "user-a", 1, "entries", 1, "2026-11-23"))
    }
}
