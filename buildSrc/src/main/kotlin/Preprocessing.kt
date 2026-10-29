import org.gradle.api.*
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.the
import java.io.File
import java.util.*

private val directiveRegex = "^//\\s*".toRegex()
private val placeholderRegex = Regex("@(\\w+)@")
private const val marker = "//!"

private data class StackFrame(val outerInclude: Boolean, val branchWasTrue: Boolean)

private fun Project.loadGradleProperties(): Map<String, String> {
    val props = Properties()
    listOf(rootProject.file("gradle.properties"), file("gradle.properties")).filter(File::exists).forEach { propsFile ->
        propsFile.inputStream().use(props::load)
    }
    return props.mapKeys { it.key as String }.mapValues { it.value as String }
}

val Project.sourceRoot: File
    get() {
        val mainSourceSet = the<SourceSetContainer>().named("main").get()
        val javaDir = mainSourceSet.java.srcDirs.firstOrNull { it != file("src/main/java") }
            ?: return layout.buildDirectory.dir("generated/stonecutter/main").get().asFile
        return javaDir.parentFile
    }

fun Task.dependsOnStonecutter() {
    if (project.tasks.findByName("stonecutterGenerate") != null) dependsOn("stonecutterGenerate")
}

private fun replaceProperties(content: String, properties: Map<String, String>): String {
    return placeholderRegex.replace(content) { match ->
        properties[match.groupValues[1].lowercase()] ?: match.value
    }
}

private fun processDirectives(sourceFile: File, variantName: String, stripLineComments: Boolean = false): List<String> {
    val isCheat = variantName == "cheat"
    val stack = mutableListOf<StackFrame>()
    val output = mutableListOf<String>()
    var include = true

    sourceFile.readLines().forEachIndexed { index, line ->
        val lineNumber = index + 1
        val directive = line.trim().replaceFirst(directiveRegex, "")

        when (directive) {
            "#if CHEAT" -> {
                stack += StackFrame(outerInclude = include, branchWasTrue = isCheat)
                include = isCheat && include
            }

            "#if LEGIT" -> {
                stack += StackFrame(outerInclude = include, branchWasTrue = ! isCheat)
                include = ! isCheat && include
            }

            "#else" -> {
                if (stack.isEmpty()) throw GradleException("Unmatched #else in $sourceFile at line $lineNumber")
                val frame = stack.last()
                include = ! frame.branchWasTrue && frame.outerInclude
            }

            "#endif" -> {
                if (stack.isEmpty()) throw GradleException("Unmatched #endif in $sourceFile at line $lineNumber")
                include = stack.removeAt(stack.lastIndex).outerInclude
            }

            else -> if (include) {
                val trimmed = line.trim()
                if (stripLineComments && trimmed.startsWith("//") && ! trimmed.startsWith(marker)) return@forEachIndexed

                val markerIndex = line.indexOf(marker)
                output += if (markerIndex >= 0) line.removeRange(markerIndex, markerIndex + marker.length) else line
            }
        }
    }

    if (stack.isNotEmpty()) throw GradleException("Unclosed #if in $sourceFile")
    return output
}

private fun Project.preprocessSources(variantName: String, outRoot: File) {
    val properties = loadGradleProperties()

    fun processTree(inputRoot: File, outputRoot: File) {
        if (! inputRoot.exists()) return

        fileTree(inputRoot).matching { include("**/*.kt", "**/*.java") }.files.forEach { sourceFile ->
            val outFile = File(outputRoot, inputRoot.toPath().relativize(sourceFile.toPath()).toString())
            outFile.parentFile.mkdirs()
            val content = processDirectives(sourceFile, variantName).joinToString(System.lineSeparator())
            outFile.writeText(replaceProperties(content, properties))
        }
    }

    processTree(File(sourceRoot, "kotlin"), File(outRoot, "kotlin"))
    processTree(File(sourceRoot, "java"), File(outRoot, "java"))
}

private fun Project.replaceSourceProperties(outRoot: File) {
    val properties = loadGradleProperties()

    fun processTree(inputRoot: File, outputRoot: File) {
        if (! inputRoot.exists()) return

        fileTree(inputRoot).matching { include("**/*.kt", "**/*.java") }.files.forEach { sourceFile ->
            val outFile = File(outputRoot, inputRoot.toPath().relativize(sourceFile.toPath()).toString())
            outFile.parentFile.mkdirs()
            outFile.writeText(replaceProperties(sourceFile.readText(), properties))
        }
    }

    processTree(File(sourceRoot, "kotlin"), File(outRoot, "kotlin"))
    processTree(File(sourceRoot, "java"), File(outRoot, "java"))
}

private fun Project.preprocessResources(variantName: String, outRoot: File) {
    val resourcesIn = File(sourceRoot, "resources")
    if (! resourcesIn.exists()) return

    val properties = loadGradleProperties()
    val modId = findProperty("mod_id")

    fun preprocessFile(srcName: String, outName: String, expandVersion: Boolean) {
        val srcFile = File(resourcesIn, srcName)
        if (! srcFile.exists()) return

        val outFile = File(outRoot, outName).also { it.parentFile.mkdirs() }
        var content = processDirectives(srcFile, variantName, stripLineComments = true).joinToString(System.lineSeparator())
        if (expandVersion) content = content.replace("\${version}", project.version.toString())
        content = replaceProperties(content, properties)
        outFile.writeText(content)
    }

    preprocessFile("fabric.mod.json5", "fabric.mod.json", expandVersion = true)
    preprocessFile("$modId.mixins.json5", "$modId.mixins.json", expandVersion = false)
}

fun Project.registerPreprocessTask(variantName: String): TaskProvider<Task> {
    val capitalised = variantName.replaceFirstChar { it.uppercase() }
    return tasks.register("preprocess$capitalised") {
        val outRoot = layout.buildDirectory.dir("preprocessed/$variantName")
        val resourcesOut = layout.buildDirectory.dir("preprocessed/$variantName/resources")

        dependsOnStonecutter()
        inputs.dir(File(sourceRoot, "kotlin"))
        inputs.dir(File(sourceRoot, "java"))
        inputs.file(File(sourceRoot, "resources/fabric.mod.json5"))
        inputs.file(File(sourceRoot, "resources/${findProperty("mod_id")}.mixins.json5"))
        outputs.dir(outRoot)
        outputs.dir(resourcesOut)

        doFirst { delete(outRoot.get().asFile) }
        doLast {
            preprocessSources(variantName, outRoot.get().asFile)
            preprocessResources(variantName, resourcesOut.get().asFile)
        }
    }
}

fun Project.registerPreprocessMainTask(): TaskProvider<Task> {
    return tasks.register("preprocessMain") {
        val outRoot = layout.buildDirectory.dir("preprocessed/main")

        dependsOnStonecutter()
        inputs.dir(File(sourceRoot, "kotlin"))
        inputs.dir(File(sourceRoot, "java"))
        outputs.dir(outRoot)

        doFirst { delete(outRoot.get().asFile) }
        doLast {
            replaceSourceProperties(outRoot.get().asFile)
        }
    }
}