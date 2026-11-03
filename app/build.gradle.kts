plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Passed in by the workflow: -PhelperId=... -PplayerId=... -Plabel=...
val helperId = (findProperty("helperId") as String?) ?: "com.fce.onlinemedia.tidal"
val playerId = (findProperty("playerId") as String?) ?: "com.aspiro.tidal"
val label = (findProperty("label") as String?) ?: "Auxio (car link)"

android {
    namespace = "com.fce.onlinemedia"
    compileSdk = 34
    defaultConfig {
        applicationId = helperId
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["playerPackage"] = playerId
        manifestPlaceholders["appLabel"] = label
        buildConfigField("String", "PLAYER_PACKAGE", "\"$playerId\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
}
