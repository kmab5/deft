plugins {
    id("deft.android.library")
    id("deft.android.compose")
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation("io.coil-kt.coil3:coil-compose-core:3.3.0")
    implementation("io.coil-kt.coil3:coil-core:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.test.manifest)
}
