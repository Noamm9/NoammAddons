import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom")
}

configure<LoomGradleExtensionAPI> {
    accessWidenerPath.set(file("src/main/resources/noammaddons.accesswidener"))
    runs {
        val clientRun = named("client")
        val serverRun = named("server")

        clientRun.configure { generateRunConfig.set(false) }
        serverRun.configure { generateRunConfig.set(false) }

        val sourceSets = the<SourceSetContainer>()
        val mainSourceSet = sourceSets.named("main").get()
        val cheatSourceSet = sourceSets.named("cheat").get()
        val legitSourceSet = sourceSets.named("legit").get()

        maybeCreate("cheatClient").apply {
            inherit(clientRun.get())
            displayName.set("Cheat Client")
            runDirectory.set(file("run/"))
            sourceSet.set(mainSourceSet.name)
            generateRunConfig.set(false)
        }

        maybeCreate("cheatClientPreprocessed").apply {
            inherit(clientRun.get())
            displayName.set("Cheat Client (Preprocessed)")
            runDirectory.set(file("run/"))
            sourceSet.set(cheatSourceSet.name)
            generateRunConfig.set(false)
        }

        maybeCreate("legitClientPreprocessed").apply {
            inherit(clientRun.get())
            displayName.set("Legit Client (Preprocessed)")
            runDirectory.set(file("run/"))
            sourceSet.set(legitSourceSet.name)
            generateRunConfig.set(false)
        }
    }

    afterEvaluate {
        val mixinAgentJar = configurations.runtimeClasspath.get().files.firstOrNull { file ->
            file.name.startsWith("sponge-mixin-") && file.extension == "jar"
        }?.absolutePath

        runs.configureEach {
            jvmArguments.add("-XX:+AllowEnhancedClassRedefinition")
            jvmArguments.add("-Dmixin.hotSwap=true")

            if (mixinAgentJar != null) {
                jvmArguments.add("-javaagent:$mixinAgentJar")
            }
        }
    }
}