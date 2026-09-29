import java.util.Properties

// Đọc cấu hình từ local.properties
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

    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.smartattendance"

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

    buildFeatures {
        buildConfig = true
        viewBinding = true
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

    // BOM để tất cả module Ktor dùng cùng version
    implementation(
        platform("io.ktor:ktor-bom:3.2.1")
    )

    // Android HTTP engine
    implementation(
        "io.ktor:ktor-client-android"
    )
}