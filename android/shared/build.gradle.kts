// Code shared by the Android app and the iOS app: root navigation, the app shell and the
// ViewModel bindings. On iOS it is linked as the `Shared` framework used by iosApp.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "io.github.beppe2880.opencodemobile.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
        androidResources { enable = true }
        withHostTest {}
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:ui"))
            api(project(":core:mvi"))
            api(project(":core:domain"))
            implementation(project(":core:api"))
            implementation(project(":feature:connect"))
            implementation(project(":feature:sessions"))
            implementation(project(":feature:chat"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:terminal"))
            implementation(project(":feature:files"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.jb.navigation.compose)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            // Keeps the AndroidX patch releases newer than the one JetBrains' navigation maps to.
            implementation(libs.androidx.navigation.compose)
        }
        iosMain.dependencies {
            implementation(project(":core:data"))
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.beppe2880.opencodemobile.shared.resources"
    generateResClass = always
}
