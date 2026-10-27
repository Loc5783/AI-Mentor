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

rootProject.name = "AI-Mentor"
include(":app", ":math", ":history")

include(":ui-core")
include(":schedule")

include(":data-core")

include(":learning-core", ":chat-core")
include(":literature", ":english", ":geography", ":chemistry", ":physics", ":biology", ":economics-law")

// Keep Gradle module names stable while grouping sources by responsibility.
project(":math").projectDir = file("subjects/math")
project(":literature").projectDir = file("subjects/literature")
project(":english").projectDir = file("subjects/english")
project(":history").projectDir = file("subjects/history")
project(":geography").projectDir = file("subjects/geography")
project(":physics").projectDir = file("subjects/physics")
project(":chemistry").projectDir = file("subjects/chemistry")
project(":biology").projectDir = file("subjects/biology")
project(":economics-law").projectDir = file("subjects/economics-law")
project(":chat-core").projectDir = file("core/chat-core")
project(":data-core").projectDir = file("core/data-core")
project(":learning-core").projectDir = file("core/learning-core")
project(":ui-core").projectDir = file("core/ui-core")