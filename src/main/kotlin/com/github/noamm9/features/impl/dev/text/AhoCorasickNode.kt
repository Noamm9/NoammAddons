package com.github.noamm9.features.impl.dev.text

class AhoCorasickNode {
    val children = HashMap<Char, AhoCorasickNode>()
    var failure: AhoCorasickNode? = null
    var output: Replacement? = null
    var outputLink: AhoCorasickNode? = null
}