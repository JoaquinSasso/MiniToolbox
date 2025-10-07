import java.util.Properties

// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "com.joasasso.minitoolbox"
    compileSdk = 36

    val keystoreProps = Properties()
    val keystoreFile = project.layout.projectDirectory.file("keystore.properties").asFile
    if (keystoreFile.exists()) {
        keystoreFile.inputStream().use { keystoreProps.load(it) }
    }

    defaultConfig {
        applicationId = "com.joasasso.minitoolbox"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Creamos el config "release" SOLO si tenemos el archivo
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        // hereda configuración de release para que AGP lo considere "publicable"
        create("oss") {
            initWith(getByName("release"))
            // firma con debug para poder instalar fácil desde Run
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false               // mantenerlo no-depurable = publicable
            applicationIdSuffix = ".oss"       // opcional, para instalar junto a debug
            versionNameSuffix = "-oss"         // opcional
            // si tu release tiene minifyEnabled=true y te molesta:
            // isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true // puedes activarlo después con ProGuard
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.androidx.foundation)



    // 4) Icons, Activity & Navigation
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

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

    //Librerias para manejar datasets
    implementation(libs.protobuf.java)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.material3)
    implementation(libs.foundation)
    implementation(libs.androidx.runtime.saveable)
    implementation(libs.androidx.runtime)


    // 7) Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // 8) Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Camera
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    //AR Camera
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // --- AR / SceneView ---
    implementation(libs.arsceneview)
    implementation(libs.arcore)

    //Proveedor único de ListenableFuture
    implementation(libs.guava.android)

    // --- Ads + Consent ---
    implementation(libs.google.ads) {
        // Evita que Ads traiga el stub 1.0
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation(libs.google.ump)

    implementation("androidx.media:media:1.7.1")

    implementation(libs.reorderable)

    implementation(libs.play.services.oss.licenses)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.browser)

    implementation("androidx.work:work-runtime-ktx:2.9.0")
}