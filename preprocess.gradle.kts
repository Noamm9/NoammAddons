import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

fun Project.preprocessSources(variantName: String, outRoot: File) {
    val kotlinIn = file("src/main/kotlin")
    val javaIn = file("src/main/java")
    val kotlinOut = File(outRoot, "kotlin")
    val javaOut = File(outRoot, "java")

    fun processTree(inputRoot: File, outputRoot: File) {
        if (! inputRoot.exists()) return

        project.fileTree(inputRoot).matching { include("**/*.kt", "**/*.java") }.files.forEach { sourceFile ->
            val relativePath = inputRoot.toPath().relativize(sourceFile.toPath()).toString()
            val outFile = File(outputRoot, relativePath)
            outFile.parentFile.mkdirs()

            val stack = mutableListOf<Boolean>()
            val lines = mutableListOf<String>()
            var include = true

            sourceFile.readLines().forEachIndexed { index, line ->
                val lineNumber = index + 1
                val trimmed = line.trim()
                val directive = trimmed.replaceFirst(Regex("^//\\s*"), "")

                when (directive) {
                    "#if CHEAT" -> {
                        stack += include
                        include = variantName == "cheat" && include
                    }

                    "#if LEGIT" -> {
                        stack += include
                        include = variantName != "cheat" && include
                    }

                    "#else" -> {
                        if (stack.isEmpty()) throw GradleException("Unmatched #else in $sourceFile at line $lineNumber")

                        val previous = stack.last()
                        include = variantName != "cheat" && previous
                    }

                    "#endif" -> {
                        if (stack.isEmpty()) throw GradleException("Unmatched #endif in $sourceFile at line $lineNumber")
                        include = stack.removeAt(stack.lastIndex)
                    }

                    else -> {
                        if (include) {
                            val marker = "//\$\$"
                            val markerIndex = line.indexOf(marker)

                            if (markerIndex >= 0) lines += line.substring(0, markerIndex) + line.substring(markerIndex + marker.length)
                            else lines += line
                        }
                    }
                }
            }

            if (stack.isNotEmpty()) throw GradleException("Unclosed #if in $sourceFile")
            outFile.writeText(lines.joinToString(System.lineSeparator()))
        }
    }

    processTree(kotlinIn, kotlinOut)
    processTree(javaIn, javaOut)
}

fun Project.preprocessResources(variantName: String, outRoot: File) {
    val resourcesIn = file("src/main/resources")
    if (! resourcesIn.exists()) return

    fun preprocessConditionalFile(srcFile: File, outFile: File, expandVersion: Boolean) {
        val stack = mutableListOf<Boolean>()
        val lines = mutableListOf<String>()
        var include = true

        srcFile.readLines().forEachIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()
            val directive = trimmed.replaceFirst(Regex("^//\\s*"), "")

            when (directive) {
                "#if CHEAT" -> {
                    stack += include
                    include = variantName == "cheat" && include
                }

                "#if LEGIT" -> {
                    stack += include
                    include = variantName != "cheat" && include
                }

                "#else" -> {
                    if (stack.isEmpty()) throw GradleException("Unmatched #else in $srcFile at line $lineNumber")

                    val previous = stack.last()
                    include = variantName != "cheat" && previous
                }

                "#endif" -> {
                    if (stack.isEmpty()) throw GradleException("Unmatched #endif in $srcFile at line $lineNumber")
                    include = stack.removeAt(stack.lastIndex)
                }

                else -> {
                    if (! include) return@forEachIndexed

                    val marker = "//\$"
                    val trimmedLine = line.trim()

                    if (trimmedLine.startsWith("//") && ! trimmedLine.startsWith(marker)) return@forEachIndexed

                    val markerIndex = line.indexOf(marker)

                    if (markerIndex >= 0) lines += line.substring(0, markerIndex) + line.substring(markerIndex + marker.length)
                    else lines += line
                }
            }
        }

        if (stack.isNotEmpty()) throw GradleException("Unclosed #if in $srcFile")

        var content = lines.joinToString(System.lineSeparator())
        if (expandVersion) content = content.replace("\${version}", project.version.toString())
        outFile.writeText(content)
    }

    val fabricSrc = File(resourcesIn, "fabric.mod.json5")

    if (fabricSrc.exists()) {
        val fabricOut = File(outRoot, "fabric.mod.json")
        fabricOut.parentFile.mkdirs()
        preprocessConditionalFile(fabricSrc, fabricOut, true)
    }

    val mixinsSrc = File(resourcesIn, "noammaddons.mixins.json5")

    if (mixinsSrc.exists()) {
        val mixinsOut = File(outRoot, "noammaddons.mixins.json")
        mixinsOut.parentFile.mkdirs()
        preprocessConditionalFile(mixinsSrc, mixinsOut, false)
    }
}

val preprocessLegit = tasks.register("preprocessLegit") {
    val outRoot = layout.buildDirectory.dir("preprocessed/legit")
    val resourcesOut = layout.buildDirectory.dir("preprocessed/legit/resources")

    inputs.dir("src/main/kotlin")
    inputs.dir("src/main/java")
    inputs.file("src/main/resources/fabric.mod.json5")
    inputs.file("src/main/resources/noammaddons.mixins.json5")
    outputs.dir(outRoot)
    outputs.dir(resourcesOut)

    doFirst {
        delete(outRoot.get().asFile)
    }

    doLast {
        project.preprocessSources("legit", outRoot.get().asFile)
        project.preprocessResources("legit", resourcesOut.get().asFile)
    }
}

val preprocessCheat = tasks.register("preprocessCheat") {
    val outRoot = layout.buildDirectory.dir("preprocessed/cheat")
    val resourcesOut = layout.buildDirectory.dir("preprocessed/cheat/resources")

    inputs.dir("src/main/kotlin")
    inputs.dir("src/main/java")
    inputs.file("src/main/resources/fabric.mod.json5")
    inputs.file("src/main/resources/noammaddons.mixins.json5")
    outputs.dir(outRoot)
    outputs.dir(resourcesOut)

    doFirst {
        delete(outRoot.get().asFile)
    }

    doLast {
        project.preprocessSources("cheat", outRoot.get().asFile)
        project.preprocessResources("cheat", resourcesOut.get().asFile)
    }
}

fun configurePreprocessedResourcesTask(taskName: String, variantName: String, preprocessTask: org.gradle.api.tasks.TaskProvider<*>) {
    tasks.named<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5")
        exclude("noammaddons.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json")
            include("noammaddons.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
}

configurePreprocessedResourcesTask("processResources", "legit", preprocessLegit)
configurePreprocessedResourcesTask("processLegitResources", "legit", preprocessLegit)
configurePreprocessedResourcesTask("processCheatResources", "cheat", preprocessCheat)