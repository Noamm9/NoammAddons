val preprocessLegit = registerPreprocessTask("legit")
val preprocessCheat = registerPreprocessTask("cheat")

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    dependsOn(preprocessLegit)
    exclude("fabric.mod.json5", "noammaddons.mixins.json5")

    from(layout.buildDirectory.dir("preprocessed/legit/resources")) {
        include("fabric.mod.json", "noammaddons.mixins.json")
    }

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

fun registerVariantResources(variantName: String, preprocessTask: TaskProvider<Task>) {
    val capitalized = variantName.replaceFirstChar { it.uppercase() }
    val taskName = "process${capitalized}Resources"

    if (tasks.findByName(taskName) == null) tasks.register<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5", "noammaddons.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json", "noammaddons.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
    else tasks.named<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5", "noammaddons.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json", "noammaddons.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
}

registerVariantResources("legit", preprocessLegit)
registerVariantResources("cheat", preprocessCheat)