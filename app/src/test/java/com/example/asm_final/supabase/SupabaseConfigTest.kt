package com.example.asm_final.supabase

import com.example.asm_final.BuildConfig
import com.example.asm_final.backend.BackendEndpointPolicy
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Properties

/**
 * Offline regression tests verifying Supabase configuration integrity:
 * - Proper project ref and publishable key targeting (qvhdoczieavwjmfrcibe).
 * - Absence of server secret / service_role keys.
 * - Configuration override precedence.
 * - Local FastAPI debug fallback preservation.
 */
class SupabaseConfigTest {

    @Test
    fun effectiveBuildConfigTargetsNewProjectAndNeverOldProject() {
        val url = BuildConfig.SUPABASE_URL
        assertNotNull(url)
        assertTrue("Must start with https://", url.startsWith("https://"))
        assertTrue("Must target project qvhdoczieavwjmfrcibe", url.contains("qvhdoczieavwjmfrcibe"))
        assertFalse("Must never silently target deprecated project hnxphwuaccztzanjcuca", url.contains("hnxphwuaccztzanjcuca"))
        assertEquals("https://qvhdoczieavwjmfrcibe.supabase.co", url)
    }

    @Test
    fun effectivePublishableKeyIsPublicClientKeyAndNeverSecret() {
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        assertNotNull(key)
        assertTrue("Must be publishable client key format", key.startsWith("sb_publishable_"))
        assertFalse("Must never contain service_role secret", key.contains("service_role"))
        assertFalse("Must never contain secret_ key", key.contains("secret"))
        assertEquals("sb_publishable_zVfQ6zmld4qsQbuD4Osw_w_uaPgQHYa", key)
    }

    @Test
    fun supabaseIsEnabledByDefaultInProductionBuildConfig() {
        assertTrue("SUPABASE_ENABLED must be true for cloud build", BuildConfig.SUPABASE_ENABLED)
    }

    @Test
    fun noServerSecretsExposedInBuildConfig() {
        val fields = BuildConfig::class.java.declaredFields.map { it.name }
        for (field in fields) {
            val upper = field.uppercase()
            assertFalse("Must not expose service_role in BuildConfig: $field", upper.contains("SERVICE_ROLE"))
            assertFalse("Must not expose master secret in BuildConfig: $field", upper.contains("MASTER_KEY") || upper.contains("DB_PASSWORD"))
        }
    }

    @Test
    fun configurationOverridePrecedenceWorksCorrectly() {
        // Precedence: supabase.local.properties.example < supabase.properties < supabase.local.properties
        val example = """
            SUPABASE_URL=https://qvhdoczieavwjmfrcibe.supabase.co
            SUPABASE_PUBLISHABLE_KEY=sb_publishable_zVfQ6zmld4qsQbuD4Osw_w_uaPgQHYa
            SUPABASE_ENABLED=true
        """.trimIndent()

        val localOverride = """
            SUPABASE_URL=https://custom-override.supabase.co
            SUPABASE_PUBLISHABLE_KEY=sb_publishable_custom_override_key
            SUPABASE_ENABLED=false
        """.trimIndent()

        val props = Properties()
        props.load(ByteArrayInputStream(example.toByteArray(Charsets.UTF_8)))
        assertEquals("https://qvhdoczieavwjmfrcibe.supabase.co", props.getProperty("SUPABASE_URL"))
        assertEquals("true", props.getProperty("SUPABASE_ENABLED"))

        // Applying override simulates higher-precedence file
        props.load(ByteArrayInputStream(localOverride.toByteArray(Charsets.UTF_8)))
        assertEquals("https://custom-override.supabase.co", props.getProperty("SUPABASE_URL"))
        assertEquals("sb_publishable_custom_override_key", props.getProperty("SUPABASE_PUBLISHABLE_KEY"))
        assertEquals("false", props.getProperty("SUPABASE_ENABLED"))
    }

    @Test
    fun localFastApiDebugFallbackPreserved() {
        // When local debug backend URL is configured, BackendEndpointPolicy allows loopback
        val localDebugHost = "http://127.0.0.1:8000"
        assertTrue(BackendEndpointPolicy.allowed(localDebugHost, true))

        val emulatorHost = "http://10.0.2.2:8000"
        assertTrue(BackendEndpointPolicy.allowed(emulatorHost, true))

        // But release mode rejects cleartext HTTP
        assertFalse(BackendEndpointPolicy.allowed(localDebugHost, false))
    }
}
