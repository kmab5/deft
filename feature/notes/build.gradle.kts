plugins {
    id("deft.android.library")
    id("deft.android.compose")
    id("deft.android.hilt")
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":library:notes"))
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(project(":core:database"))
    testImplementation("androidx.room:room-ktx:2.8.4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
    debugImplementation(libs.compose.ui.test.manifest)
}
