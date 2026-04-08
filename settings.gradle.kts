pluginManagement {
    val loomVersion = providers.gradleProperty("loom_version").get()

    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }

        maven {
            name = "JitPack"
            url = uri("https://jitpack.io")
        }
    }

    plugins {
        id("net.fabricmc.fabric-loom-remap") version loomVersion
    }
}