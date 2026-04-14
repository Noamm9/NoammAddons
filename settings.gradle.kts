rootProject.name = "NoammAddons"

pluginManagement {
    val loomVersion = providers.gradleProperty("loom_version").getOrElse("1.15-SNAPSHOT")

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }

    plugins {
        id("net.fabricmc.fabric-loom-remap") version loomVersion
    }
}