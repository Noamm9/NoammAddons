plugins {
    `maven-publish`
}

val mod_name: String by project

publishing {
    publications {
        register<MavenPublication>("mavenLegit") {
            artifactId = mod_name
            from(components["java"])
            val legitSourcesJar by tasks.existing
            artifact(legitSourcesJar) {
                classifier = "legit-sources"
            }
        }

        register<MavenPublication>("mavenCheat") {
            artifactId = mod_name
            from(components["cheat"])
            val cheatSourcesJar by tasks.existing
            artifact(cheatSourcesJar) {
                classifier = "cheat-sources"
            }
        }
    }
}