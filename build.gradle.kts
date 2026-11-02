import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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

if (System.getProperty("idea.sync.active").toBoolean()) {
    val hiddenSourceSets = listOfNotNull(
        sourceSets["cheat"],
        sourceSets["legit"],
        sourceSets["main"].takeUnless { stonecutter.current.isActive }
    )

    hiddenSourceSets.forEach { sourceSet ->
        sourceSet.java.setSrcDirs(emptyList<File>())
        sourceSet.kotlin.setSrcDirs(emptyList<File>())
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
val datafixer_version = project.property("datafixer_version") as String
val devauth_version = project.property("devauth_version") as String
val mixinextras_version = project.property("mixinextras_version") as String
val classgraph_version = project.property("classgraph_version") as String
val datafixerCapability = "me.owdding:item-data-fixer-$minecraft_version"

version = mod_version
group = maven_group

base { archivesName.set(mod_name) }

val bundled = configurations.create("bundled")
val processedIncludeJarsDir = layout.buildDirectory.dir("processIncludeJars")

val shadedGroups = mapOf(
    "io.ktor" to "io.ktor",
    "com.squareup.okio" to "okio",
    "io.github.classgraph" to "io.github.classgraph",
    "me.owdding" to "me.owdding.dfu",
    "com.charleskorn.kaml" to "com.charleskorn.kaml",
    "it.krzeminski" to "it.krzeminski.snakeyaml",
    "net.thauvin.erik.urlencoder" to "net.thauvin.erik.urlencoder"
)

val relocatedPackages = shadedGroups.values

val shadedLibraries = bundled.incoming.artifactView {
    componentFilter { it is ModuleComponentIdentifier && it.group in shadedGroups }
}.files

configurations { implementation.get().extendsFrom(bundled) }

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.teamresourceful.com/repository/thatgravyboat/") {
        content { includeGroup("me.owdding") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    implementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:$devauth_version")
    compileOnly("maven.modrinth:iris:$iris_version")
    compileOnly("com.terraformersmc:modmenu:$modmenu_version")

    compileOnly("io.github.llamalad7:mixinextras-fabric:$mixinextras_version")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:$mixinextras_version")

    bundled("io.github.classgraph:classgraph:$classgraph_version")
    bundled("me.owdding:item-data-fixer:$datafixer_version") {
        capabilities { requireCapability(datafixerCapability) }
    }
    bundled("io.ktor:ktor-client-cio-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-encoding:$ktor_version")
    bundled("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    testImplementation(kotlin("test"))
}

afterEvaluate {
    bundled.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        val id = artifact.moduleVersion.id
        if (id.group in shadedGroups) return@forEach
        dependencies.add("include", "${id.group}:${id.name}:${id.version}")
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
    tasks.named<ShadowJar>(taskName) {
        // NoammAddons-<mod_version>-<minecraft_version>-<cheat|legit>.jar
        archiveVersion.set("$mod_version-$minecraft_version")

        dependsOn("processIncludeJars")
        from(processedIncludeJarsDir) {
            into("META-INF/jars")
        }

        from(shadedLibraries.elements.map { jars -> jars.map(::zipTree) }) {
            exclude(
                "fabric.mod.json",
                "**/module-info.class",
                "META-INF/MANIFEST.MF",
                "META-INF/native-image/**",
                "META-INF/versions/*/OSGI-INF/**"
            )
        }

        relocatedPackages.forEach { relocate(it, "$maven_group.lib.$it") }
        mergeServiceFiles()
        filesMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
    workingDir = temporaryDir
}