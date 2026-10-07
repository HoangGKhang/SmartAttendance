import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}


// =========================
// Đọc local.properties
// =========================

val localProperties = Properties().apply {

    val localPropertiesFile =
        rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile
            .inputStream()
            .use {
                load(it)
            }
    }
}


// =========================
// Android config
// =========================

android {

    namespace = "com.example.smartattendance"

    compileSdk = 37

    defaultConfig {

        applicationId = "com.example.smartattendance"

        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"


        // =========================
        // Supabase BuildConfig
        // =========================

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")}\""
        )
    }


    // =========================
    // Build Types
    // =========================

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }


    // =========================
    // Android Features
    // =========================

    buildFeatures {

        // Cho phép:
        // BuildConfig.SUPABASE_URL
        // BuildConfig.SUPABASE_PUBLISHABLE_KEY
        buildConfig = true

        // Cho phép:
        // ActivityLoginBinding
        viewBinding = true
    }


    // =========================
    // Java compatibility
    // =========================

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}


// =========================
// Dependencies
// =========================

dependencies {

    // =========================
    // AndroidX
    // =========================

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.androidx.activity.ktx
    )

    implementation(
        libs.androidx.constraintlayout
    )

    implementation(
        libs.material
    )


    // =========================
    // Supabase
    // =========================

    // Login / Logout / Session / JWT
    implementation(
        libs.supabase.auth
    )

    // Data API / PostgreSQL / profiles
    implementation(
        libs.supabase.postgrest
    )


    // =========================
    // Ktor
    // =========================

    // HTTP engine cho Android
    implementation(
        libs.ktor.client.android
    )


    // =========================
    // Unit Test
    // =========================

    testImplementation(
        libs.junit
    )


    // =========================
    // Android Test
    // =========================

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}