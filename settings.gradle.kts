pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://jitpack.io") { name = "JitPack" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9-beta.1"
}

stonecutter {
    create(rootProject) {
        versions("1.21.10", /* "1.21.11" */) // Uncomment to enable 1.21.11 support
        vcsVersion = "1.21.10"
    }
}

rootProject.name = "NoamAddons"