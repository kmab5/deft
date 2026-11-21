plugins {
    id("deft.android.library")
    id("deft.android.hilt")
}
dependencies {
    api(project(":core:model"))
    api(project(":core:database"))
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
}
