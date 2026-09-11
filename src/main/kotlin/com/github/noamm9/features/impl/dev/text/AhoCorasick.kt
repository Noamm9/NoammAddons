package com.github.noamm9.features.impl.dev.text

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import java.util.Optional
import kotlin.collections.ArrayDeque

/**
 * Taken from Starred's library
 * Under BSD 3-Clause License
 * https://github.com/skies-starred/library/blob/7a2ddc19f9e1c7356005ff317fbf0eba855935a5/src/main/kotlin/foo/starred/snowbird/api/text/replacer/AbstractTextReplacer.kt
 * Modified by Noamm9
 */
abstract class AhoCorasick {
    private var root = Node()
    private var ia = emptyArray<IntArray>()
    private var r0 = emptyArray<String>()
    private var r1 = emptyArray<Component>()
    private var r2 = emptyArray<FormattedCharSequence>()
    private var firstChars = emptySet<Int>()

    private var map0 = HashMap<String, String>()
    private var map1 = HashMap<String, Component>()
    private var map2 = HashMap<String, FormattedCharSequence>()

    private val stringCache = LruCache<String, String>(CACHE_SIZE)
    private val componentCache = LruCache<Component, Component>(CACHE_SIZE)
    private val sequenceCache = LruCache<FormattedCharSequence, FormattedCharSequence>(CACHE_SIZE)

    fun put(key: String, str: String, cmp: Component, seq: FormattedCharSequence = cmp.visualOrderText) {
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
        stringCache.clear()
        componentCache.clear()
        sequenceCache.clear()

        val keys = map0.keys.sortedByDescending { it.length }.toTypedArray()
        val n = keys.size

        if (n == 0) {
            root = Node()
            ia = emptyArray()
            r0 = emptyArray()
            r1 = emptyArray()
            r2 = emptyArray()
            firstChars = emptySet()
            return
        }

        ia = Array(n) { keys[it].codePoints().toArray() }
        r0 = Array(n) { map0[keys[it]] !! }
        r1 = Array(n) { map1[keys[it]] !! }
        r2 = Array(n) { map2[keys[it]] !! }
        firstChars = ia.map { it[0] }.toHashSet()

        root = Node()
        val queue = ArrayDeque<Node>(n * 4)

        for (i in 0 until n) {
            val cps = ia[i]
            var cur = root

            for (j in cps.indices) {
                var child = cur.goto.get(cps[j])
                if (child == null) {
                    child = Node()
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
    }

    fun fn(input: String): String {
        if (ia.isEmpty()) return input
        return stringCache.getOrPut(input) { replaceString(input) }
    }

    private fun replaceString(input: String): String {
        val chars = input.codePoints().toArray()
        val size = chars.size
        if (size == 0) return input

        var hasFirstChar = false
        for (i in 0 until size) {
            if (chars[i] in firstChars) {
                hasFirstChar = true
                break
            }
        }
        if (! hasFirstChar) return input

        val sb = StringBuilder(size + 32)
        val b = IntArray(size)
        var bl = 0
        var i = 0
        var state = root
        var matched = false

        var pendingOutput = - 1
        var pendingStartAbs = - 1
        var pendingEndAbs = - 1
        var pendingBl = - 1

        fun flush(upto: Int) {
            for (j in 0 until upto) sb.appendCodePoint(b[j])
        }

        fun commitPending(isEnd: Boolean) {
            val matchLen = ia[pendingOutput].size
            val trailingLen = bl - pendingBl

            bl = pendingBl - matchLen
            flush(bl)
            sb.append(r0[pendingOutput])
            matched = true

            if (isEnd) {
                for (k in 0 until trailingLen) b[k] = b[pendingBl + k]
                flush(trailingLen)
                bl = 0
            }
            else {
                i = pendingEndAbs + 1
                bl = 0
                state = root
            }

            pendingOutput = - 1
        }

        while (i < size) {
            state = state.goto.get(chars[i]) ?: root

            b[bl] = chars[i]
            bl ++

            if (state.output >= 0) {
                val length = ia[state.output].size
                val startAbs = i - length + 1

                val effPrev = findColor(chars, startAbs)
                val validBefore = effPrev < 0 || ! isNameChar(chars[effPrev])
                val validAfter = i == size - 1 || ! isNameChar(chars[i + 1])

                if (validBefore && validAfter) {
                    if (pendingOutput == - 1 || startAbs == pendingStartAbs) {
                        pendingOutput = state.output
                        pendingStartAbs = startAbs
                        pendingEndAbs = i
                        pendingBl = bl
                    }
                    else {
                        commitPending(false)
                        continue
                    }
                }
            }

            i ++
        }

        if (pendingOutput != - 1) commitPending(true)

        if (! matched) return input

        flush(bl)
        return sb.toString()
    }

    fun fn(input: Component): Component {
        if (ia.isEmpty()) return input
        return componentCache.getOrPut(input) { replaceComponent(input) }
    }

    private fun replaceComponent(input: Component): Component {
        var chars = IntArray(128)
        val styles = ArrayList<Style>(128)
        var size = 0

        input.visit({ style, str ->
            for (cp in str.codePoints()) {
                if (size >= chars.size) chars = chars.copyOf(chars.size * 2)
                chars[size] = cp
                styles.add(style)
                size ++
            }

            Optional.empty()
        }, Style.EMPTY)

        if (size == 0) return input

        var hasFirstChar = false
        for (i in 0 until size) {
            if (chars[i] in firstChars) {
                hasFirstChar = true
                break
            }
        }
        if (! hasFirstChar) return input

        val result = Component.empty()

        val b = IntArray(size)
        val bs = arrayOfNulls<Style>(size)
        var bl = 0
        var i = 0
        var state = root
        var matched = false

        var pendingOutput = - 1
        var pendingStartAbs = - 1
        var pendingEndAbs = - 1
        var pendingBl = - 1

        fun flush() {
            var j = 0
            while (j < bl) {
                val style = bs[j] !!
                val sb = StringBuilder()

                while (j < bl && bs[j] === style) {
                    sb.appendCodePoint(b[j])
                    j ++
                }

                result.append(Component.literal(sb.toString()).withStyle(style))
            }
        }

        fun commitPending(isEnd: Boolean) {
            val matchLen = ia[pendingOutput].size
            val trailingLen = bl - pendingBl

            bl = pendingBl - matchLen
            flush()
            result.append(r1[pendingOutput])
            matched = true

            if (isEnd) {
                for (k in 0 until trailingLen) {
                    b[k] = b[pendingBl + k]
                    bs[k] = bs[pendingBl + k]
                }
                bl = trailingLen
                flush()
                bl = 0
            }
            else {
                i = pendingEndAbs + 1
                bl = 0
                state = root
            }

            pendingOutput = - 1
        }

        while (i < size) {
            state = state.goto.get(chars[i]) ?: root

            b[bl] = chars[i]
            bs[bl] = styles[i]
            bl ++

            if (state.output >= 0) {
                val length = ia[state.output].size
                val startAbs = i - length + 1

                val effPrev = findColor(chars, startAbs)
                val validBefore = effPrev < 0 || ! isNameChar(chars[effPrev])
                val validAfter = i == size - 1 || ! isNameChar(chars[i + 1])

                if (validBefore && validAfter) {
                    if (pendingOutput == - 1 || startAbs == pendingStartAbs) {
                        pendingOutput = state.output
                        pendingStartAbs = startAbs
                        pendingEndAbs = i
                        pendingBl = bl
                    }
                    else {
                        commitPending(false)
                        continue
                    }
                }
            }

            i ++
        }

        if (pendingOutput != - 1) commitPending(true)

        if (! matched) return input

        flush()
        return result
    }

    fun fn(input: FormattedCharSequence): FormattedCharSequence {
        if (ia.isEmpty()) return input
        return sequenceCache.getOrPut(input) { replaceSequence(input) }
    }

    private fun replaceSequence(input: FormattedCharSequence): FormattedCharSequence {
        var chars = IntArray(128)
        val styles = ArrayList<Style>(128)
        var size = 0

        input.accept { _, style, cp ->
            if (size >= chars.size) chars = chars.copyOf(chars.size * 2)
            chars[size] = cp
            styles.add(style)
            size ++
            true
        }

        if (size == 0) return input

        var hasFirstChar = false
        for (i in 0 until size) {
            if (chars[i] in firstChars) {
                hasFirstChar = true
                break
            }
        }
        if (! hasFirstChar) return input

        return FormattedCharSequence { sink ->
            val s = IntArray(size)
            val bs = arrayOfNulls<Style>(size)
            var bl = 0
            var i = 0
            var state = root

            var pendingOutput = - 1
            var pendingStartAbs = - 1
            var pendingEndAbs = - 1
            var pendingBl = - 1

            fun flush(upto: Int) {
                for (j in 0 until upto) sink.accept(0, bs[j] !!, s[j])
            }

            fun commitPending(isEnd: Boolean) {
                val io = pendingOutput
                val matchLen = ia[io].size
                val trailingLen = bl - pendingBl

                bl = pendingBl - matchLen
                flush(bl)

                val bss = bs[bl] !!
                r2[io].accept { _, repStyle, repCp ->
                    sink.accept(0, repStyle.applyTo(bss), repCp)
                    true
                }

                if (isEnd) {
                    for (k in 0 until trailingLen) {
                        s[k] = s[pendingBl + k]
                        bs[k] = bs[pendingBl + k]
                    }
                    bl = trailingLen
                    flush(bl)
                    bl = 0
                }
                else {
                    i = pendingEndAbs + 1
                    bl = 0
                    state = root
                }

                pendingOutput = - 1
            }

            while (i < size) {
                state = state.goto.get(chars[i]) ?: root

                s[bl] = chars[i]
                bs[bl] = styles[i]
                bl ++

                if (state.output >= 0) {
                    val length = ia[state.output].size
                    val startAbs = i - length + 1

                    val effPrev = findColor(chars, startAbs)
                    val validBefore = effPrev < 0 || ! isNameChar(chars[effPrev])
                    val validAfter = i == size - 1 || ! isNameChar(chars[i + 1])

                    if (validBefore && validAfter) {
                        if (pendingOutput == - 1 || startAbs == pendingStartAbs) {
                            pendingOutput = state.output
                            pendingStartAbs = startAbs
                            pendingEndAbs = i
                            pendingBl = bl
                        }
                        else {
                            commitPending(false)
                            continue
                        }
                    }
                }

                i ++
            }

            if (pendingOutput != - 1) commitPending(true)

            flush(bl)
            true
        }
    }

    private fun isNameChar(cp: Int) = (cp in 'a'.code .. 'z'.code) || (cp in '0'.code .. '9'.code) || cp == '_'.code
    private fun findColor(chars: IntArray, startAbs: Int): Int {
        var idx = startAbs - 1
        while (idx >= 1 && chars[idx - 1] == 0x00A7) idx -= 2 // '§'
        return idx
    }

    companion object {
        private const val CACHE_SIZE = 256

        private class Node {
            val goto = Int2ObjectOpenHashMap<Node>(4)
            var fail: Node? = null
            var output: Int = - 1
        }

        private class LruCache<K, V>(private val maxSize: Int): LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > maxSize
        }
    }
}