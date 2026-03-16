package com.github.noamm9.utils.dungeons

import com.github.noamm9.NoammAddons.electionData
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.dungeon.ScoreCalculator
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.network.data.ElectionData
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SkullBlock
import net.minecraft.world.level.block.entity.SkullBlockEntity

object DungeonUtils {
    val FLOOR_NAMES = listOf("ENTRANCE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN")
    const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"
    const val REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e"

    @JvmStatic
    fun isSecret(pos: BlockPos): Boolean {
        val block = mc.level?.getBlockState(pos)?.block ?: Blocks.AIR

        return when {
            block is SkullBlock -> (mc.level?.getBlockEntity(pos) as? SkullBlockEntity)?.ownerProfile?.partialProfile()?.id.toString().equalsOneOf(WITHER_ESSENCE_ID, REDSTONE_KEY_ID)
            block.equalsOneOf(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.LEVER) -> true
            else -> false
        }
    }

    fun isPaul(): Boolean {
        if (ScoreCalculator.forcePaul.value) return true
        val mayorPerks = mutableListOf<ElectionData.Perk>()
        electionData.mayor.perks.let(mayorPerks::addAll)
        electionData.minister?.perk?.let(mayorPerks::add)
        return mayorPerks.any { it.name == "EZPZ" }
    }

    val dungeonItemDrops = listOf(
        "Health Potion VIII Splash Potion", "Healing Potion 8 Splash Potion",
        "Healing Potion VIII Splash Potion", "Healing VIII Splash Potion",
        "Healing 8 Splash Potion", "Decoy", "Inflatable Jerry", "Spirit Leap",
        "Trap", "Training Weights", "Defuse Kit", "Dungeon Chest Key",
        "Treasure Talisman", "Revive Stone", "Architect's First Draft",
        "Secret Dye", "Candycomb"
    )
}