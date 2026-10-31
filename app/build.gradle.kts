// Role: Module build configuration defining Android plugins, SDK settings, dependencies, and compiler options.
// Purpose: Controls the module build environment and dependencies; it does not contain screen business logic.
import java.util.Properties
import java.net.URI

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

val mentorConfig = Properties().apply {
    val file = rootProject.file("api.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val backendUrl = mentorConfig.getProperty("MENTOR_BACKEND_URL", "").trim()
require(backendUrl.isEmpty() || backendUrl.matches(Regex("https://[A-Za-z0-9.-]+(?::[0-9]+)?(?:/[A-Za-z0-9_/-]*)?"))) {
    "MENTOR_BACKEND_URL must be an HTTPS base URL without credentials, query or fragment"
}
val localBackendConfig = Properties().apply {
    val file = rootProject.file("backend.local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val debugBackendUrl = localBackendConfig.getProperty("MENTOR_DEBUG_BACKEND_URL", backendUrl).trim()
require(debugBackendUrl.isEmpty() || runCatching {
    val uri = URI(debugBackendUrl)
    val local = uri.scheme == "http" && (uri.host in listOf("127.0.0.1", "10.0.2.2") || uri.host.matches(Regex("^192\\.168\\.\\d+\\.\\d+$")) || uri.host.matches(Regex("^10\\.\\d+\\.\\d+\\.\\d+$"))) && uri.port in 1..65535
    (uri.scheme == "https" || local) && uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null && (uri.port == -1 || uri.port in 1..65535)
}.getOrDefault(false)) { "Debug backend must use HTTPS or the explicit local emulator host with a port" }

val supabaseConfig = Properties().apply {
    // The publishable project config is safe to bundle; ignored local files override it.
    for (configName in listOf("supabase.local.properties.example", "supabase.properties", "supabase.local.properties")) {
        val configFile = rootProject.file(configName)
        if (configFile.exists()) configFile.inputStream().use { load(it) }
    }
}
val supabaseUrl = supabaseConfig.getProperty("SUPABASE_URL", "").trim()
val supabaseKey = supabaseConfig.getProperty("SUPABASE_PUBLISHABLE_KEY", "").trim()
// Opt-in switch for local UI tests; normal builds still use the configured Supabase project.
val disableSupabaseForTests =
    providers.gradleProperty("disableSupabaseForTests").orNull?.toBoolean() ?: false
val supabaseEnabled =
    supabaseConfig.getProperty("SUPABASE_ENABLED", "false") == "true" && !disableSupabaseForTests
require(!supabaseEnabled || (supabaseUrl.matches(Regex("https://[a-z0-9-]+\\.supabase\\.co")) && supabaseKey.matches(Regex("sb_publishable_[A-Za-z0-9_-]+")))) { "Invalid Supabase public configuration" }

android {
    namespace = "com.example.asm_final"
    compileSdk = 36

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabaseKey\"")
        buildConfigField("boolean", "SUPABASE_ENABLED", supabaseEnabled.toString())
        applicationId = "com.example.asm_final"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MENTOR_BACKEND_URL", "\"$backendUrl\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "MENTOR_BACKEND_URL", "\"$debugBackendUrl\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures { buildConfig = true; compose = true }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.activity.compose)
    implementation(libs.compose.livedata)
    implementation(libs.navigation.compose)
    implementation(libs.fragment.compose)
    implementation(libs.coil.compose)
    implementation(libs.konfetti.compose)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    debugImplementation(libs.compose.test.manifest)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(project(":learning-core"))
    implementation(project(":chat-core"))
    implementation(project(":literature"))
    implementation(project(":english"))
    implementation(project(":geography"))
    implementation(project(":chemistry"))
    implementation(project(":physics"))
    implementation(project(":biology"))
    implementation(project(":economics-law"))
    implementation(project(":data-core"))
    implementation(project(":ui-core"))
    implementation(project(":schedule"))
    implementation(project(":math"))
    implementation(project(":history"))
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
