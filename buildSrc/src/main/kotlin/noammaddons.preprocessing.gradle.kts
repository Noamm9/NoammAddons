val projectVersion = project.property("mod_version") as String
val modId = project.property("mod_id") as String

val preprocessLegit = registerPreprocessTask("legit")
val preprocessCheat = registerPreprocessTask("cheat")
val preprocessMain = registerPreprocessMainTask()

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    val modId = findProperty("mod_id").toString()

    dependsOn(preprocessLegit)
    exclude("fabric.mod.json5", "$modId.mixins.json5")

    from(layout.buildDirectory.dir("preprocessed/legit/resources")) {
        include("fabric.mod.json", "$modId.mixins.json")
    }

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to projectVersion))
    }
}

fun registerVariantResources(variantName: String, preprocessTask: TaskProvider<Task>) {
    val capitalized = variantName.replaceFirstChar { it.uppercase() }
    val taskName = "process${capitalized}Resources"

    if (tasks.findByName(taskName) == null) tasks.register<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5", "$modId.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json", "$modId.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to projectVersion))
        }
    }
    else tasks.named<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5", "$modId.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json", "$modId.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to projectVersion))
        }
    }
}

registerVariantResources("legit", preprocessLegit)
registerVariantResources("cheat", preprocessCheat)