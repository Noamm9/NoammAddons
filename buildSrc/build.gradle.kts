plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:fabric-loom:1.17.19")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.4.10")
}