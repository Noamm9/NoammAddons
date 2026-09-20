package com.github.noamm9.features.impl.misc.shit

enum class HellionShield(val cleanName: String, val chatColor: String, var active: Boolean = false) {
    AURIC("Auric", "§e"),
    ASHEN("Ashen", "§8"),
    SPIRIT("Spirit", "§f"),
    CRYSTAL("Crystal", "§b");

    fun other(): HellionShield = Dagger.entries.first { this in it.shields }.shields.first { it != this }

    companion object {
        fun fromText(text: String): HellionShield? = entries.find { text.contains(it.name, true) }
    }
}