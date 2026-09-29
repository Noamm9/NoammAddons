import groovy.json.JsonOutput
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

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
val datafixerCapability = "me.owdding:item-data-fixer-$minecraftVersion"

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
    maven("https://jitpack.io")
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
    if (minecraftVersion == "26.2") {
        bundled("me.owdding:item-data-fixer:${property("datafixer_version")}") {
            capabilities { requireCapability(datafixerCapability) }
        }
    } else {
        bundled("com.github.Noamm9:datafixer:d60875927e")
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
    inputs.property("modVersion", modVersion)
    inputs.dir(processedIncludeJarsDir)
    doLast {
        for (name in listOf("fabric.mod", "$modId.mixins")) {
            val source = destinationDir.resolve("$name.json5")
            val content = source.readText().replace("\${version}", modVersion)
            val json = JsonSlurper().setType(JsonParserType.LAX).parseText(content) as Map<*, *>
            val result = json.toMutableMap()
            if (name == "fabric.mod") {
                result["description"] = "$minecraftVersion port of the greatest skyblock mod ever made! Version: ${flavor.replaceFirstChar { it.uppercase() }}"
                result["jars"] = processedIncludeJarsDir.get().asFile.listFiles()
                    ?.filter { it.isFile && it.extension == "jar" }
                    ?.sortedBy { it.name }
                    ?.map { mapOf("file" to "META-INF/jars/${it.name}") }
                    .orEmpty()
            }
            destinationDir.resolve("$name.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(result)))
            source.delete()
        }
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