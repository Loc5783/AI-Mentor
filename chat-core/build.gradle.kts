import java.util.Properties

plugins { alias(libs.plugins.android.library) }
val chatProperties = Properties().apply {
    val config = rootProject.file("api.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
fun chatQuoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.example.mentor.chat"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        for (slot in 1..3) buildConfigField("String", "CHAT_GEMINI_API_KEY_$slot",
            chatQuoted(chatProperties.getProperty("CHAT_GEMINI_API_KEY_$slot", "")))
        buildConfigField("String", "CHAT_GEMINI_MODEL",
            chatQuoted(chatProperties.getProperty("CHAT_GEMINI_MODEL", "gemini-flash-latest")))
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    api(project(":learning-core"))
    testImplementation(libs.junit)
}

