// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.joasasso.minitoolbox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.joasasso.minitoolbox"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // 1) Compose BOM for version alignment
    implementation(platform(libs.androidx.compose.bom))

    // 2) Core Compose UI
    implementation(libs.androidx.ui)

    // 3) Compose Material 3 (Actualización a Material3)
    implementation(libs.material3)
    implementation(libs.material)


    // 4) Icons, Activity & Navigation
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.compose.v280)  // Predictive back support

    // 5) Glance (AppWidget + Material3)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // 6) Core Android
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)

    //Funcionalidades
    implementation(libs.compose.qr.code)

    //Gson para parsear JSON
    implementation("com.google.protobuf:protobuf-java:4.31.1")

    // 7) Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // 8) Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
