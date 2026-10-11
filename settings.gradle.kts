pluginManagement {
    includeBuild("build-logic")
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
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}
rootProject.name = "tailormyresume"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:network")
include(":core:navigation")
include(":core:screenshot")
include(":core:testing")

include(":feature:applications:api")
include(":feature:applications:impl")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:profile:api")
include(":feature:profile:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:analysis:api")
include(":feature:analysis:impl")
include(":feature:tailor:api")
include(":feature:tailor:impl")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
    TailorMyResume requires JDK 17+ but it is currently using JDK ${JavaVersion.current()}.
    Java Home: [${System.getProperty("java.home")}]
    """.trimIndent()
}
