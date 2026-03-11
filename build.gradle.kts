plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
}

val mc = stonecutter.current.version
val loader = "fabric"

version = "${property("mod.version")}+${mc}"
base.archivesName = property("mod.id") as String

repositories {
    @Suppress("UnstableApiUsage")
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://maven.parchmentmc.org", "org.parchmentmc")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("parchment".mc(mc))
    })

    modRuntimeOnly(libs.devauth)

    modImplementation("fabric-api".mc(mc))
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.language.kotlin)

    shadow(libs.classgraph)
    shadow(libs.serialization)
    shadow(libs.websocket)
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    //accessWidenerPath = rootProject.file("src/main/resources/template.accesswidener")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()
fun DependencyHandler.shadow(dep: Any) { include(dep);modImplementation(dep) }