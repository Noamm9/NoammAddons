package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.dungeons.enums.WitherRelic
import com.github.noamm9.utils.endsWithOneOf
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.startsWithOneOf
import net.minecraft.world.item.context.BlockPlaceContext

/**
 * @see com.github.noamm9.mixin.MixinBlockItem
 */
object NoItemPlace: Feature("Stops you from placing skull blocks/items.") {

    @JvmStatic
    fun placeHook(context: BlockPlaceContext): Boolean {
        if (! enabled) return false
        val item = context.player?.mainHandItem ?: return false
        val name = item.hoverName.unformattedText
        val id = item.skyblockId

        return when {
            LocationUtils.F7Phase == 5 && WitherRelic.fromName(name) != null -> true

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
            )
        }
    }
}