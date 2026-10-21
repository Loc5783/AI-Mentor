plugins {
    alias(libs.plugins.compose.compiler)
 alias(libs.plugins.android.library) }
android {
    namespace = "com.example.mentor.ui"
    compileSdk = 36
    buildFeatures { compose = true }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(project(":data-core"))
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
