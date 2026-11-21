pluginManagement {
    includeBuild("build-logic")
    repositories { mavenCentral {
            content {
                excludeGroupByRegex("androidx\\..*")
                excludeGroupByRegex("com\\.android.*")
                excludeGroup("com.google.testing.platform")
            }
        }
        google(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral {
            content {
                excludeGroupByRegex("androidx\\..*")
                excludeGroupByRegex("com\\.android.*")
                excludeGroup("com.google.testing.platform")
            }
        }
        google() }
}
rootProject.name = "Deft"
include(":app", ":core:model", ":core:common", ":core:database", ":core:datastore", ":core:designsystem", ":format:epub", ":library:scanner", ":feature:library", ":feature:settings")

include(":format:readium", ":feature:reader", ":feature:reader-settings", ":feature:book")

include(":tracking", ":feature:celebrate")
include(":library:organization")
