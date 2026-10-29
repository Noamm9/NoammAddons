rootProject.name = "NoammAddons"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
    }

    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version").get()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        versions("26.2", "26.3")
        vcsVersion = "26.3"
    }
}
