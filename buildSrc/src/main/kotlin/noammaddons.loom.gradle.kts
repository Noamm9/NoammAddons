import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom")
}

configure<LoomGradleExtensionAPI> {
    accessWidenerPath.set(rootProject.file("src/main/resources/${findProperty("mod_id")}.accesswidener"))

    runs {
        val clientRun = named("client")
        val serverRun = named("server")

        clientRun.configure {
            runDirectory.set(rootProject.file("run/"))
            generateRunConfig.set(false)
        }

        serverRun.configure {
            runDirectory.set(rootProject.file("run/"))
            generateRunConfig.set(false)
        }

        val sourceSets = the<SourceSetContainer>()
        val mainSourceSet = sourceSets.named("main").get()
        val cheatSourceSet = sourceSets.named("cheat").get()
        val legitSourceSet = sourceSets.named("legit").get()

        maybeCreate("cheatClient").apply {
            inherit(clientRun.get())
            displayName.set("Cheat Client")
            runDirectory.set(rootProject.file("run/"))
            sourceSet.set(mainSourceSet.name)
            generateRunConfig.set(false)
        }

        maybeCreate("cheatClientPreprocessed").apply {
            inherit(clientRun.get())
            displayName.set("Cheat Client (Preprocessed)")
            runDirectory.set(rootProject.file("run/"))
            sourceSet.set(cheatSourceSet.name)
            generateRunConfig.set(false)
        }

        maybeCreate("legitClientPreprocessed").apply {
            inherit(clientRun.get())
            displayName.set("Legit Client (Preprocessed)")
            runDirectory.set(rootProject.file("run/"))
            sourceSet.set(legitSourceSet.name)
            generateRunConfig.set(false)
        }
    }

    afterEvaluate {
        runs.configureEach {
            jvmArguments.add("-XX:+AllowEnhancedClassRedefinition")
        }
    }
}