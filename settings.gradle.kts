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

rootProject.name = "ASM-final"
include(":app", ":math", ":science-support", ":history")

include(":ui-core")

include(":data-core")

// Nine school subjects; science-support is shared legacy infrastructure, not a subject.
include(":learning-core", ":chat-core")
include(":literature", ":english", ":geography", ":chemistry", ":physics", ":biology", ":economics-law")
