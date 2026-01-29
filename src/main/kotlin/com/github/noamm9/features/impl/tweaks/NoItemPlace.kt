package com.github.noamm9.features.impl.tweaks

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.Utils.endsWithOneOf
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.Utils.startsWithOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.world.item.context.BlockPlaceContext

object NoItemPlace: Feature("Stops you from placing skull blocks/Items") {
    private val WitherRelicRegex = Regex("Corrupted .+ Relic")
    @JvmStatic
    fun placeHook(context: BlockPlaceContext): Boolean {
        if (! enabled) return false
        val item = context.player?.mainHandItem ?: return false
        val id = item.skyblockId

        return when {
            LocationUtils.F7Phase == 5 && WitherRelicRegex.matches(item.item.name.unformattedText) -> true

            id.startsWithOneOf("ABIPHONE") -> true

            id.endsWithOneOf(
                "_TUBA",
                "_POWER_ORB",
                "_POCKET_BLACK_HOLE"
            ) -> true

            else -> id.equalsOneOf(
                "BOUQUET_OF_LIES",
                "FLOWER_OF_TRUTH",
                "BAT_WAND",
                "STARRED_BAT_WAND",
                "INFINITE_SPIRIT_LEAP",
                "ROYAL_PIGEON",
                "ARROW_SWAPPER",
                "JINGLE_BELLS",
                "FIRE_FREEZE_STAFF",
                "UMBERELLA",
                "ASCENSION_ROPE"
            )
        }
    }
}