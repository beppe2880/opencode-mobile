plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "io.github.beppe2880.opencodemobile.benchmarks"
    compileSdk = 37

    defaultConfig {
        // Macrobenchmark traces need API 28; the app itself still supports 26.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Emulators only give relative numbers (frame counts, before/after); see the
        // android-benchmarks skill. On a phone pass `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=`.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    buildTypes {
        // Pairs with the app's `benchmark` build type; the test APK itself stays debuggable.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" }
}

dependencies {
    implementation(libs.junit)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
