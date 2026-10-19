import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

// Secrets live in android-app/local.properties (git-ignored), never in source. The Secrets Gradle
// Plugin turns each key into a manifest placeholder ${KEY} and a BuildConfig.KEY field:
//   MAPS_API_KEY         -> AndroidManifest com.google.android.geo.API_KEY
//   GOOGLE_WEB_CLIENT_ID -> BuildConfig.GOOGLE_WEB_CLIENT_ID ("Continue with Google"; the OAuth web
//                           client id, same value as the backend's GOOGLE_OAUTH_CLIENT_ID)
// local.defaults.properties (committed, no real values) keeps the build working without them.
secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
    // Not a secret: declared below as BuildConfig.API_BASE_URL with a default.
    ignoreList.add("API_BASE_URL")
    ignoreList.add("RELEASE_API_BASE_URL")
}

// Secrets plugin 2.0.1 (latest) writes BuildConfig String values without quotes under AGP 9, which
// does not compile. Re-declare those fields quoted, reading the same files with the same precedence
// (local.properties over local.defaults.properties). The manifest placeholders stay the plugin's.
val secretValues = Properties().apply {
    for (name in listOf("local.defaults.properties", "local.properties")) {
        val file = rootProject.file(name)
        if (file.exists()) file.inputStream().use { load(it) }
    }
}

// ---------- Release (docs/DEPLOYMENT.md, step 5) ----------
// The deployed API: LAUNDRYHUB_API_URL (environment) or RELEASE_API_BASE_URL (local.properties). HTTPS only.
val releaseApiUrl: String = (System.getenv("LAUNDRYHUB_API_URL")
    ?: secretValues.getProperty("RELEASE_API_BASE_URL", "")).trim().let { if (it.isEmpty() || it.endsWith("/")) it else "$it/" }
// Upload/signing key from environment variables only (never in Git or local.properties).
// (Names differ from SigningConfig's own properties so they are not shadowed inside signingConfigs { }.)
val releaseStoreFile: String? = System.getenv("LAUNDRYHUB_KEYSTORE_FILE")
val releaseStorePassword: String? = System.getenv("LAUNDRYHUB_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("LAUNDRYHUB_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("LAUNDRYHUB_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

androidComponents {
    onVariants { variant ->
        for (key in listOf("MAPS_API_KEY", "GOOGLE_WEB_CLIENT_ID")) {
            val value = secretValues.getProperty(key, "")
            variant.buildConfigFields?.put(key, com.android.build.api.variant.BuildConfigField(
                "String", "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"", "from local.properties"))
        }
    }
}

android {
    namespace = "com.laundryhub.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.laundryhub.app"
        minSdk = 24
        targetSdk = 37
        // Bump per release: ./gradlew assembleRelease -PversionCode=2 -PversionName=1.0.1
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Backend URL: API_BASE_URL in local.properties, else 10.0.2.2 (the host machine as seen from the
        // emulator). A USB phone can use http://localhost:8080/api/ with `adb reverse tcp:8080 tcp:8080`.
        val apiBaseUrl = secretValues.getProperty("API_BASE_URL", "http://10.0.2.2:8080/api/")
        buildConfigField("String", "API_BASE_URL", "\"" + apiBaseUrl + "\"")

        // The Secrets plugin fills ${MAPS_API_KEY} for the app only; unit tests merge the manifest too.
        manifestPlaceholders["MAPS_API_KEY"] = secretValues.getProperty("MAPS_API_KEY", "")
        // Plain HTTP only for the local backend (debug); release talks HTTPS only.
        manifestPlaceholders["usesCleartextTraffic"] = "true"

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

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "API_BASE_URL", "\"" + releaseApiUrl + "\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Retrofit/Gson read DTO fields by reflection; shrinking would need keep rules, so it stays off.
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

    // R1 bookings list with pull-to-refresh
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // MVVM
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    // 04a: Google map with draggable pin, "Use my current location"
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

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

// Stops a release build early when the deployed URL or the signing key is missing.
val checkReleaseConfig by tasks.registering {
    doLast {
        if (!releaseApiUrl.startsWith("https://")) {
            throw GradleException("Set LAUNDRYHUB_API_URL (or RELEASE_API_BASE_URL in local.properties) to the " +
                "deployed HTTPS API, e.g. https://laundryhub-api.onrender.com/api/")
        }
        if (!hasReleaseSigning) {
            throw GradleException("Set LAUNDRYHUB_KEYSTORE_FILE, LAUNDRYHUB_KEYSTORE_PASSWORD, LAUNDRYHUB_KEY_ALIAS " +
                "and LAUNDRYHUB_KEY_PASSWORD to sign the release (docs/DEPLOYMENT.md).")
        }
    }
}
tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        dependsOn(checkReleaseConfig)
    }
}
