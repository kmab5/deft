pluginManagement { repositories { mavenCentral {
            content {
                excludeGroupByRegex("androidx\\..*")
                excludeGroupByRegex("com\\.android.*")
                excludeGroup("com.google.testing.platform")
            }
        }
        google(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { mavenCentral {
            content {
                excludeGroupByRegex("androidx\\..*")
                excludeGroupByRegex("com\\.android.*")
                excludeGroup("com.google.testing.platform")
            }
        }
        google(); gradlePluginPortal() } }
rootProject.name = "build-logic"
include(":convention")
