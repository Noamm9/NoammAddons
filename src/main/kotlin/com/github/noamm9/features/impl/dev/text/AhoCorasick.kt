package com.github.noamm9.features.impl.dev.text

class AhoCorasick(replacements: List<Replacement>) {
    val root = AhoCorasickNode()

    init {
        for (replacement in replacements) {
            var node = root
            for (ch in replacement.target) {
                node = node.children.getOrPut(ch) { AhoCorasickNode() }
            }

            if (node.output == null) node.output = replacement
        }

        val queue = ArrayDeque<AhoCorasickNode>()

        for (child in root.children.values) {
            child.failure = root
            queue.add(child)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for ((ch, child) in current.children) {
                child.failure = goto(current.failure ?: root, ch)

                child.outputLink = when {
                    child.failure?.output != null -> child.failure
                    else -> child.failure?.outputLink
                }

                queue.add(child)
            }
        }
    }

    fun goto(node: AhoCorasickNode, ch: Char): AhoCorasickNode {
        var current = node
        while (current != root && ch !in current.children)
            current = current.failure!!

        return current.children[ch] ?: root
    }
}