import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.tertiaryinfotech.hrportal"
    compileSdk = 36

    // local.properties is gitignored — it carries the SDK path and the public Google OAuth
    // client id, so a fresh checkout builds fine (with the Google button simply hidden).
    val localPropsFile = rootProject.file("local.properties")
    val localProps = Properties().apply {
        if (localPropsFile.exists()) load(localPropsFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.tertiaryinfotech.hrportal"
        minSdk = 24
        targetSdk = 36
        // The existing Play "hrportal" app already has versionCode 14 (1.2) sent for closed-track
        // review, so each new release must increment from there.
        versionCode = 18
        versionName = "1.6"
        vectorDrawables { useSupportLibrary = true }

        // Google Sign-In (native, Custom Tabs + PKCE) — mirrors the iOS app's GIDClientID.
        // The "Android" OAuth client id from Google Cloud Console, registered against the app's
        // *Play App Signing* SHA-1 (not the upload key). Public by design: a native OAuth client
        // has no secret, and the backend verifies every id_token with Google and checks the
        // audience allow-list. Left empty, the app hides the Google button entirely, exactly as
        // the iOS build does when GOOGLE_IOS_CLIENT_ID is unset.
        val googleClientId = localProps.getProperty("GOOGLE_ANDROID_CLIENT_ID").orEmpty()
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        // Google's Android convention: the redirect URI is the client id reversed as a scheme.
        // Registered as a manifest placeholder so the redirect activity only claims the scheme
        // this build is actually configured for.
        manifestPlaceholders["googleRedirectScheme"] =
            googleClientId.split(".").reversed().joinToString(".").ifBlank { "com.tertiaryinfotech.hrportal.nogoogle" }
    }

    // Release signing is read from keystore.properties when present (gitignored),
    // so a signed AAB can be produced for Google Play without hardcoding secrets.
    val keystorePropsFile = rootProject.file("keystore.properties")
    val hasKeystore = keystorePropsFile.exists()
    val keystoreProps = Properties().apply {
        if (hasKeystore) load(keystorePropsFile.inputStream())
    }
    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    // Chrome Custom Tabs — the Google sign-in consent screen (no Play Services SDK needed).
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Networking + JSON (Retrofit rides the same OkHttp client + kotlinx.serialization models
    // the app already used; mirrors the iOS URLSession + Codable layer)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Image loading (employee/profile avatars)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Session-adjacent app preferences (remembered email) — cookie persistence stays in the
    // existing PersistentCookieJar, which DataStore's key-value model doesn't fit.
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
