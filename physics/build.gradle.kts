plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.example.mentor.physics"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    api(project(":learning-core"))
    implementation(project(":science-support"))
    implementation(project(":ui-core"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation(libs.junit)
}

