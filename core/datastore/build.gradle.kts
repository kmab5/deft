plugins {
    id("deft.android.library")
    id("deft.android.hilt")
}
dependencies {
    implementation(project(":core:model"))
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
