plugins { alias(libs.plugins.android.library) }

android {
    namespace = "dev.rideguard.detection.accessibility"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":core:model"))
    implementation(project(":core:calculator"))
    implementation(project(":core:settings"))
    implementation(project(":platforms:uber"))
    implementation(project(":platforms:cabify"))
    implementation(project(":platforms:didi"))
    implementation(project(":overlay"))
}
