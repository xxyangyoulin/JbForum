import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.xxyangyoulin.jbforum"
    compileSdk = 36
    val versionNameFromCi = providers.gradleProperty("VERSION_NAME").orNull
    val versionCodeFromCi = providers.gradleProperty("VERSION_CODE").orNull?.toIntOrNull()
    val releaseStoreFilePath = providers.gradleProperty("RELEASE_STORE_FILE").orNull
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: rootProject.file("release.keystore").takeIf { it.exists() }?.absolutePath
    val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
        ?: System.getenv("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
        ?: System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
        ?: System.getenv("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning =
        !releaseStoreFilePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()

    defaultConfig {
        applicationId = "com.xxyangyoulin.jbforum"
        minSdk = 31
        targetSdk = 36
        versionCode = versionCodeFromCi ?: 8
        versionName = versionNameFromCi ?: "1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material3:material3:1.5.0-alpha16")
    implementation("androidx.compose.runtime:runtime-saveable:1.7.8")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("dev.chrisbanes.haze:haze:1.7.2")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.31")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
}
