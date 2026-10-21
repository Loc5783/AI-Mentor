plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.example.historymodule"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    api(project(":learning-core"))
    implementation(project(":ui-core"))
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
}
