plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.beppe2880.opencodemobile.core.testing"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
