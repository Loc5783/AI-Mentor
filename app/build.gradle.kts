// Role: Module build configuration defining Android plugins, SDK settings, dependencies, and compiler options.
// Purpose: Controls the module build environment and dependencies; it does not contain screen business logic.
import java.util.Properties

plugins {
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

android {
    namespace = "com.example.asm_final"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.asm_final"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MENTOR_BACKEND_URL", "\"$backendUrl\"")
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
    buildFeatures { buildConfig = true }
}

dependencies {
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
    implementation(project(":math"))
    implementation(project(":science"))
    implementation(project(":programming"))
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
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
