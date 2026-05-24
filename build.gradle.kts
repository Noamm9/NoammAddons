import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("net.fabricmc.fabric-loom-remap")
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("noammaddons.variants")
    id("noammaddons.preprocessing")
    id("noammaddons.publishing")
    id("noammaddons.loom")
}

val minecraft_version: String by project
val loader_version: String by project
val fabric_kotlin_version: String by project
val mod_version: String by project
val maven_group: String by project
val mod_name: String by project
val fabric_version: String by project
val modmenu_version: String by project
val iris_version: String by project
val ktor_version: String by project

version = mod_version
group = maven_group

base { archivesName.set(mod_name) }

val bundled by configurations.creating

configurations {
    implementation.get().extendsFrom(bundled)
}

repositories {
    maven(url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1"))
    maven(url = uri("https://maven.terraformersmc.com/"))
    maven(url = uri("https://api.modrinth.com/maven"))
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modCompileOnly("maven.modrinth:iris:$iris_version")
    modCompileOnly("com.terraformersmc:modmenu:$modmenu_version")

    implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.4.1")

    bundled("io.github.classgraph:classgraph:4.8.174")
    bundled("io.ktor:ktor-client-okhttp-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-websockets-jvm:$ktor_version")
    bundled("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    bundled("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    modImplementation("com.github.Noamm9:DataFixer:7148621a34")
    include("com.github.Noamm9:DataFixer:7148621a34")

    testImplementation(kotlin("test"))
}

afterEvaluate {
    bundled.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        artifact.moduleVersion.id.let { id ->
            dependencies.add("include", "${id.group}:${id.name}:${id.version}")
        }
    }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

tasks.named<Jar>("jar") {
    destinationDirectory.set(layout.buildDirectory.dir("tmp/intermediateJars"))
    archiveClassifier.set("dev")

    from("LICENSE") {
        rename { "${it}_$mod_name" }
    }
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}