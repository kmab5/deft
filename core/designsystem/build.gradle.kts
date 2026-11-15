plugins {
    id("deft.android.library")
    id("deft.android.compose")
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation(platform("androidx.compose:compose-bom:2025.10.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
tasks.withType<Test>().configureEach {
    systemProperty("deft.recordScreenshots", providers.gradleProperty("deft.recordScreenshots").getOrElse("false"))
}
