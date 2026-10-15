import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun escapeBuildConfigValue(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

// History owns a separate Gemini configuration. It is not shared with the other modules.
val historyGeminiApiKey = localProperties.getProperty("HISTORY_GEMINI_API_KEY")
    ?: providers.gradleProperty("HISTORY_GEMINI_API_KEY").orNull
    ?: ""
val historyGeminiModel = localProperties.getProperty("HISTORY_GEMINI_MODEL")
    ?: providers.gradleProperty("HISTORY_GEMINI_MODEL").orNull
    ?: "gemini-flash-latest"

android {
    namespace = "com.example.historymodule"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField(
            "String",
            "HISTORY_GEMINI_API_KEY",
            "\"${escapeBuildConfigValue(historyGeminiApiKey)}\""
        )
        buildConfigField(
            "String",
            "HISTORY_GEMINI_MODEL",
            "\"${escapeBuildConfigValue(historyGeminiModel)}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
