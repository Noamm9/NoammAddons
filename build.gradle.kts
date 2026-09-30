import groovy.json.*
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    `maven-publish`
    idea
}

val minecraftVersion = sc.current.version
val modVersion = property("mod_version") as String
val modName = property("mod_name") as String
val modId = property("mod_id") as String
val flavor = sc.current.project.substringAfterLast('-')
val datafixerCapability = "me.owdding:item-data-fixer-${if (minecraftVersion == "26.1.2") "26.1" else minecraftVersion}"

version = modVersion
group = property("maven_group") as String
base.archivesName.set(modName)

stonecutter {
    constants["cheat"] = flavor == "cheat"
}

idea.module {
    isDownloadSources = true
    isDownloadJavadoc = true
}

val bundled = configurations.create("bundled")
configurations.implementation.get().extendsFrom(bundled)
val processedIncludeJarsDir = layout.buildDirectory.dir("processIncludeJars")

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.teamresourceful.com/repository/thatgravyboat/") {
        content { includeGroup("me.owdding") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    compileOnly("maven.modrinth:iris:${property("iris_version")}")
    compileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    compileOnly("io.github.llamalad7:mixinextras-fabric:0.5.5")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.5")

    bundled("io.github.classgraph:classgraph:4.8.195")
    bundled("me.owdding:item-data-fixer:${property("datafixer_version")}") {
        capabilities { requireCapability(datafixerCapability) }
    }

    val universalcraftTarget = if (minecraftVersion == "26.1.2") "26.1" else minecraftVersion
    bundled("gg.essential:universalcraft-$universalcraftTarget-fabric:${property("universalcraft_version")}") {
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }

    val ktorVersion = property("ktor_version")
    bundled("io.ktor:ktor-client-cio:$ktorVersion")
    bundled("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    bundled("io.ktor:ktor-client-encoding:$ktorVersion")
    bundled("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    testImplementation(kotlin("test"))
}

afterEvaluate {
    bundled.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        artifact.moduleVersion.id.let { id ->
            val include = dependencies.add("include", "${id.group}:${id.name}:${id.version}") as ModuleDependency
            if ("${id.group}:${id.name}" == "me.owdding:item-data-fixer") {
                include.capabilities { requireCapability(datafixerCapability) }
            }
        }
    }
}

configure<LoomGradleExtensionAPI> {
    accessWidenerPath.set(rootProject.file("src/main/resources/$modId.accesswidener"))
    runs {
        named("client") {
            runDirectory.set(file("run"))
            generateRunConfig.set(false)
        }
        named("server") { generateRunConfig.set(false) }
    }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_25) } }

tasks.named<ProcessResources>("processResources") {
    dependsOn("processIncludeJars")
    inputs.dir(processedIncludeJarsDir)

    val properties = mapOf(
        "fabric_loader" to project.property("loader_version"),
        "mc_version" to minecraftVersion,
        "mod_version" to modVersion,
        "mod_name" to modName,
        "mod_id" to modId,
        "mod_description" to "$minecraftVersion port of the greatest skyblock mod ever made! Version: ${flavor.replaceFirstChar { it.uppercase() }}",
    ).mapValues { it.value.toString() }

    doLast {
        val modJson = "fabric.mod.json"
        val file = destinationDir.resolve(modJson)
        var content = file.readText()
        properties.forEach { (k, v) -> content = content.replace("\${$k}", v) }
        file.delete()

        val result = (JsonSlurper().setType(JsonParserType.LAX).parseText(content) as Map<*, *>).toMutableMap()
        result["jars"] = processedIncludeJarsDir.get().asFile.listFiles()
            ?.filter { it.isFile && it.extension == "jar" }?.sortedBy { it.name }
            ?.map { mapOf("file" to "META-INF/jars/${it.name}") }.orEmpty()

        destinationDir.resolve(modJson).writeText(JsonOutput.prettyPrint(JsonOutput.toJson(result)))

        val props = Properties()
        props.setProperty("ci", (System.getenv("GITHUB_ACTIONS") == "true").toString())
        props.setProperty("built_at", System.currentTimeMillis().toString())
        destinationDir.resolve("build-info.properties").outputStream().use { props.store(it, null) }
    }
}

tasks.named<Jar>("jar") {
    archiveVersion.set("$modVersion-$minecraftVersion")
    archiveClassifier.set(flavor)
    from(rootProject.file("LICENSE")) { rename { "${it}_$modName" } }
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}

publishing.publications.register<MavenPublication>("mavenMod") {
    artifactId = "$modName-$minecraftVersion-$flavor"
    from(components["java"])
}