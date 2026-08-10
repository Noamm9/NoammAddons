package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.DoorTile
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object IHateDoors: Feature("Replaces Wither and Blood doors with stained glass.") {
    private val glassOptions = Glass.entries.map(Glass::displayName)
    private val stainedGlassBlocks = Glass.entries.mapTo(mutableSetOf(), Glass::block)

    private val witherGlass by DropdownSetting("Wither Door Glass", Glass.BLACK.ordinal, glassOptions).section("Wither Door")
    private val witherKeyGlass by DropdownSetting("Wither Key Glass", Glass.LIME.ordinal, glassOptions).withDescription("Glass color after a Wither Key has been picked up.")

    private val bloodGlass by DropdownSetting("Blood Door Glass", Glass.RED.ordinal, glassOptions).section("Blood Door")
    private val bloodKeyGlass by DropdownSetting("Blood Key Glass", Glass.LIME.ordinal, glassOptions).withDescription("Glass color after a Blood Key has been picked up.")

    private val replacedBlocks = mutableMapOf<BlockPos, ReplacedDoorBlock>()
    private val cursor = BlockPos.MutableBlockPos()

    override fun init() {
        register<WorldChangeEvent> { replacedBlocks.clear() }

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register

            updateBlocks()

            for (tile in DungeonScanner.dungeonList) {
                if (tile !is DoorTile) continue
                val sourceBlock = tile.type.sourceBlock ?: continue
                val glassState = tile.type.glassState ?: continue

                for (x in (tile.x - 1) .. (tile.x + 1)) {
                    for (y in 69 until 73) {
                        for (z in (tile.z - 1) .. (tile.z + 1)) {
                            cursor.set(x, y, z)
                            val state = WorldUtils.getStateAt(cursor)
                            if (! state.`is`(sourceBlock)) continue

                            replacedBlocks.putIfAbsent(cursor.immutable(), ReplacedDoorBlock(state, tile.type))
                            WorldUtils.setBlockAt(cursor, glassState)
                        }
                    }
                }
            }
        }
    }

    override fun onDisable() {
        super.onDisable()

        for ((pos, replacedBlock) in replacedBlocks) {
            if (WorldUtils.getBlockAt(pos) !in stainedGlassBlocks) continue
            WorldUtils.setBlockAt(pos, replacedBlock.originalState)
        }
        replacedBlocks.clear()
    }

    private fun updateBlocks() {
        val iterator = replacedBlocks.iterator()
        while (iterator.hasNext()) {
            val (pos, replacedBlock) = iterator.next()
            val glassState = replacedBlock.type.glassState ?: continue
            val currentBlock = WorldUtils.getBlockAt(pos)

            if (currentBlock != replacedBlock.originalState.block && currentBlock !in stainedGlassBlocks) {
                iterator.remove()
                continue
            }

            if (currentBlock != glassState.block) WorldUtils.setBlockAt(pos, glassState)
        }
    }

    private val DoorType.sourceBlock: Block?
        get() = when (this) {
            DoorType.WITHER -> Blocks.COAL_BLOCK
            DoorType.BLOOD -> Blocks.RED_TERRACOTTA
            else -> null
        }

    private val DoorType.glassState: BlockState?
        get() = when (this) {
            DoorType.WITHER -> (if (DungeonListener.hasDoorKey(this)) witherKeyGlass else witherGlass).glassState
            DoorType.BLOOD -> (if (DungeonListener.hasDoorKey(this)) bloodKeyGlass else bloodGlass).glassState
            else -> null
        }

    private val DropdownSetting.glassState get() = Glass.entries[value].state

    private data class ReplacedDoorBlock(val originalState: BlockState, val type: DoorType)

    private enum class Glass(val displayName: String, val block: Block) {
        WHITE("White", Blocks.WHITE_STAINED_GLASS),
        ORANGE("Orange", Blocks.ORANGE_STAINED_GLASS),
        MAGENTA("Magenta", Blocks.MAGENTA_STAINED_GLASS),
        LIGHT_BLUE("Light Blue", Blocks.LIGHT_BLUE_STAINED_GLASS),
        YELLOW("Yellow", Blocks.YELLOW_STAINED_GLASS),
        LIME("Lime", Blocks.LIME_STAINED_GLASS),
        PINK("Pink", Blocks.PINK_STAINED_GLASS),
        GRAY("Gray", Blocks.GRAY_STAINED_GLASS),
        LIGHT_GRAY("Light Gray", Blocks.LIGHT_GRAY_STAINED_GLASS),
        CYAN("Cyan", Blocks.CYAN_STAINED_GLASS),
        PURPLE("Purple", Blocks.PURPLE_STAINED_GLASS),
        BLUE("Blue", Blocks.BLUE_STAINED_GLASS),
        BROWN("Brown", Blocks.BROWN_STAINED_GLASS),
        GREEN("Green", Blocks.GREEN_STAINED_GLASS),
        RED("Red", Blocks.RED_STAINED_GLASS),
        BLACK("Black", Blocks.BLACK_STAINED_GLASS);

        val state: BlockState = block.defaultBlockState()
    }
}
//#endif
