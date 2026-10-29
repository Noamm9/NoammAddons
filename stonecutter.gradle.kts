plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds every Stonecutter version."
    dependsOn(stonecutter.tasks.named("build"))
}
