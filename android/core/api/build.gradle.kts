// Generated OpenCode API client. Sources under src/main/kotlin are produced by
// tools/api-gen (`bun run generate`) and must not be edited by hand.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.beppe2880.opencodemobile.core.api"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        // Generated code; lint findings are not actionable here.
        checkGeneratedSources = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        suppressWarnings.set(true)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    api(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.retrofit.scalars)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
}
