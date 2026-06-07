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

rootProject.name = "MoonPic"

include(":app")

// Core modules
include(":core:core-ui")
include(":core:core-utils")
include(":core:core-data")
include(":core:core-domain")
include(":core:core-db")
include(":core:core-filters")
include(":core:core-camera")
include(":core:core-ocr")
include(":core:core-compress")

// Feature modules
include(":feature:feature-main")
include(":feature:feature-editor")
include(":feature:feature-filters")
include(":feature:feature-camera")
include(":feature:feature-ocr")
include(":feature:feature-scan")
include(":feature:feature-pdf")
include(":feature:feature-gif")
include(":feature:feature-collage")
include(":feature:feature-batch")
include(":feature:feature-settings")

// Native/3rd-party libs
include(":lib:lib-opencv")
include(":lib:lib-neural")
include(":lib:lib-cpp")
