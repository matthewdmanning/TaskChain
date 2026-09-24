import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Reads the gitignored .env file, if one exists. Commit .env.example instead.
// Android gives an application no process environment, so a value can only be
// read here, at build time. Gradle writes each entry into BuildConfig, and
// BuildConfig ships inside the APK. Anyone who unpacks the APK reads the value.
// Put only non-sensitive configuration here. Keep a real secret on a server.
val dotenv: Map<String, String> = rootProject.file(".env")
    .takeIf(File::exists)
    ?.readLines()
    ?.map(String::trim)
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
    ?.associate { it.substringBefore("=").trim() to it.substringAfter("=").trim().removeSurrounding("\"") }
    .orEmpty()

android {
    namespace = "com.taskchain"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taskchain"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        dotenv.forEach { (key, value) ->
            buildConfigField("String", key, "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = dotenv.isNotEmpty()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
