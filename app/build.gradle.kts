import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}
val releaseSecrets = Properties().apply {
    val source = rootProject.file("signing.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}
fun signingValue(name: String): String? = System.getenv("COELHO_${name}") ?: releaseSecrets.getProperty(name)
val releaseSigningReady = listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD").all { !signingValue(it).isNullOrBlank() }
dependencyLocking { lockAllConfigurations() }
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.guava" && requested.name == "guava") {
            useVersion(if (name.startsWith("kapt")) "33.7.2-jre" else "33.7.2-android")
            because("CVE-2026-102554 fixed in Guava 33.7.2")
        }
    }
}
android {
    namespace = "com.coelho.music"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.coelho.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    signingConfigs {
        if (releaseSigningReady) create("distribution") {
            storeFile = rootProject.file(signingValue("STORE_FILE")!!)
            storePassword = signingValue("STORE_PASSWORD")
            keyAlias = signingValue("KEY_ALIAS")
            keyPassword = signingValue("KEY_PASSWORD")
            enableV1Signing = false // minSdk 26 supports APK Signature Scheme v2.
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isDebuggable = false
            // Keep class names readable for auditing. No packer or obfuscation is used.
            isMinifyEnabled = false
            isShrinkResources = false
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("distribution")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
}
tasks.configureEach {
    if (name == "validateSigningRelease" || name == "packageRelease" || name == "packageReleaseBundle") {
        doFirst { check(releaseSigningReady) { "Release requires your own signing key. Run scripts/New-ReleaseSigning.ps1 or configure signing.properties. No debug fallback is allowed." } }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-compiler:2.57.2")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    // Coil 3 does not include an HTTP client unless a network module is added.
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
}
