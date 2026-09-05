import java.util.Properties

// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.oss.licenses)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.joasasso.minitoolbox"
    compileSdk = 37

    val keystoreProps = Properties()
    val keystoreFile = project.layout.projectDirectory.file("keystore.properties").asFile
    if (keystoreFile.exists()) {
        keystoreFile.inputStream().use { keystoreProps.load(it) }
    }
    val hasSigningConfig = keystoreProps.isNotEmpty()

    val localProperties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    val metricsEndpoint = localProperties.getProperty("METRICS_ENDPOINT") ?: ""

    defaultConfig {
        applicationId = "com.joasasso.minitoolbox"
        minSdk = 28
        targetSdk = 37
        versionCode = 23
        versionName = "1.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configuración de telemetría (se carga de local.properties, no se versiona)
        buildConfigField("String", "METRICS_ENDPOINT", "\"$metricsEndpoint\"")
    }

    signingConfigs {
        // Creamos el config "release" SOLO si tenemos el archivo
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    sourceSets {
        // Las fixtures del contrato de claves viven en la raíz del repo y las comparten
        // los tests de Kotlin y los del backend en TypeScript. Se exponen como recurso
        // de test en lugar de duplicarlas, para que no puedan desincronizarse.
        getByName("test") {
            resources.srcDir("../metrics-fixtures")
        }
    }

    testOptions {
        unitTests {
            // Robolectric necesita los recursos empaquetados para levantar un Context real.
            isIncludeAndroidResources = true
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkReleaseBuilds = false
    }
}

configurations.configureEach {
    exclude(group = "com.google.protobuf", module = "protobuf-javalite")
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
    implementation(libs.androidx.compose.runtime)

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
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.runtime.saveable)
    implementation(libs.androidx.ui.graphics)

    // 7) Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)
    // Implementación real de org.json: en tests unitarios el stub de android.jar
    // lanza "not mocked" en cada llamada.
    testImplementation(libs.json)
    // Context y DataStore reales en la JVM, sin emulador.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // 8) Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kotlin.reflect)

    // Camera
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    //AR Camera
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)

    // --- AR / SceneView ---
    implementation(libs.arsceneview)
    implementation(libs.arcore)

    //Pomodoro Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    //Proveedor único de ListenableFuture
    implementation(libs.guava.android)

    // --- Ads + Consent ---
    implementation(libs.google.ads) {
        // Evita que Ads traiga el stub 1.0
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    implementation(libs.admob.unity)
    implementation(libs.unity.sdk)
    implementation(libs.admob.meta)
    implementation(libs.meta.sdk)

    implementation(libs.google.ump)

    implementation(libs.androidx.media)

    implementation(libs.reorderable)

    implementation(libs.play.services.oss.licenses) {
        // Esta librería arrastra androidx.compose.material3:1.5.0-alpha17, que está
        // compilada contra foundation 1.11.0-beta02 y rompe en runtime con el
        // foundation 1.12.0 que fija el BOM (AbstractMethodError en CustomStyle).
        // Excluyéndola, material3 queda alineado con el resto de Compose.
        exclude(group = "androidx.compose.material3")
    }
    implementation(libs.billing.ktx)
    implementation(libs.androidx.browser)

    implementation(libs.androidx.work.runtime.ktx)

    // --- Firebase App Check ---
    implementation(platform(libs.firebase.bom))
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
}

// cualquier configuración que dependa de un archivo no versionado debe degradar de forma segura cuando ese archivo falta, y la verificación es correr assembleDebug sin él.