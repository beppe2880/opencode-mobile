import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** `version.txt` at the repository root is bumped by release-please; `-PversionName` overrides it. */
val appVersionName = (project.findProperty("versionName") as String?)
    ?: rootProject.file("../version.txt").takeIf { it.exists() }?.readText()?.trim()
    ?: "dev"

/** 1.2.3 -> 1002003 (up to 999 minor and patch versions), always higher than the release before. */
val appVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull()
    ?: appVersionName.split(".").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }
        ?.let { (major, minor, patch) -> major * 1_000_000 + minor * 1_000 + patch }
        ?.coerceAtLeast(1)
    ?: 1

/**
 * Release signing from `OCM_KEYSTORE_*` environment variables (CI) or a gitignored
 * `android/keystore.properties` (storeFile, storePassword, keyAlias, keyPassword).
 * Without either, release builds fall back to the debug key and are only for local testing.
 */
val releaseSigning: Map<String, String>? = run {
    val file = rootProject.file("keystore.properties")
    val props = Properties().apply { if (file.exists()) file.inputStream().use { load(it) } }
    val values = mapOf(
        "storeFile" to (System.getenv("OCM_KEYSTORE_FILE") ?: props.getProperty("storeFile")),
        "storePassword" to (System.getenv("OCM_KEYSTORE_PASSWORD") ?: props.getProperty("storePassword")),
        "keyAlias" to (System.getenv("OCM_KEY_ALIAS") ?: props.getProperty("keyAlias")),
        "keyPassword" to (System.getenv("OCM_KEY_PASSWORD") ?: props.getProperty("keyPassword")),
    )
    if (values.values.all { !it.isNullOrBlank() }) values.mapValues { it.value!! } else null
}

// In CI a release build must never fall back to the public debug key: users could not update
// from such an APK to properly signed ones.
if (System.getenv("CI") != null && releaseSigning == null &&
    gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
) {
    error("Release signing is incomplete: set all OCM_KEYSTORE_* / OCM_KEY_* variables.")
}

android {
    namespace = "io.github.beppe2880.opencodemobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.beppe2880.opencodemobile"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseSigning != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            // `-PfakeData=true` runs the UI on in-memory sample repositories (see src/debug).
            buildConfigField("boolean", "USE_FAKE_DATA", (project.findProperty("fakeData") == "true").toString())
            resValue("string", "app_name", "OpenCode Mobile Dev")
        }
        release {
            signingConfig = signingConfigs.getByName(if (releaseSigning != null) "release" else "debug")
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "USE_FAKE_DATA", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            resValue("string", "app_name", "OpenCode Mobile")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:mvi"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:api"))
    implementation(project(":feature:connect"))
    implementation(project(":feature:sessions"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:terminal"))
    // Link parsing for the in-memory fake data module.
    debugImplementation(project(":core:network"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
