plugins {
    id("deft.android.library")
    id("deft.android.compose")
    id("deft.android.hilt")
}

android { namespace = "com.kmab.deft.feature.readersettings" }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(libs.activity.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.datastore:datastore-preferences:1.1.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
    debugImplementation(libs.compose.ui.test.manifest)
}

val checkReaderFonts by tasks.registering {
    group = "verification"
    val fonts = fileTree("src/main/assets") { include("reader-fonts/**", "reader-font-previews/**") }
    inputs.files(fonts)
    doLast {
        check(fonts.files.count { it.extension == "woff2" } == 33) { "Each of 11 reader fonts needs three faces." }
        check(fonts.files.sumOf { it.length() } <= 3_000_000) { "Bundled reader fonts exceed the 3 MB budget." }
    }
}
tasks.named("check") { dependsOn(checkReaderFonts) }
