import java.util.Properties

plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

val versions = Properties().apply {
    file("../gradle.properties").inputStream().use { load(it) }
}

val loom_version = versions.getProperty("loom_version")
val kotlin_version = versions.getProperty("kotlin_version")
val shadow_version = versions.getProperty("shadow_version")

dependencies {
    implementation("net.fabricmc:fabric-loom:$loom_version")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlin_version")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:$shadow_version")
}