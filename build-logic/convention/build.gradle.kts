plugins {
    kotlin("jvm") version "2.2.21"
    `java-gradle-plugin`
}
gradlePlugin {
    plugins {
        create("networkIsolation") { id = "deft.network-isolation"; implementationClass = "NetworkIsolationPlugin" }
        create("androidApplication") { id = "deft.android.application"; implementationClass = "DeftAndroidApplicationPlugin" }
        create("androidLibrary") { id = "deft.android.library"; implementationClass = "DeftAndroidLibraryPlugin" }
        create("androidCompose") { id = "deft.android.compose"; implementationClass = "DeftAndroidComposePlugin" }
        create("androidHilt") { id = "deft.android.hilt"; implementationClass = "DeftAndroidHiltPlugin" }
        create("androidRoom") { id = "deft.android.room"; implementationClass = "DeftAndroidRoomPlugin" }
        create("jvmLibrary") { id = "deft.jvm.library"; implementationClass = "DeftJvmLibraryPlugin" }
    }
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies {
    implementation("com.android.tools.build:gradle:8.13.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.21")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.57.2")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.21-2.0.4")
    implementation("androidx.room:room-gradle-plugin:2.8.4")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:13.1.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    testImplementation("junit:junit:4.13.2")
}
