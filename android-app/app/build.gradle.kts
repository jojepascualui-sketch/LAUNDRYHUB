import java.util.Properties

plugins {
    id("com.android.application")
}

// Secrets live in android-app/local.properties (git-ignored), never in source.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.laundryhub.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.laundryhub.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 10.0.2.2 is the host machine as seen from the Android emulator.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api/\"")

        // "Continue with Google": the OAuth *web* client id, the same value as the backend's
        // GOOGLE_OAUTH_CLIENT_ID. Add GOOGLE_WEB_CLIENT_ID=... to local.properties (blank = disabled).
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID",
            "\"" + localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "") + "\"")

        // Google Maps key: add MAPS_API_KEY=... to local.properties.
        manifestPlaceholders["mapsApiKey"] = localProps.getProperty("MAPS_API_KEY", "")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    testOptions {
        // Robolectric reads the real resources for the screenshot test.
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // ./gradlew testDebugUnitTest -Pscreenshots renders the auth screens to PNG.
            it.systemProperty("screenshots", project.hasProperty("screenshots").toString())
            it.systemProperty("screenshotDir",
                layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time (token expiry, order dates) on minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.activity:activity:1.9.3")

    // MVVM
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    // REST client
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Token storage (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.0.0")

    // "Continue with Google" (Credential Manager)
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
