plugins {
    id("deft.android.library")
    id("deft.android.compose")
    id("deft.android.hilt")
}
dependencies {
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":library:scanner"))
    implementation(libs.navigation.compose)
    implementation(libs.activity.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
}

dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(project(":core:common"))
    testImplementation("androidx.room:room-ktx:2.8.4")
    testImplementation("androidx.datastore:datastore-preferences:1.1.7")
    testImplementation("androidx.work:work-testing:2.10.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.test.manifest)
}
