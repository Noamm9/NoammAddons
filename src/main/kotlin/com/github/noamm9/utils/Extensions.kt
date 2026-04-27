package com.github.noamm9.utils

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
 * Returns true if this array contains any of the specified [elements].
 */
fun <T> Array<T>.containsOneOf(vararg elements: T): Boolean = elements.any { contains(it) }

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

/**
 * Casts an object safely to type [T] and runs the [block] if the cast succeeded.
 *
 * @param block The code to execute with the casted object.
 * @return The casted object of type [T], or null if the receiver is not an instance of [T].
 */
inline fun <reified T> Any?.asType(block: (T) -> Unit): T? {
    if (this is T) {
        block(this)
        return this
    }
    return null
}

/**
 * Performs a safe cast of the receiver to type [T].
 * Equivalent to `this as? T`.
 */
inline fun <reified T> Any?.cast(): T? = this as? T

/**
 * Executes the given [block] on the receiver if the [condition] is true.
 * Returns the receiver to allow for further chaining.
 *
 * @param condition The boolean check to determine if the block should run.
 * @param block The lambda to execute on the receiver.
 */
inline fun <T> T.runIf(condition: Boolean, block: T.() -> Unit): T {
    if (condition) block()
    return this
}

/**
 * Executes the given [block] on the receiver unless the [condition] is true.
 * Returns the receiver to allow for further chaining.
 *
 * @param condition If true, the block will NOT be executed.
 * @param block The lambda to execute on the receiver.
 */
inline fun <T> T.runUnless(condition: Boolean, block: T.() -> Unit): T {
    if (! condition) block()
    return this
}