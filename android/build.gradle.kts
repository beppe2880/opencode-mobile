plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

// Formatting rules live in .editorconfig. core/api is generated and keeps the generator's layout.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/*.kt")
        targetExclude("core/api/**", "**/build/**")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
}

// Kotlin Multiplatform modules run their JVM unit tests as `testAndroidHostTest`; keep the
// usual `testDebugUnitTest` entry point working for every module.
subprojects {
    plugins.withId("com.android.kotlin.multiplatform.library") {
        tasks.register("testDebugUnitTest") { dependsOn(tasks.matching { it.name == "testAndroidHostTest" }) }
    }
}
