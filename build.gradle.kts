plugins {
    base
    id("deft.network-isolation")
}

tasks.check {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
