enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "PlannerApp"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:navigation")
include(":core:ui")

include(":feature:home")
//include(":feature:friend")
//include(":feature:intro")
include(":core:database")
include(":core:network")
