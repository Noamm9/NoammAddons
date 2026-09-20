package com.github.noamm9.features.impl.misc.shit

import com.github.noamm9.utils.items.ItemUtils.skyblockId
import net.minecraft.world.item.ItemStack

enum class Dagger(val displayName: String, val skyblockIds: Set<String>, val shields: List<HellionShield>, var updated: Boolean = false) {
    TWILIGHT("Twilight Dagger", setOf("HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER"), listOf(HellionShield.SPIRIT, HellionShield.CRYSTAL)),
    FIREDUST("Firedust Dagger", setOf("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER"), listOf(HellionShield.ASHEN, HellionShield.AURIC));

    fun other(): Dagger = if (this == TWILIGHT) FIREDUST else TWILIGHT
    fun activeShield(): HellionShield = shields.firstOrNull { it.active } ?: shields[0]

    companion object {
        fun fromStack(stack: ItemStack?): Dagger? = stack?.skyblockId?.let { id -> entries.find { id in it.skyblockIds } }

        fun updateActiveFromTitle(titleText: String): HellionShield? {
            val shield = HellionShield.fromText(titleText) ?: return null

            for (dagger in entries.filter { shield in it.shields }) {
                dagger.shields.forEach { it.active = false }
                dagger.updated = true
            }
            shield.active = true

            return shield
        }
    }
}