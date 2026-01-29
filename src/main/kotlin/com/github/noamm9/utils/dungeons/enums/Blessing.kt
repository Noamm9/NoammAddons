package com.github.noamm9.utils.dungeons.enums

enum class Blessing(
    val displayString: String,
    var regex: Regex,
    var current: Int = 0
) {
    POWER("Power", Regex("Blessing of Power (X{0,3}(IX|IV|V?I{0,3}))")),
    LIFE("Life", Regex("Blessing of Life (X{0,3}(IX|IV|V?I{0,3}))")),
    WISDOM("Wisdom", Regex("Blessing of Wisdom (X{0,3}(IX|IV|V?I{0,3}))")),
    STONE("Stone", Regex("Blessing of Stone (X{0,3}(IX|IV|V?I{0,3}))")),
    TIME("Time", Regex("Blessing of Time (V)"));

    companion object {
        fun reset() = entries.forEach { it.current = 0 }
    }
}