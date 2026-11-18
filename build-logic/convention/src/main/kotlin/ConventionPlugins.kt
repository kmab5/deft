import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

private fun Project.androidTests() {
    val toolchains = extensions.getByType(org.gradle.jvm.toolchain.JavaToolchainService::class.java)
    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        it.javaLauncher.set(toolchains.launcherFor { spec -> spec.languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)) })
        it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
    }
}

private fun Project.quality() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")
    extensions.configure(KtlintExtension::class.java) { it.version.set("1.7.1") }
    extensions.configure(DetektExtension::class.java) {
        it.buildUponDefaultConfig = true
        it.config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }
}

class DeftAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            quality()
            androidTests()
            extensions.configure(ApplicationExtension::class.java) {
                it.namespace = "com.kmab.deft"
                it.compileSdk = 36
                it.defaultConfig.minSdk = 26
                it.defaultConfig.targetSdk = 36
                it.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                it.compileOptions.targetCompatibility = JavaVersion.VERSION_17
                it.testOptions.unitTests.isIncludeAndroidResources = true
            }
            extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension::class.java) { it.jvmToolchain(17) }
        }
    }
}

class DeftAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            quality()
            androidTests()
            extensions.configure(LibraryExtension::class.java) {
                it.namespace = "com.kmab.deft." + path.drop(1).replace(':', '.')
                it.compileSdk = 36
                it.defaultConfig.minSdk = 26
                it.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                it.compileOptions.targetCompatibility = JavaVersion.VERSION_17
                it.testOptions.unitTests.isIncludeAndroidResources = true
            }
            extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension::class.java) { it.jvmToolchain(17) }
        }
    }
}

class DeftAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.withPlugin("com.android.application") {
                extensions.configure(ApplicationExtension::class.java) { it.buildFeatures.compose = true }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure(LibraryExtension::class.java) { it.buildFeatures.compose = true }
            }
            dependencies.add("implementation", dependencies.platform("androidx.compose:compose-bom:2025.10.00"))
            dependencies.add("implementation", "androidx.compose.material3:material3")
            dependencies.add("implementation", "androidx.compose.ui:ui")
            dependencies.add("implementation", "androidx.compose.ui:ui-tooling-preview")
            dependencies.add("debugImplementation", "androidx.compose.ui:ui-tooling")
        }
    }
}

class DeftAndroidHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.dagger.hilt.android")
            pluginManager.apply("com.google.devtools.ksp")
            dependencies.add("implementation", "com.google.dagger:hilt-android:2.57.2")
            dependencies.add("ksp", "com.google.dagger:hilt-compiler:2.57.2")
        }
    }
}

class DeftAndroidRoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")
            extensions.configure(RoomExtension::class.java) { it.schemaDirectory("$projectDir/schemas") }
            dependencies.add("implementation", "androidx.room:room-runtime:2.8.4")
            dependencies.add("ksp", "androidx.room:room-compiler:2.8.4")
            dependencies.add("testImplementation", "androidx.room:room-testing:2.8.4")
        }
    }
}

class DeftJvmLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            quality()
            extensions.configure(KotlinJvmProjectExtension::class.java) { it.jvmToolchain(17) }
            dependencies.add("testImplementation", "junit:junit:4.13.2")
        }
    }
}
