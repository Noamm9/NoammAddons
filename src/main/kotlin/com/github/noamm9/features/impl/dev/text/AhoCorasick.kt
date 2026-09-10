package com.github.noamm9.features.impl.dev.text

import com.google.common.cache.CacheBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import java.util.concurrent.*

/**
 * Taken from Starred's library
 * Under BSD 3-Clause License
 * https://github.com/skies-starred/library/blob/master/src/main/kotlin/xyz/aerii/library/handlers/minecraft/AbstractWords.kt
 * Modified by Noamm9
 */
abstract class AhoCorasick {
    private class Node {
        val goto = Int2ObjectOpenHashMap<Node>(4)
        var fail: Node? = null
        var output: Int = - 1
    }

    var map = HashMap<String, Component>()
    private var ia = emptyArray<IntArray>()
    private var r1 = emptyArray<Component>()
    private var r1Seq = arrayOfNulls<FormattedCharSequence>(0)
    private var root = Node()
    private var skips: String? = null
    private var firstChars = emptySet<Int>()
    private val replaceCache = CacheBuilder.newBuilder()
        .maximumSize(512)
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build<Int, FormattedCharSequence>()


    fun build() {
        val keys = map.keys.sortedByDescending(String::length).toTypedArray()
        val n = keys.size

        if (n == 0) {
            root = Node()
            ia = emptyArray()
            r1 = emptyArray()
            firstChars = emptySet()
            replaceCache.invalidateAll()
            return
        }

        ia = Array(n) { keys[it].codePoints().toArray() }
        r1 = Array(n) { map[keys[it]] !! }
        r1Seq = arrayOfNulls(n)
        firstChars = ia.map { it[0] }.toHashSet()
        replaceCache.invalidateAll()

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

    fun replace(input: FormattedCharSequence): FormattedCharSequence {
        if (ia.isEmpty()) return input

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

        var contentHash = 17
        for (i in 0 until size) {
            contentHash = 31 * contentHash + chars[i]
            contentHash = 31 * contentHash + System.identityHashCode(styles[i])
        }
        val cached = replaceCache.getIfPresent(contentHash)
        if (cached != null) return cached

        var hasFirstChar = false
        for (i in 0 until size) {
            if (chars[i] in firstChars) {
                hasFirstChar = true
                break
            }
        }
        if (! hasFirstChar) return input

        val skip = skips
        val bool = skip != null
        val parts = ArrayList<FormattedCharSequence>()

        val b = IntArray(size)
        val bs = arrayOfNulls<Style>(size)
        var bl = 0
        var i = 0
        var state = root

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

                parts.add(FormattedCharSequence.forward(sb.toString(), style))
            }
        }

        fun replacementSeq(index: Int): FormattedCharSequence {
            var seq = r1Seq[index]
            if (seq == null) {
                seq = Language.getInstance().getVisualOrder(r1[index])
                r1Seq[index] = seq
            }
            return seq
        }

        fun commitPending(isEnd: Boolean) {
            val matchLen = ia[pendingOutput].size
            val trailingLen = bl - pendingBl

            bl = pendingBl - matchLen
            flush()
            parts.add(replacementSeq(pendingOutput))

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
            if (bool && styles[i].insertion == skip) {
                if (pendingOutput != - 1) {
                    commitPending(false)
                    continue
                }

                flush()
                bl = 0
                state = root
                parts.add(FormattedCharSequence.forward(Character.toString(chars[i]), styles[i]))
                i ++
                continue
            }

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

        flush()
        val result = FormattedCharSequence.composite(parts)
        replaceCache.put(contentHash, result)
        return result
    }

    private fun isNameChar(cp: Int) = (cp in 'a'.code .. 'z'.code) || (cp in '0'.code .. '9'.code) || cp == '_'.code
    private fun findColor(chars: IntArray, startAbs: Int): Int {
        var idx = startAbs - 1
        while (idx >= 1 && chars[idx - 1] == 0x00A7) idx -= 2 // '§'
        return idx
    }
}