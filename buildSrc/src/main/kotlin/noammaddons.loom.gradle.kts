import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

configure<LoomGradleExtensionAPI> {
    accessWidenerPath.set(file("src/main/resources/noammaddons.accesswidener"))
    runs {
        val clientRun = named("client")
        val serverRun = named("server")

        clientRun.configure { ideConfigGenerated(false) }
        serverRun.configure { ideConfigGenerated(false) }

        val sourceSets = the<SourceSetContainer>()
        val mainSourceSet = sourceSets.named("main").get()
        val cheatSourceSet = sourceSets.named("cheat").get()
        val legitSourceSet = sourceSets.named("legit").get()

        maybeCreate("cheatClient").apply {
            inherit(clientRun.get())
            name("Cheat Client")
            runDir("run/")
            source(mainSourceSet)
            ideConfigGenerated(false)
        }

        maybeCreate("cheatClientPreprocessed").apply {
            inherit(clientRun.get())
            name("Cheat Client (Preprocessed)")
            runDir("run/")
            source(cheatSourceSet)
            ideConfigGenerated(false)
        }

        maybeCreate("legitClientPreprocessed").apply {
            inherit(clientRun.get())
            name("Legit Client (Preprocessed)")
            runDir("run/")
            source(legitSourceSet)
            ideConfigGenerated(false)
        }
    }

    afterEvaluate {
        val mixinAgentJar = configurations.runtimeClasspath.get().files.firstOrNull { file ->
            file.name.startsWith("sponge-mixin-") && file.extension == "jar"
        }?.absolutePath

        runs.configureEach {
            vmArg("-XX:+AllowEnhancedClassRedefinition")
            vmArg("-Dmixin.hotSwap=true")

            if (mixinAgentJar != null) {
                vmArg("-javaagent:$mixinAgentJar")
            }
        }
    }
}