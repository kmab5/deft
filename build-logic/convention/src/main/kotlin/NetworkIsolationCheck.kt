import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@DisableCachingByDefault(because = "Every invocation must inspect the current resolved project dependency graphs")
abstract class NetworkIsolationCheck : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mergedReleaseManifest: RegularFileProperty

    @TaskAction
    fun verify() {
        project.subprojects.filter { it.path != ":data:matching" }.forEach { module ->
            module.configurations.filter { it.isCanBeResolved && it.name.endsWith("RuntimeClasspath") }.forEach { config ->
                config.incoming.resolutionResult.allComponents.forEach { component ->
                    component.moduleVersion?.let { id ->
                        check(!forbiddenNetworkDependency(id.group, id.name)) {
                            "${module.path} contains prohibited network dependency $id"
                        }
                    }
                }
            }
        }
        val appRuntime = project.project(":app").configurations.getByName("releaseRuntimeClasspath")
        val matchingIncluded = appRuntime.incoming.resolutionResult.allComponents.any {
            (it.id as? ProjectComponentIdentifier)?.projectPath == ":data:matching"
        }
        validateReleaseNetworkManifest(mergedReleaseManifest.get().asFile, matchingIncluded)
    }
}

internal fun forbiddenNetworkDependency(group: String, name: String): Boolean =
    group in setOf("com.squareup.okhttp3", "com.squareup.retrofit2", "com.android.volley") ||
        (group == "io.ktor" && name.startsWith("ktor-client")) ||
        (group == "io.coil-kt.coil3" && name.startsWith("coil-network-"))

internal fun validateReleaseNetworkManifest(file: File, matchingIncluded: Boolean) {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    val permissions = factory.newDocumentBuilder().parse(file).getElementsByTagName("uses-permission")
    val internet = (0 until permissions.length).any {
        permissions.item(it).attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "name")?.nodeValue ==
            "android.permission.INTERNET"
    }
    check(matchingIncluded || !internet) { ":app merged release manifest requests INTERNET without :data:matching" }
}

class NetworkIsolationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) { "Network isolation belongs on the root project" }
        val guard = target.tasks.register("checkNetworkIsolation", NetworkIsolationCheck::class.java) {
            it.group = "verification"
            it.description = "Verify HTTP dependencies and the actual merged release manifest."
        }
        val app = target.project(":app")
        app.pluginManager.withPlugin("com.android.application") {
            val variants = app.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            variants.onVariants(variants.selector().withBuildType("release")) { variant ->
                guard.configure { it.mergedReleaseManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)) }
            }
        }
        target.tasks.named("check").configure { it.dependsOn(guard) }
    }
}
