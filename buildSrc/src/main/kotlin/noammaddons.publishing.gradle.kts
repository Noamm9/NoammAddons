import org.gradle.jvm.tasks.Jar

plugins {
    `maven-publish`
}

val mod_name: String = project.property("mod_name") as String

publishing {
    publications {
        register<MavenPublication>("mavenLegit") {
            artifactId = mod_name
            from(components["java"])
            val legitSourcesJar = tasks.named<Jar>("legitSourcesJar")
            artifact(legitSourcesJar) {
                classifier = "legit-sources"
            }
        }

        register<MavenPublication>("mavenCheat") {
            artifactId = mod_name
            from(components["cheat"])
            val cheatSourcesJar = tasks.named<Jar>("cheatSourcesJar")
            artifact(cheatSourcesJar) {
                classifier = "cheat-sources"
            }
        }
    }
}