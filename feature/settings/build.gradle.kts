plugins {
    id("deft.android.library")
    id("deft.android.compose")
    id("deft.android.hilt")
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":library:scanner"))
    implementation(libs.navigation.compose)
    implementation(libs.activity.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
}

dependencies { testImplementation(libs.junit) }
