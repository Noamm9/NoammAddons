package com.github.noamm9.features.impl.dev.text

import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction
import java.util.function.Function

internal class ReplacementMap(private val onMutation: Runnable): ConcurrentHashMap<String, String>() {
    @Volatile
    var version = 0
        private set

    private fun markDirty() {
        version ++
        onMutation.run()
    }

    override fun put(key: String, value: String): String? {
        val previous = super.put(key, value)
        if (previous != value) markDirty()
        return previous
    }

    override fun putIfAbsent(key: String, value: String): String? {
        val previous = super.putIfAbsent(key, value)
        if (previous == null) markDirty()
        return previous
    }

    override fun putAll(from: Map<out String, String>) {
        if (from.isEmpty()) return

        var changed = false
        from.forEach { (key, value) ->
            if (super.put(key, value) != value) changed = true
        }

        if (changed) markDirty()
    }

    override fun remove(key: String): String? {
        val removed = super.remove(key)
        if (removed != null) markDirty()
        return removed
    }

    override fun remove(key: String, value: String): Boolean {
        val removed = super.remove(key, value)
        if (removed) markDirty()
        return removed
    }

    override fun replace(key: String, value: String): String? {
        val previous = super.replace(key, value)
        if (previous != null && previous != value) markDirty()
        return previous
    }

    override fun replace(key: String, oldValue: String, newValue: String): Boolean {
        val replaced = super.replace(key, oldValue, newValue)
        if (replaced && oldValue != newValue) markDirty()
        return replaced
    }

    override fun clear() {
        if (isEmpty()) return
        super.clear()
        markDirty()
    }

    override fun compute(key: String, remappingFunction: BiFunction<in String, in String?, out String?>): String? {
        val before = super[key]
        val result = super.compute(key, remappingFunction)
        if (before != result) markDirty()
        return result
    }

    override fun computeIfAbsent(key: String, mappingFunction: Function<in String, out String>): String {
        val contained = containsKey(key)
        val result = super.computeIfAbsent(key, mappingFunction)
        if (! contained) markDirty()
        return result
    }

    override fun computeIfPresent(key: String, remappingFunction: BiFunction<in String, in String, out String?>): String? {
        val before = super[key]
        val result = super.computeIfPresent(key, remappingFunction)
        if (before != result) markDirty()
        return result
    }

    override fun merge(key: String, value: String, remappingFunction: BiFunction<in String, in String, out String>): String {
        val before = super[key]
        val result = super.merge(key, value, remappingFunction)
        if (before != result) markDirty()
        return result ?: value
    }
}