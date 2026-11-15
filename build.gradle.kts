plugins { base }

val checkNetworkIsolation by tasks.registering {
    group = "verification"
    description = "Reject HTTP client dependencies outside the explicit matching module."
    doLast {
        val forbiddenGroups = setOf("com.squareup.okhttp3", "com.squareup.retrofit2", "com.android.volley")
        subprojects.filter { it.path != ":data:matching" }.forEach { module ->
            module.configurations.filter { it.isCanBeResolved && it.name.endsWith("RuntimeClasspath") }.forEach { configuration ->
                configuration.incoming.resolutionResult.allComponents.forEach component@{ component ->
                    val id = component.moduleVersion ?: return@component
                    check(id.group !in forbiddenGroups && !(id.group == "io.ktor" && id.name.startsWith("ktor-client")) && !id.name.startsWith("coil-network")) {
                        "${module.path} contains prohibited network dependency $id"
                    }
                }
            }
        }
    }
}
tasks.check {
    dependsOn(checkNetworkIsolation)
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
