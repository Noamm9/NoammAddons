import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom")
}

configure<LoomGradleExtensionAPI> {
    val runFolder = rootProject.file("run/")

    accessWidenerPath.set(rootProject.file("src/main/resources/${findProperty("mod_id")}.accesswidener"))

    runs {
        val clientRun = named("client")
        val serverRun = named("server")

        clientRun.configure {
            runDirectory.set(runFolder)
            generateRunConfig.set(false)
        }

        serverRun.configure {
            runDirectory.set(runFolder)
            generateRunConfig.set(false)
        }

        val sourceSets = the<SourceSetContainer>()
        val mainSourceSet = sourceSets.named("main").get()
        val cheatSourceSet = sourceSets.named("cheat").get()
        val legitSourceSet = sourceSets.named("legit").get()

        maybeCreate("cheatClient").apply {
            inherit(clientRun.get())
            displayName.set("Cheat Client")
            runDirectory.set(runFolder)
            sourceSet.set(mainSourceSet.name)
            generateRunConfig.set(false)
        }

        maybeCreate("cheatClientPreprocessed").apply {
            inherit(clientRun.get())
            displayName.set("Cheat Client (Preprocessed)")
            runDirectory.set(runFolder)
            sourceSet.set(cheatSourceSet.name)
            generateRunConfig.set(false)
        }

        maybeCreate("legitClientPreprocessed").apply {
            inherit(clientRun.get())
            displayName.set("Legit Client (Preprocessed)")
            runDirectory.set(runFolder)
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