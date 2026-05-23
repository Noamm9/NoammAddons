plugins {
    `maven-publish`
}

val mod_name: String by project

publishing {
    publications {
        register<MavenPublication>("mavenLegit") {
            artifactId = mod_name
            from(components["java"])
            val remapLegitSourcesJar by tasks.existing
            artifact(remapLegitSourcesJar) {
                classifier = "legit-sources"
            }
        }

        register<MavenPublication>("mavenCheat") {
            artifactId = mod_name
            from(components["cheat"])
            val remapCheatSourcesJar by tasks.existing
            artifact(remapCheatSourcesJar) {
                classifier = "cheat-sources"
            }
        }
    }
}