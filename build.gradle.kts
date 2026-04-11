import net.fabricmc.loom.configuration.ide.RunConfigSettings
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.jvm.tasks.Jar
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
val archives_base_name: String by project
val mod_name: String by project
val fabric_version: String by project
val iris_version: String by project

fun SourceSet.kotlin(action: SourceDirectorySet.() -> Unit) {
    (extensions.getByName("kotlin") as SourceDirectorySet).action()
}

version = mod_version
group = maven_group

base {
    archivesName.set(archives_base_name)
}

repositories {
    maven(url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1"))
    maven(url = uri("https://maven.parchmentmc.org"))
    maven(url = uri("https://api.modrinth.com/maven"))
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    mappings(
        loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-1.21.10:2025.10.12@zip")
        }
    )

    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modCompileOnly("maven.modrinth:iris:$iris_version")

    implementation("io.github.classgraph:classgraph:4.8.174")
    include("io.github.classgraph:classgraph:4.8.174")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.4.1")

    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    include("org.java-websocket:Java-WebSocket:1.5.4")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val intermediateJarsDir = layout.buildDirectory.dir("tmp/intermediateJars")
val libsDir = layout.buildDirectory.dir("libs")

tasks.named<Jar>("jar") {
    destinationDirectory.set(intermediateJarsDir)

    from("LICENSE") {
        rename { "${it}_$archives_base_name" }
    }
}

val sourceSets = the<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main").get()

val cheatJavaDir = layout.buildDirectory.dir("preprocessed/cheat/java")
val cheatKotlinDir = layout.buildDirectory.dir("preprocessed/cheat/kotlin")
val legitJavaDir = layout.buildDirectory.dir("preprocessed/legit/java")
val legitKotlinDir = layout.buildDirectory.dir("preprocessed/legit/kotlin")

fun SourceSet.configurePreprocessedVariant(
    javaDir: Provider<Directory>,
    kotlinDir: Provider<Directory>,
) {
    java.setSrcDirs(listOf(javaDir.get().asFile))
    kotlin {
        setSrcDirs(listOf(kotlinDir.get().asFile))
    }
    resources.setSrcDirs(listOf("src/main/resources"))
    compileClasspath += mainSourceSet.compileClasspath
    runtimeClasspath += mainSourceSet.runtimeClasspath - mainSourceSet.output
}

fun NamedDomainObjectContainer<RunConfigSettings>.configureRunConfig(
    configName: String,
    configure: RunConfigSettings.() -> Unit,
): RunConfigSettings = maybeCreate(configName).apply(configure)

val cheatSourceSet: SourceSet = sourceSets.create("cheat").apply {
    configurePreprocessedVariant(cheatJavaDir, cheatKotlinDir)
}

val legitSourceSet: SourceSet = sourceSets.create("legit").apply {
    configurePreprocessedVariant(legitJavaDir, legitKotlinDir)
}

tasks.named<KotlinCompile>("compileCheatKotlin") {
    dependsOn("preprocessCheat")
    inputs.dir("src/main/kotlin")
    inputs.dir(cheatKotlinDir)

    compilerOptions {
        freeCompilerArgs.add("-Xjava-source-roots=${cheatJavaDir.get().asFile.invariantSeparatorsPath}")
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        freeCompilerArgs.add("-Xjava-source-roots=${file("src/main/java").invariantSeparatorsPath}")
    }
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

    compilerOptions {
        freeCompilerArgs.add("-Xjava-source-roots=${legitJavaDir.get().asFile.invariantSeparatorsPath}")
    }
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

    from("LICENSE") {
        rename { "${it}_$archives_base_name" }
    }
}

val jarLegit = tasks.register<Jar>("jarLegit") {
    dependsOn("legitClasses", "processLegitResources")
    from(legitSourceSet.output)
    archiveClassifier.set("legit")
    destinationDirectory.set(intermediateJarsDir)

    from("LICENSE") {
        rename { "${it}_$archives_base_name" }
    }
}

val remapJarCheat = tasks.register<RemapJarTask>("remapJarCheat") {
    dependsOn(jarCheat)
    inputFile.set(jarCheat.flatMap { it.archiveFile })
    archiveFileName.set("$mod_name - ${project.version}-cheat.jar")
    destinationDirectory.set(libsDir)
    classpath.from(cheatSourceSet.runtimeClasspath)
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(jarLegit)
    inputFile.set(jarLegit.flatMap { it.archiveFile })
    archiveFileName.set("$mod_name - ${project.version}-legit.jar")
    destinationDirectory.set(libsDir)
    classpath.from(legitSourceSet.runtimeClasspath)
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("mavenLegit") {
            artifactId = "legit"
            artifact(tasks.named("remapJar"))
            from(components["java"])
        }

        create<MavenPublication>("mavenCheat") {
            artifactId = "cheat"
            artifact(remapJarCheat)
            from(components["java"])
        }
    }
}

loom {
    runs {
        val clientRun = named("client")
        val serverRun = named("server")

        clientRun.configure { ideConfigGenerated(false) }
        serverRun.configure { ideConfigGenerated(false) }

        configureRunConfig("cheatClient") {
            inherit(clientRun.get())
            name("Cheat Client")
            runDir("run/")
            source(mainSourceSet)
        }

        configureRunConfig("cheatClientPreprocessed") {
            inherit(clientRun.get())
            name("Cheat Client (Preprocessed)")
            runDir("run/")
            source(cheatSourceSet)
        }

        configureRunConfig("legitClientPreprocessed") {
            inherit(clientRun.get())
            name("Legit Client (Preprocessed)")
            runDir("run/")
            source(legitSourceSet)
        }
    }
}

tasks.named("build") {
    dependsOn(remapJarCheat, tasks.named("remapJar"))
}

apply(from = file("preprocess.gradle.kts"))
