pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenCodeMobile"

include(":app")
include(":core:ui")
include(":core:mvi")
include(":core:domain")
include(":core:api")
include(":core:network")
include(":core:data")
include(":core:testing")
include(":feature:connect")
include(":feature:sessions")
include(":feature:chat")
include(":feature:settings")
include(":feature:terminal")
include(":feature:files")
