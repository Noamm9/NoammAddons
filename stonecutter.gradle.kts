plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds every Stonecutter version."
    dependsOn(stonecutter.tasks.named("build"))
}

listOf("build", "runClient", "runLegitClientPreprocessed").forEach { name ->
    tasks.register("${name}Active") {
        group = "stonecutter"
        description = "Runs $name on the active Stonecutter version."
        dependsOn(":${stonecutter.current?.project}:$name")
    }
}