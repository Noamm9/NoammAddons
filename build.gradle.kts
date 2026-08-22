import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("noammaddons.variants")
    id("noammaddons.preprocessing")
    id("noammaddons.publishing")
    id("noammaddons.loom")
    idea
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val minecraft_version = project.property("minecraft_version") as String
val loader_version = project.property("loader_version") as String
val fabric_kotlin_version = project.property("fabric_kotlin_version") as String
val mod_version = project.property("mod_version") as String
val maven_group = project.property("maven_group") as String
val mod_name = project.property("mod_name") as String
val fabric_version = project.property("fabric_version") as String
val modmenu_version = project.property("modmenu_version") as String
val ktor_version = project.property("ktor_version") as String
val iris_version = project.property("iris_version") as String
val universalcraft_version = project.property("universalcraft_version") as String

version = mod_version
group = maven_group

base { archivesName.set(mod_name) }

val bundled = configurations.create("bundled")
val processedIncludeJarsDir = layout.buildDirectory.dir("processIncludeJars")

configurations {
    implementation.get().extendsFrom(bundled)
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://api.modrinth.com/maven")
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    implementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    compileOnly("maven.modrinth:iris:$iris_version")
    compileOnly("com.terraformersmc:modmenu:$modmenu_version")

    compileOnly("io.github.llamalad7:mixinextras-fabric:0.5.4")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.4")

    bundled("io.github.classgraph:classgraph:4.8.191")
    bundled("com.github.Noamm9:datafixer:d60875927e")
    bundled("gg.essential:universalcraft-26.1-fabric:$universalcraft_version")
    bundled("io.ktor:ktor-client-cio:$ktor_version")
    bundled("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-encoding:$ktor_version")
    bundled("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    testImplementation(kotlin("test"))
}

afterEvaluate {
    bundled.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        artifact.moduleVersion.id.let { id ->
            dependencies.add("include", "${id.group}:${id.name}:${id.version}")
        }
    }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_25) } }

fun File.writeWithoutNestedJarMetadata() {
    if (! isFile) return

    val modJson = JsonSlurper().parse(this).let { parsed ->
        @Suppress("UNCHECKED_CAST")
        (parsed as Map<String, Any?>).toMutableMap()
    }

    modJson.remove("jars")
    writeText(JsonOutput.prettyPrint(JsonOutput.toJson(modJson)))
}

fun ProcessResources.writeNestedJarMetadata() {
    dependsOn("processIncludeJars")
    inputs.dir(processedIncludeJarsDir)

    doLast {
        val modJsonFile = destinationDir.resolve("fabric.mod.json")
        val modJson = JsonSlurper().parse(modJsonFile).let { parsed ->
            @Suppress("UNCHECKED_CAST")
            (parsed as Map<String, Any?>).toMutableMap()
        }

        modJson["jars"] = processedIncludeJarsDir.get().asFile.listFiles()
            ?.filter { it.isFile && it.extension == "jar" }
            ?.sortedBy { it.name }
            ?.map { mapOf("file" to "META-INF/jars/${it.name}") }
            .orEmpty()

        modJsonFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(modJson)))
    }
}

fun ProcessResources.writeBuildInfo() {
    doLast {
        val props = Properties()
        props.setProperty("ci", (System.getenv("GITHUB_ACTIONS") == "true").toString())
        props.setProperty("built_at", System.currentTimeMillis().toString())
        destinationDir.resolve("build-info.properties").outputStream().use { props.store(it, null) }
    }
}

tasks.named<ProcessResources>("processResources") {
    doLast {
        destinationDir.resolve("fabric.mod.json").writeWithoutNestedJarMetadata()
    }
}

listOf("processCheatResources", "processLegitResources").forEach { taskName ->
    tasks.named<ProcessResources>(taskName) {
        writeNestedJarMetadata()
        writeBuildInfo()
    }
}

tasks.named<Jar>("jar") {
    destinationDirectory.set(layout.buildDirectory.dir("tmp/intermediateJars"))
    archiveClassifier.set("dev")

    from("LICENSE") {
        rename { "${it}_$mod_name" }
    }
}

listOf("jarCheat", "jarLegit").forEach { taskName ->
    tasks.named<Jar>(taskName) {
        // NoammAddons-<mod_version>-<minecraft_version>-<cheat|legit>.jar
        archiveVersion.set("$mod_version-$minecraft_version")

        dependsOn("processIncludeJars")
        from(processedIncludeJarsDir) {
            into("META-INF/jars")
        }
    }
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}