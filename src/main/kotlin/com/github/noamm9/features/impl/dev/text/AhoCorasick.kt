package com.github.noamm9.features.impl.dev.text

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import java.util.Optional
import kotlin.collections.ArrayDeque

/**
 * Taken from Starred's library
 * Under BSD 3-Clause License https://github.com/skies-starred/library/blob/master/LICENSE
 * https://github.com/skies-starred/library/blob/master/src/main/kotlin/xyz/aerii/library/handlers/minecraft/AbstractWords.kt
 * Modified by Noamm9
 */
abstract class AhoCorasick {
    private class Node {
        val goto = Int2ObjectOpenHashMap<Node>(4)
        var fail: Node? = null
        var output: Int = - 1
        var depth: Int = 0
    }

    private var root = Node()
    private var rootAscii = arrayOfNulls<Node>(128)
    private var ia = emptyArray<IntArray>()
    private var r0 = emptyArray<String>()
    private var r1 = emptyArray<Component>()
    private var r2 = emptyArray<FormattedCharSequence>()

    var skips: String? = null

    var map0 = HashMap<String, String>()
        private set

    var map1 = HashMap<String, Component>()
        private set

    var map2 = HashMap<String, FormattedCharSequence>()
        private set

    private var scChars = IntArray(128)
    private var scStyles = arrayOfNulls<Style>(128)
    private var scB = IntArray(128)
    private var scBs = arrayOfNulls<Style>(128)
    private var scStyleList = ArrayList<Style>(128)

    private fun ensureScratch(minSize: Int) {
        if (scChars.size >= minSize) return
        var newSize = scChars.size
        while (newSize < minSize) newSize *= 2
        scChars = scChars.copyOf(newSize)
        scStyles = scStyles.copyOf(newSize)
        scB = scB.copyOf(newSize)
        scBs = scBs.copyOf(newSize)
    }

    fun put(key: String, str: String, cmp: Component, seq: FormattedCharSequence) {
        map0[key] = str
        map1[key] = cmp
        map2[key] = seq
    }

    fun remove(key: String) {
        map0.remove(key)
        map1.remove(key)
        map2.remove(key)
    }

    fun build() {
        val keys = map0.keys.sortedByDescending { it.length }.toTypedArray()
        val n = keys.size

        if (n == 0) {
            root = Node()
            rootAscii = arrayOfNulls(128)
            ia = emptyArray()
            r0 = emptyArray()
            r1 = emptyArray()
            r2 = emptyArray()
            return
        }

        ia = Array(n) { keys[it].codePoints().toArray() }
        r0 = Array(n) { map0[keys[it]] !! }
        r1 = Array(n) { map1[keys[it]] !! }
        r2 = Array(n) { map2[keys[it]] !! }

        root = Node()
        val queue = ArrayDeque<Node>(n * 4)

        for (i in 0 until n) {
            val cps = ia[i]
            var cur = root

            for (j in cps.indices) {
                var child = cur.goto.get(cps[j])
                if (child == null) {
                    child = Node()
                    child.depth = cur.depth + 1
                    cur.goto.put(cps[j], child)
                }

                cur = child
            }

            cur.output = i
        }

        root.fail = root
        for (child in root.goto.values) {
            child.fail = root
            queue.addLast(child)
        }

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val fail = cur.fail !!

            for (entry in cur.goto.int2ObjectEntrySet()) {
                val child = entry.value

                child.fail = fail.goto.get(entry.intKey) ?: root
                if (child.output == - 1) child.output = child.fail !!.output

                queue.addLast(child)
            }

            for (entry in fail.goto.int2ObjectEntrySet()) {
                cur.goto.putIfAbsent(entry.intKey, entry.value)
            }
        }

        rootAscii = arrayOfNulls(128)
        for (cp in 0 until 128) rootAscii[cp] = root.goto.get(cp)
    }

    private fun rootGoto(cp: Int): Node? = if (cp < 128) rootAscii[cp] else root.goto.get(cp)

    private fun isWordCp(cp: Int): Boolean {
        if (cp == - 1) return false
        if (cp < 128) return cp == '_'.code || (cp in '0'.code .. '9'.code) || (cp in 'a'.code .. 'z'.code) || (cp in 'A'.code .. 'Z'.code)
        return Character.isLetterOrDigit(cp)
    }

    private fun isValidStringMatch(input: String, start: Int, end: Int): Boolean {
        if (start > 0 && isWordCp(input.codePointBefore(start))) return false
        if (end < input.length && isWordCp(input.codePointAt(end))) return false
        return true
    }

    private fun isValidArrayMatch(chars: IntArray, size: Int, start: Int, end: Int): Boolean {
        if (start > 0 && isWordCp(chars[start - 1])) return false
        if (end < size && isWordCp(chars[end])) return false
        return true
    }

    fun replaceString(input: String): String {
        if (ia.isEmpty()) return input

        val len = input.length
        if (len == 0) return input

        val sb = StringBuilder(len + 32)
        ensureScratch(len)
        val b = scB
        var bl = 0
        var i = 0
        var state = root
        var runStart = 0

        var matchIdx = - 1
        var matchBl = - 1
        var matchPos = - 1

        while (i < len) {
            val cp = input.codePointAt(i)
            val charLen = Character.charCount(cp)

            if (state === root) {
                val next = rootGoto(cp)
                if (next == null) {
                    sb.appendCodePoint(cp)
                    i += charLen
                    continue
                }

                runStart = i
                b[0] = cp
                bl = 1
                state = next
                i += charLen

                if (state.output >= 0 && isValidStringMatch(input, runStart, i)) {
                    matchIdx = state.output; matchBl = bl; matchPos = i
                }

                continue
            }

            val next = state.goto.get(cp) ?: root
            val genuine = next.depth == state.depth + 1

            if (genuine) {
                b[bl] = cp; bl ++
                state = next
                i += charLen

                if (state.output >= 0 && isValidStringMatch(input, runStart, i)) {
                    matchIdx = state.output; matchBl = bl; matchPos = i
                }

                continue
            }

            if (matchIdx >= 0) {
                val litLen = matchBl - ia[matchIdx].size
                for (j in 0 ..< litLen) sb.appendCodePoint(b[j])
                sb.append(r0[matchIdx])
                i = matchPos
            }
            else for (j in 0 ..< bl) sb.appendCodePoint(b[j])

            bl = 0
            state = root
            matchIdx = - 1; matchBl = - 1; matchPos = - 1
        }

        if (matchIdx >= 0) {
            val litLen = matchBl - ia[matchIdx].size
            for (j in 0 ..< litLen) sb.appendCodePoint(b[j])
            sb.append(r0[matchIdx])
            for (j in matchBl ..< bl) sb.appendCodePoint(b[j])
        }
        else for (j in 0 ..< bl) sb.appendCodePoint(b[j])

        return sb.toString()
    }

    fun replaceComponent(input: Component): Component {
        if (ia.isEmpty()) return input

        ensureScratch(128)
        var chars = scChars
        scStyleList.clear()
        val styles = scStyleList
        var size = 0

        input.visit({ style, str ->
            for (cp in str.codePoints()) {
                if (size >= chars.size) {
                    ensureScratch(chars.size * 2)
                    chars = scChars
                }
                chars[size] = cp
                styles.add(style)
                size ++
            }

            Optional.empty()
        }, Style.EMPTY)

        if (size == 0) return input.copy()

        ensureScratch(size)
        val b = scB
        val bs = scBs

        val skip = skips
        val bool = skip != null
        val result = Component.empty()

        var bl = 0
        var i = 0
        var state = root
        var runStart = 0

        var matchIdx = - 1
        var matchBl = - 1
        var matchPos = - 1

        fun flushLiteral(count: Int, offset: Int = 0) {
            var j = offset
            val end = offset + count
            while (j < end) {
                val style = bs[j] !!
                val sb = StringBuilder()

                while (j < end && bs[j] === style) {
                    sb.appendCodePoint(b[j])
                    j ++
                }

                result.append(Component.literal(sb.toString()).withStyle(style))
            }
        }

        fun commitOrFlushAll() {
            if (matchIdx >= 0) {
                val litLen = matchBl - ia[matchIdx].size
                flushLiteral(litLen)
                result.append(r1[matchIdx])
                flushLiteral(bl - matchBl, matchBl)
            }
            else flushLiteral(bl)
        }

        while (i < size) {
            if (bool && styles[i].insertion == skip) {
                commitOrFlushAll()
                bl = 0; state = root
                matchIdx = - 1; matchBl = - 1; matchPos = - 1

                result.append(Component.literal(Character.toString(chars[i])).withStyle(styles[i]))
                i ++
                continue
            }

            val cp = chars[i]

            if (state === root) {
                val next = rootGoto(cp)
                if (next == null) {
                    b[0] = cp; bs[0] = styles[i]
                    flushLiteral(1)
                    i ++
                    continue
                }

                runStart = i
                b[0] = cp; bs[0] = styles[i]
                bl = 1
                state = next
                i ++

                if (state.output >= 0 && isValidArrayMatch(chars, size, runStart, i)) {
                    matchIdx = state.output; matchBl = bl; matchPos = i
                }

                continue
            }

            val next = state.goto.getOrElse(cp) { null } ?: root
            val genuine = next.depth == state.depth + 1

            if (genuine) {
                b[bl] = cp; bs[bl] = styles[i]; bl ++
                state = next
                i ++

                if (state.output >= 0 && isValidArrayMatch(chars, size, runStart, i)) {
                    matchIdx = state.output; matchBl = bl; matchPos = i
                }

                continue
            }

            if (matchIdx >= 0) {
                val litLen = matchBl - ia[matchIdx].size
                flushLiteral(litLen)
                result.append(r1[matchIdx])
                i = matchPos
            }
            else flushLiteral(bl)

            bl = 0
            state = root
            matchIdx = - 1; matchBl = - 1; matchPos = - 1
        }

        commitOrFlushAll()
        return result
    }

    fun replaceCharSequence(input: FormattedCharSequence): FormattedCharSequence {
        if (ia.isEmpty()) return input

        ensureScratch(128)
        var chars = scChars
        scStyleList.clear()
        val styles = scStyleList
        var size = 0

        input.accept { _, style, cp ->
            if (size >= chars.size) {
                ensureScratch(chars.size * 2)
                chars = scChars
            }
            chars[size] = cp
            styles.add(style)
            size ++
            true
        }

        if (size == 0) return input

        val chSnapshot = chars.copyOf(size)
        val stSnapshot = arrayOfNulls<Style>(size)
        for (k in 0 until size) stSnapshot[k] = styles[k]

        val skip = skips
        val bool = skip != null

        return FormattedCharSequence { sink ->
            val b = IntArray(size)
            val bs = arrayOfNulls<Style>(size)
            var bl = 0
            var i = 0
            var state = root
            var runStart = 0

            var matchIdx = - 1
            var matchBl = - 1
            var matchPos = - 1

            fun emitLiteral(count: Int, offset: Int = 0) {
                for (j in offset ..< offset + count) sink.accept(0, bs[j] !!, b[j])
            }

            fun emitMatch(idx: Int) {
                val bss = bs[matchBl - ia[idx].size] !!
                r2[idx].accept { _, repStyle, repCp ->
                    sink.accept(0, repStyle.applyTo(bss), repCp)
                    true
                }
            }

            fun commitOrFlushAll() {
                if (matchIdx >= 0) {
                    val litLen = matchBl - ia[matchIdx].size
                    emitLiteral(litLen)
                    emitMatch(matchIdx)
                    emitLiteral(bl - matchBl, matchBl)
                }
                else emitLiteral(bl)
            }

            while (i < size) {
                if (bool && stSnapshot[i] !!.insertion == skip) {
                    commitOrFlushAll()
                    bl = 0; state = root
                    matchIdx = - 1; matchBl = - 1; matchPos = - 1

                    sink.accept(0, stSnapshot[i] !!, chSnapshot[i])
                    i ++
                    continue
                }

                val cp = chSnapshot[i]

                if (state === root) {
                    val next = rootGoto(cp)
                    if (next == null) {
                        sink.accept(0, stSnapshot[i] !!, cp)
                        i ++
                        continue
                    }

                    runStart = i
                    b[0] = cp; bs[0] = stSnapshot[i]
                    bl = 1
                    state = next
                    i ++

                    if (state.output >= 0 && isValidArrayMatch(chSnapshot, size, runStart, i)) {
                        matchIdx = state.output; matchBl = bl; matchPos = i
                    }

                    continue
                }

                val next = state.goto.get(cp) ?: root
                val genuine = next.depth == state.depth + 1

                if (genuine) {
                    b[bl] = cp; bs[bl] = stSnapshot[i]; bl ++
                    state = next
                    i ++

                    if (state.output >= 0 && isValidArrayMatch(chSnapshot, size, runStart, i)) {
                        matchIdx = state.output; matchBl = bl; matchPos = i
                    }

                    continue
                }

                if (matchIdx >= 0) {
                    val litLen = matchBl - ia[matchIdx].size
                    emitLiteral(litLen)
                    emitMatch(matchIdx)
                    i = matchPos
                }
                else emitLiteral(bl)

                bl = 0
                state = root
                matchIdx = - 1; matchBl = - 1; matchPos = - 1
            }

            commitOrFlushAll()
            true
        }
    }
}