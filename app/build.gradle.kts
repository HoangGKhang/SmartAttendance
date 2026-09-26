import java.util.Properties

// Đọc các giá trị trong local.properties ở root project.
// Ví dụ:
// SUPABASE_URL=https://xxxxx.supabase.co
// SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxxxx
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            load(it)
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartattendance"

    // Giữ Android 36.1
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.smartattendance"

        // Supabase Kotlin
        minSdk = 26

        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties -> BuildConfig.SUPABASE_URL
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )

        // local.properties -> BuildConfig.SUPABASE_PUBLISHABLE_KEY
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")}\""
        )
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

    // Cho phép sử dụng BuildConfig
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // =========================
    // AndroidX
    // =========================

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // =========================
    // Test
    // =========================

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // =========================
    // Supabase
    // =========================

    implementation(
        platform("io.github.jan-tennert.supabase:bom:3.2.0")
    )

    implementation(
        "io.github.jan-tennert.supabase:auth-kt"
    )

    implementation(
        "io.github.jan-tennert.supabase:postgrest-kt"
    )

    // =========================
    // Ktor
    // =========================

    implementation(
        "io.ktor:ktor-client-android:3.2.3"
    )
}