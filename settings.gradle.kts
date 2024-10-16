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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Andriod_Labs"
include(":app")
include(":app:lab07")
include(":app:lab08")
include(":app:lab06")
include(":app:myfirstcoroutine")
include(":app:lab12")
include(":app:lab10")
include(":app:lab11")
include(":app:lab13")
include(":app:lab14")
include(":app:lab15")
