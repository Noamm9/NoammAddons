package com.github.noamm9.utils

import com.github.noamm9.utils.ChatUtils.addColor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import kotlin.text.forEach

private val SPACE_CAPS_REGEX_1 = Regex("(?<=[a-z])(?=[A-Z])")
private val SPACE_CAPS_REGEX_2 = Regex("(?<=[A-Z])(?=[A-Z][a-z])")

/**
 * Returns true if this string starts with any of the specified [prefixes].
 */
fun String.startsWithOneOf(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }

/**
 * Returns true if this string ends with any of the specified [suffixes].
 */
fun String.endsWithOneOf(vararg suffixes: String): Boolean = suffixes.any { endsWith(it) }

/**
 * Returns true if this object is equal to any of the specified [others].
 *
 * @param others The values to compare against the receiver.
 */
fun <T> T.equalsOneOf(vararg others: T): Boolean = others.contains(this)

/**
 * Returns true if this string contains any of the specified [elements].
 */
fun String.containsOneOf(vararg elements: CharSequence): Boolean = elements.any { contains(it) }

/**
 * Returns true if this collection contains any of the specified [elements].
 */
fun <T> Collection<T>.containsOneOf(vararg elements: T): Boolean = elements.any { contains(it) }

/**
 * Returns true if this string contains all the specified [elements].
 */
fun String.containsAll(vararg elements: String): Boolean = elements.all { contains(it) }

/**
 * Inserts spaces between words in a CamelCase or PascalCase string.
 * Example: "MySuperCoolFeature" -> "My Super Cool Feature"
 */
fun String.spaceCaps(): String = replace(SPACE_CAPS_REGEX_1, " ").replace(SPACE_CAPS_REGEX_2, " ").trim()

/**
 * Capitalizes the first character of this string.
 * Safe to use on empty strings.
 */
fun String.uppercaseFirst(): String = replaceFirstChar { it.uppercase() }

/**
 * Removes all occurrences of the specified [patterns] from this string.
 *
 * @param patterns String literals to be replaced with an empty string.
 */
fun String.remove(vararg patterns: String): String = patterns.fold(this) { acc, s -> acc.replace(s, "") }

/**
 * Removes all matches of the specified [patterns] from this string.
 *
 * @param patterns Regular expressions to be replaced with an empty string.
 */
fun String.remove(vararg patterns: Regex): String = patterns.fold(this) { acc, r -> acc.replace(r, "") }

inline fun <T> catch(block: () -> T) = try {
    block()
}
catch (_: Throwable) {
    null
}

/**
 * Converts this string to a [MutableComponent], replacing specified placeholder strings
 * with their corresponding [Component]s and applying color formatting to text segments.
 *
 * @param replacements A map of placeholder strings to the [Component]s that should replace them.
 * @return A [MutableComponent] combining the formatted text literals and injected components.
 */
fun String.formatComponents(replacements: Map<String, Component>): MutableComponent {
    // replace placeholders with special chars to set up for component building
    var tokenizedTitle = this
    val componentMap = mutableMapOf<Char, Component>()

    replacements.entries.forEachIndexed { index, (placeholder, component) ->
        val token = ('\u0001' + index)
        tokenizedTitle = tokenizedTitle.replace(placeholder, token.toString())
        componentMap[token] = component
    }
    val result = Component.empty()
    val currentText = StringBuilder()

    // build the output component, replacing each placeholder char with the respective component
    tokenizedTitle.forEach { char ->
        if (componentMap.containsKey(char)) {
            if (currentText.isNotEmpty()) {
                result.append(Component.literal(currentText.toString().addColor()))
                currentText.clear()
            }
            result.append(componentMap[char]!!)
        } else {
            currentText.append(char)
        }
    }

    if (currentText.isNotEmpty()) {
        result.append(Component.literal(currentText.toString().addColor()))
    }

    return result
}