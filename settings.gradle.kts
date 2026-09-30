rootProject.name = "NoammAddons"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
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
        versions(
            "26.1.2-cheat" to "26.1.2",
            "26.1.2-legit" to "26.1.2",
            "26.2-cheat" to "26.2",
            "26.2-legit" to "26.2",
            "26.3-cheat" to "26.3",
            "26.3-legit" to "26.3",
        )
        vcsVersion = "26.3-cheat"
    }
}