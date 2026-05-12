package com.github.noamm9.features.impl.dev.text

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence


/**
 * Taken from Starred's library
 * Under BSD 3-Clause License
 * https://github.com/skies-starred/library/blob/master/src/main/kotlin/xyz/aerii/library/handlers/minecraft/AbstractWords.kt
 */
class AhoCorasick {
    private var root = Node()

    private var ia = emptyArray<IntArray>()
    private var r0 = emptyArray<String>()
    private var r1 = emptyArray<MutableComponent>()
    private var r2 = emptyArray<FormattedCharSequence>()

    fun isEmpty(): Boolean = ia.isEmpty()

    fun build(map: HashMap<String, String>, resolve: (String) -> Pair<String, MutableComponent>) {
        root = Node()
        if (map.isEmpty()) {
            ia = emptyArray()
            r0 = emptyArray()
            r1 = emptyArray()
            r2 = emptyArray()
            return
        }

        val keys = map.keys.sortedByDescending { it.length }.toTypedArray()
        val n = keys.size
        ia = Array(n) { keys[it].codePoints().toArray() }
        val pairs = Array(n) { resolve(map[keys[it]] !!) }
        r0 = Array(n) { pairs[it].first }
        r1 = Array(n) { pairs[it].second.copy() }
        r2 = Array(n) { r1[it].visualOrderText }

        val queue = ArrayDeque<Node>(n * 4)

        for (i in 0 until n) {
            val cps = ia[i]
            var cur = root
            for (j in cps.indices) {
                val cp = cps[j]
                var child = cur.goto.get(cp)
                if (child == null) {
                    child = Node()
                    cur.goto.put(cp, child)
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

            val curIter = cur.goto.int2ObjectEntrySet().iterator()
            while (curIter.hasNext()) {
                val entry = curIter.next()
                val child = entry.value

                val childFail = fail.goto.get(entry.intKey) ?: root
                child.fail = childFail
                if (child.output == - 1) child.output = childFail.output

                queue.addLast(child)
            }

            val failIter = fail.goto.int2ObjectEntrySet().iterator()
            while (failIter.hasNext()) {
                val entry = failIter.next()
                cur.goto.putIfAbsent(entry.intKey, entry.value)
            }
        }
    }

    fun replaceString(input: String): String {
        if (ia.isEmpty()) return input
        val len = input.length
        if (len == 0) return input

        val b = IntArray(len + 16)
        var bl = 0
        var i = 0
        var state = root
        var sb: StringBuilder? = null

        while (i < len) {
            val cp = input.codePointAt(i)
            state = state.goto.get(cp) ?: root

            b[bl] = cp
            bl ++

            if (state.output >= 0) {
                val idx = state.output
                bl -= ia[idx].size

                val outSb = sb ?: StringBuilder(len + 32).also { sb = it }
                var j = 0
                while (j < bl) outSb.appendCodePoint(b[j ++])
                outSb.append(r0[idx])
                bl = 0
                state = root
            }

            i += Character.charCount(cp)
        }

        val tail = sb ?: return input
        var j = 0
        while (j < bl) tail.appendCodePoint(b[j ++])

        return tail.toString()
    }

    fun replaceComponent(input: Component): Component {
        if (ia.isEmpty()) return input

        val charsList = ArrayList<Int>(128)
        val stylesList = ArrayList<Style>(128)

        input.visualOrderText.accept { _, style, cp ->
            charsList.add(cp)
            stylesList.add(style)
            true
        }

        val size = charsList.size
        if (size == 0) return input.copy()

        val chars = IntArray(size) { charsList[it] }

        val styles = stylesList.toTypedArray()

        val b = IntArray(size)
        val bs = arrayOfNulls<Style>(size)

        fun flush(builder: MutableComponent, blLim: Int) {
            var j = 0
            while (j < blLim) {
                val style = bs[j] ?: Style.EMPTY
                val slice = StringBuilder()
                while (j < blLim && bs[j] === style) {
                    slice.appendCodePoint(b[j])
                    j ++
                }
                builder.append(Component.literal(slice.toString()).withStyle(style))
            }
        }

        var result: MutableComponent? = null

        fun ensureResult(): MutableComponent {
            val existing = result
            if (existing != null) return existing
            val r = Component.literal("") as MutableComponent
            result = r
            return r
        }

        var bl = 0
        var idx = 0
        var state = root

        while (idx < size) {
            state = state.goto.get(chars[idx]) ?: root

            b[bl] = chars[idx]
            bs[bl] = styles[idx]
            bl ++

            if (state.output >= 0) {
                val outIdx = state.output
                bl -= ia[outIdx].size
                flush(ensureResult(), bl)

                val baseStyle = bs[bl] ?: Style.EMPTY
                val rep = r1[outIdx].copy() as MutableComponent
                if (rep.style.isEmpty) rep.withStyle(baseStyle)
                ensureResult().append(rep)

                bl = 0
                state = root
            }
            idx ++
        }

        val out = result ?: return input
        flush(out, bl)
        return out
    }

    fun replaceCharSequence(input: FormattedCharSequence): FormattedCharSequence {
        if (ia.isEmpty()) return input

        var chars = IntArray(128)
        val styles = ArrayList<Style>(128)
        var size = 0
        var scanState = root
        var mightMatch = false

        input.accept { _, style, cp ->
            if (size >= chars.size) chars = chars.copyOf(chars.size * 2)
            chars[size] = cp
            styles.add(style)
            scanState = scanState.goto.get(cp) ?: root
            if (scanState.output >= 0) mightMatch = true
            size ++
            true
        }

        if (size == 0) return input
        if (! mightMatch) return input

        val charsFinal = chars.copyOf(size)
        val stylesFinal = styles.toTypedArray()

        return FormattedCharSequence { sink ->
            val s = IntArray(size)
            val bs = arrayOfNulls<Style>(size)
            var bl = 0
            var i = 0
            var state = root

            while (i < size) {
                state = state.goto.get(charsFinal[i]) ?: root

                s[bl] = charsFinal[i]
                bs[bl] = stylesFinal[i]
                bl ++

                if (state.output >= 0) {
                    val io = state.output
                    val ml = ia[io].size
                    val ms = bl - ml

                    var j = 0
                    while (j < ms) {
                        sink.accept(0, bs[j] !!, s[j])
                        j ++
                    }

                    val bss = bs[ms] !!
                    r2[io].accept { _, repStyle, repCp ->
                        sink.accept(0, repStyle.applyTo(bss), repCp)
                        true
                    }

                    bl = 0
                    state = root
                }
                i ++
            }

            var jTail = 0
            while (jTail < bl) {
                sink.accept(0, bs[jTail] !!, s[jTail])
                jTail ++
            }

            true
        }
    }

    private class Node {
        val goto = Int2ObjectOpenHashMap<Node>(4)
        var fail: Node? = null
        var output: Int = - 1
    }
}