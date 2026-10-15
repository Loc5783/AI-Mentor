plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.mathmodule"
    buildFeatures { buildConfig = true }
    compileSdk = 36

    defaultConfig {
        buildConfigField("String", "MATH_GEMINI_API_KEY_1", "\"${project.findProperty("MATH_GEMINI_API_KEY_1") ?: project.findProperty("MATH_GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "MATH_GEMINI_API_KEY_2", "\"${project.findProperty("MATH_GEMINI_API_KEY_2") ?: ""}\"")
        buildConfigField("String", "MATH_GEMINI_API_KEY_3", "\"${project.findProperty("MATH_GEMINI_API_KEY_3") ?: ""}\"")
        buildConfigField("String", "MATH_GEMINI_MODEL", "\"${project.findProperty("MATH_GEMINI_MODEL") ?: "gemini-flash-latest"}\"")
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
