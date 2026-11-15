plugins {
    id("deft.jvm.library")
    `java-test-fixtures`
}
dependencies {
    implementation(project(":core:model"))
    implementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
