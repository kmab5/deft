plugins {
    id("deft.jvm.library")
}

apply(plugin = "com.google.devtools.ksp")

dependencies {
    implementation(project(":core:model"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.google.dagger:hilt-core:2.57.2")
    implementation("com.google.dagger:dagger:2.57.2")
    implementation("javax.inject:javax.inject:1")
    add("ksp", "com.google.dagger:hilt-compiler:2.57.2")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "64m"
}
