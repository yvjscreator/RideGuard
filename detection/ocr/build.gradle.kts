plugins { alias(libs.plugins.android.library) }

android {
    namespace = "dev.rideguard.detection.ocr"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.mlkit.text.recognition)
}
