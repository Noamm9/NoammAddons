plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:fabric-loom:1.14.10")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.0")
}