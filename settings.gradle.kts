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
        id("dev.kikugie.stonecutter") version providers.gradleProperty("stonecutter_version").get()
    }
}

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        versions("26.2", "26.3")
        vcsVersion = "26.3"
    }
}
