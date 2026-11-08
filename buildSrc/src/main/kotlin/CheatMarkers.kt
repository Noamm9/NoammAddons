import java.io.File

/*
 * Keeps the ClickGui cheat markers (`Feature(..., cheat = true)` / `Setting(...).cheat()`) in sync with
 * the `//#if CHEAT` preprocessor blocks that decide what actually ships in the cheat build.
 *
 * Rules (Kotlin sources only, comment lines are ignored):
 *  1. A Feature declared inside a cheat-only branch must pass `cheat = true`.
 *  2. A delegated setting (`val x by SomeSetting(...)`) inside a cheat-only branch must call `.cheat()`,
 *     unless it belongs to a Feature that is itself declared inside a cheat-only branch
 *     (the feature row and window title already carry the marker).
 *  3. Neither marker may appear outside a cheat-only branch, or the legit build would show cheat warnings.
 */

private val directivePrefix = Regex("^//\\s*")
private val topLevelTypeRegex = Regex("^(?:(?:open|abstract|data|sealed|private|internal)\\s+)*(?:object|class)\\s+(\\w+)")
private val featureSuperRegex = Regex("^(?:(?:open|abstract|data|sealed|private|internal)\\s+)*(?:object|class)\\s+\\w+[^:]*:\\s*Feature\\s*\\(")
private val settingDeclRegex = Regex("\\bby\\s+\\w*Setting\\s*[(<]")
private val featureTagRegex = Regex("\\bcheat\\s*=\\s*true\\b")
private val stringLiteralRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"")

private data class CheatFrame(val parentCheat: Boolean, val ifIsCheat: Boolean)

/** Returns one human-readable message per violation, formatted as `file:line: message`. */
internal fun findCheatMarkerViolations(fileName: String, lines: List<String>): List<String> {
    val violations = mutableListOf<String>()
    val stack = ArrayDeque<CheatFrame>()
    var inCheat = false
    var ownerIsCheatFeature = false

    fun report(index: Int, message: String) {
        violations += "$fileName:${index + 1}: $message"
    }

    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()

        when (trimmed.replaceFirst(directivePrefix, "").takeIf { trimmed.startsWith("//") }) {
            "#if CHEAT", "#if LEGIT" -> {
                val frame = CheatFrame(parentCheat = inCheat, ifIsCheat = trimmed.endsWith("CHEAT"))
                stack.addLast(frame)
                inCheat = frame.parentCheat || frame.ifIsCheat
                return@forEachIndexed
            }

            "#else" -> {
                val frame = stack.lastOrNull() ?: return@forEachIndexed
                inCheat = frame.parentCheat || ! frame.ifIsCheat
                return@forEachIndexed
            }

            "#endif" -> {
                val frame = stack.removeLastOrNull() ?: return@forEachIndexed
                inCheat = frame.parentCheat
                return@forEachIndexed
            }
        }

        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) return@forEachIndexed

        if (topLevelTypeRegex.containsMatchIn(line) && ! line.first().isWhitespace()) {
            val isFeature = featureSuperRegex.containsMatchIn(line)
            ownerIsCheatFeature = isFeature && inCheat

            if (isFeature) {
                val declaration = gatherFeatureDeclaration(lines, index)
                val tagged = featureTagRegex.containsMatchIn(declaration)
                val name = topLevelTypeRegex.find(line) !!.groupValues[1]
                if (inCheat && ! tagged) report(index, "Feature '$name' is cheat-only (#if CHEAT) but is missing `cheat = true`.")
                if (! inCheat && tagged) report(index, "Feature '$name' has `cheat = true` but is not inside an #if CHEAT block.")
            }
            return@forEachIndexed
        }

        if (settingDeclRegex.containsMatchIn(line)) {
            val tagged = gatherStatement(lines, index).contains(".cheat()")
            if (inCheat && ! tagged && ! ownerIsCheatFeature) report(index, "Setting is cheat-only (#if CHEAT) but is missing `.cheat()`.")
            if (! inCheat && tagged) report(index, "Setting has `.cheat()` but is not inside an #if CHEAT block.")
        }
    }

    return violations
}

/** The text of a Feature declaration header, up to and including the line that opens its body. */
private fun gatherFeatureDeclaration(lines: List<String>, start: Int): String = buildString {
    for (i in start until lines.size) {
        appendLine(lines[i])
        if (lines[i].trimEnd().endsWith("{")) break
    }
}

/** The text of a (possibly multi-line) setting declaration, following chained `.calls()` on later lines. */
private fun gatherStatement(lines: List<String>, start: Int): String = buildString {
    var depth = 0
    var i = start
    while (i < lines.size) {
        val line = lines[i]
        appendLine(line)
        line.replace(stringLiteralRegex, "\"\"").forEach {
            when (it) {
                '(', '[', '{' -> depth ++
                ')', ']', '}' -> depth --
            }
        }
        val nextStartsChain = lines.getOrNull(i + 1)?.trimStart()?.startsWith(".") == true
        if (depth <= 0 && ! nextStartsChain) break
        i ++
    }
}

internal fun findCheatMarkerViolations(root: File): List<String> {
    if (! root.exists()) return emptyList()
    return root.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .sortedBy { it.invariantSeparatorsPath }
        .flatMap { findCheatMarkerViolations(it.relativeTo(root).invariantSeparatorsPath, it.readLines()) }
        .toList()
}
