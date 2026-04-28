import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("net.fabricmc.fabric-loom-remap")
    `maven-publish`
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
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
kotlin { jvmToolchain(21) }
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }

val bundled by configurations.creating

configurations {
    implementation.get().extendsFrom(bundled)
}

repositories {
    maven(url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1"))
    maven(url = uri("https://api.modrinth.com/maven"))
    maven(url = uri("https://maven.terraformersmc.com/"))
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

val intermediateJarsDir = layout.buildDirectory.dir("tmp/intermediateJars")
val libsDir = layout.buildDirectory.dir("libs")
val variantSourcesDir = layout.buildDirectory.dir("tmp/variantSources")
val softwareComponentFactory = project.serviceOf<SoftwareComponentFactory>()

tasks.named<Jar>("jar") {
    destinationDirectory.set(intermediateJarsDir)
    archiveClassifier.set("dev")

    from("LICENSE") {
        rename { "${it}_$mod_name" }
    }
}

val sourceSets = the<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main").get()

val cheatJavaDir = layout.buildDirectory.dir("preprocessed/cheat/java")
val cheatKotlinDir = layout.buildDirectory.dir("preprocessed/cheat/kotlin")
val legitJavaDir = layout.buildDirectory.dir("preprocessed/legit/java")
val legitKotlinDir = layout.buildDirectory.dir("preprocessed/legit/kotlin")

fun SourceSet.configurePreprocessedVariant(javaDir: Provider<Directory>, kotlinDir: Provider<Directory>) {
    java.setSrcDirs(listOf(javaDir.get().asFile))
    (extensions.getByName("kotlin") as SourceDirectorySet).apply { setSrcDirs(listOf(kotlinDir.get().asFile)) }
    resources.setSrcDirs(listOf("src/main/resources"))
    compileClasspath += mainSourceSet.compileClasspath
    runtimeClasspath += mainSourceSet.runtimeClasspath - mainSourceSet.output
}

fun Configuration.copyAttributesFrom(other: Configuration) {
    other.attributes.keySet().forEach { key ->
        @Suppress("UNCHECKED_CAST")
        key as Attribute<Any>
        other.attributes.getAttribute(key)?.let { attributes.attribute(key, it) }
    }
}

val cheatSourceSet = sourceSets.create("cheat").apply { configurePreprocessedVariant(cheatJavaDir, cheatKotlinDir) }
val legitSourceSet = sourceSets.create("legit").apply { configurePreprocessedVariant(legitJavaDir, legitKotlinDir) }

tasks.named<KotlinCompile>("compileCheatKotlin") {
    dependsOn("preprocessCheat")
    inputs.dir("src/main/kotlin")
    inputs.dir(cheatKotlinDir)

    compilerOptions { freeCompilerArgs.add("-Xjava-source-roots=${cheatJavaDir.get().asFile.invariantSeparatorsPath}") }
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions { freeCompilerArgs.add("-Xjava-source-roots=${file("src/main/java").invariantSeparatorsPath}") }
}

tasks.named<JavaCompile>("compileCheatJava") {
    dependsOn("preprocessCheat", "compileCheatKotlin")
    inputs.dir("src/main/java")
    inputs.dir(cheatJavaDir)
    options.release.set(21)
    classpath += files(tasks.named<KotlinCompile>("compileCheatKotlin").flatMap { it.destinationDirectory })
}

tasks.named("cheatClasses") {
    dependsOn("compileCheatKotlin", "compileCheatJava")
}

tasks.named<KotlinCompile>("compileLegitKotlin") {
    dependsOn("preprocessLegit")
    inputs.dir("src/main/kotlin")
    inputs.dir(legitKotlinDir)

    compilerOptions { freeCompilerArgs.add("-Xjava-source-roots=${legitJavaDir.get().asFile.invariantSeparatorsPath}") }
}

tasks.named<JavaCompile>("compileLegitJava") {
    dependsOn("preprocessLegit", "compileLegitKotlin")
    inputs.dir("src/main/java")
    inputs.dir(legitJavaDir)
    options.release.set(21)
    classpath += files(tasks.named<KotlinCompile>("compileLegitKotlin").flatMap { it.destinationDirectory })
}

tasks.named("legitClasses") {
    dependsOn("compileLegitKotlin", "compileLegitJava")
}

val jarCheat = tasks.register<Jar>("jarCheat") {
    dependsOn("cheatClasses", "processCheatResources")
    from(cheatSourceSet.output)
    archiveClassifier.set("cheat")
    destinationDirectory.set(intermediateJarsDir)
    from("LICENSE") { rename { "${it}_$mod_name" } }
}

val jarLegit = tasks.register<Jar>("jarLegit") {
    dependsOn("legitClasses", "processLegitResources")
    from(legitSourceSet.output)
    archiveClassifier.set("legit")
    destinationDirectory.set(intermediateJarsDir)
    from("LICENSE") { rename { "${it}_$mod_name" } }
}

val remapJarCheat = tasks.register<RemapJarTask>("remapJarCheat") {
    dependsOn(jarCheat)
    inputFile.set(jarCheat.flatMap { it.archiveFile })
    archiveClassifier.set("cheat")
    destinationDirectory.set(libsDir)
    classpath.from(cheatSourceSet.runtimeClasspath)
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(jarLegit)
    inputFile.set(jarLegit.flatMap { it.archiveFile })
    archiveClassifier.set("legit")
    destinationDirectory.set(libsDir)
    classpath.from(legitSourceSet.runtimeClasspath)
}

val legitSourcesJar = tasks.register<Jar>("legitSourcesJar") {
    dependsOn("preprocessLegit")
    from(legitJavaDir)
    from(legitKotlinDir)
    archiveClassifier.set("legit-sources-raw")
    destinationDirectory.set(intermediateJarsDir)
}

val remapLegitSourcesJar = tasks.register<RemapSourcesJarTask>("remapLegitSourcesJar") {
    dependsOn(legitSourcesJar)
    inputFile.set(legitSourcesJar.flatMap { it.archiveFile })
    archiveClassifier.set("sources")
    destinationDirectory.set(libsDir)
}

val cheatSourcesJar = tasks.register<Jar>("cheatSourcesJar") {
    dependsOn("preprocessCheat")
    from(cheatJavaDir)
    from(cheatKotlinDir)
    archiveClassifier.set("cheat-sources-raw")
    destinationDirectory.set(intermediateJarsDir)
}

val remapCheatSourcesJar = tasks.register<RemapSourcesJarTask>("remapCheatSourcesJar") {
    dependsOn(cheatSourcesJar)
    inputFile.set(cheatSourcesJar.flatMap { it.archiveFile })
    archiveClassifier.set("cheat-sources")
    destinationDirectory.set(libsDir)
}

val baseApiElements = configurations.named("apiElements")
val baseRuntimeElements = configurations.named("runtimeElements")

val cheatApiElements = configurations.create("cheatApiElements").apply {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(baseApiElements.get())
    copyAttributesFrom(baseApiElements.get())
    outgoing.artifact(remapJarCheat)
}

val cheatRuntimeElements = configurations.create("cheatRuntimeElements").apply {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(baseRuntimeElements.get())
    copyAttributesFrom(baseRuntimeElements.get())
    outgoing.artifact(remapJarCheat)
}

val cheatComponent = softwareComponentFactory.adhoc("cheat").apply {
    components.add(this)
    addVariantsFromConfiguration(cheatApiElements) { mapToMavenScope("compile") }
    addVariantsFromConfiguration(cheatRuntimeElements) { mapToMavenScope("runtime") }
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}

publishing {
    publications {
        create<MavenPublication>("mavenLegit") {
            artifactId = mod_name
            from(components["java"])
            artifact(remapLegitSourcesJar) {
                classifier = "legit-sources"
            }
        }

        create<MavenPublication>("mavenCheat") {
            artifactId = mod_name
            from(components["cheat"])
            artifact(remapCheatSourcesJar) {
                classifier = "cheat-sources"
            }
        }
    }
}

loom {
    runs {
        val clientRun = named("client")
        val serverRun = named("server")

        clientRun.configure { ideConfigGenerated(false) }
        serverRun.configure { ideConfigGenerated(false) }

        maybeCreate("cheatClient").apply {
            inherit(clientRun.get())
            name("Cheat Client")
            runDir("run/")
            source(mainSourceSet)
            ideConfigGenerated(false)
        }

        maybeCreate("cheatClientPreprocessed").apply {
            inherit(clientRun.get())
            name("Cheat Client (Preprocessed)")
            runDir("run/")
            source(cheatSourceSet)
            ideConfigGenerated(false)
        }

        maybeCreate("legitClientPreprocessed").apply {
            inherit(clientRun.get())
            name("Legit Client (Preprocessed)")
            runDir("run/")
            source(legitSourceSet)
            ideConfigGenerated(false)
        }
    }
}

tasks.named("build") {
    dependsOn(remapJarCheat, tasks.named("remapJar"))
}

apply(file("preprocess.gradle.kts"))
