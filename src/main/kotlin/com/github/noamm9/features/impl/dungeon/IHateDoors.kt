package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.core.DoorTile
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.util.Mth.floor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object IHateDoors: Feature("Replaces Wither and Blood doors with stained glass.") {
    private val glassEntrance by ToggleSetting("Glass Entrance Door").section("Doors")
    private val glassWither by ToggleSetting("Glass Wither Door")
    private val glassBlood by ToggleSetting("Glass Blood Door")

    private val entranceGlass by DropdownSetting("Entrance Door Glass", Glass.WHITE.ordinal, Glass.options).section("Glass Color").showIf { glassEntrance.value }
    private val witherGlass by DropdownSetting("Wither Door Glass", Glass.BLACK.ordinal, Glass.options).showIf { glassWither.value }
    private val bloodGlass by DropdownSetting("Blood Door Glass", Glass.RED.ordinal, Glass.options).showIf { glassBlood.value }

    private val doors = mutableMapOf<DoorTile, Iterable<BlockPos>>()

    override fun init() {
        register<WorldChangeEvent> { doors.clear() }
        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            doors.forEach { (tile, blocks) ->
                val glassState = tile.type.getGlass() ?: return@forEach
                val currentBlock = WorldUtils.getBlockAt(tile.x, 69, tile.z)
                if (currentBlock.equalsOneOf(Blocks.AIR, Blocks.BARRIER)) return@forEach
                if (tile.type.source == glassState.block) return@forEach
                for (pos in blocks) WorldUtils.setBlockAt(pos, glassState)
            }
        }

        register<DungeonEvent.TileScannedEvent> {
            val door = event.tile as? DoorTile ?: return@register
            val glassState = door.type.getGlass() ?: return@register

            doors[door] = BlockPos.betweenClosed(
                floor(door.aabb.minX), floor(door.aabb.minY), floor(door.aabb.minZ),
                floor(door.aabb.maxX) - 1, floor(door.aabb.maxY) - 1, floor(door.aabb.maxZ) - 1
            ).also { for (pos in it) WorldUtils.setBlockAt(pos, glassState) }
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            val packet = event.packet as? ClientboundBlockUpdatePacket ?: return@register
            if (packet.blockState.block.equalsOneOf(Blocks.BARRIER, Blocks.AIR)) return@register
            val door = doors.entries.find { it.value.any { pos -> pos == packet.pos } }?.key ?: return@register
            if (packet.blockState.block != door.type.source) return@register
            WorldUtils.setBlockAt(packet.pos, door.type.getGlass() ?: return@register)
            event.isCanceled = true
        }
    }

    fun DoorType.getGlass(): BlockState? {
        val glassIndex = when (this) {
            DoorType.ENTRANCE -> if (glassEntrance.value) entranceGlass else null
            DoorType.WITHER -> if (glassWither.value) witherGlass else null
            DoorType.BLOOD -> if (glassBlood.value) bloodGlass else null
            else -> null
        }?.value ?: return null

        return Glass.entries[glassIndex].state
    }

    private enum class Glass(val displayName: String, block: Block) {
        DEFAULT("Default", Blocks.GLASS),
        WHITE("White", Blocks.WHITE_STAINED_GLASS),
        BLACK("Black", Blocks.BLACK_STAINED_GLASS),
        CYAN("Cyan", Blocks.CYAN_STAINED_GLASS),
        LIGHT_BLUE("Light Blue", Blocks.LIGHT_BLUE_STAINED_GLASS),
        RED("Red", Blocks.RED_STAINED_GLASS),
        PINK("Pink", Blocks.PINK_STAINED_GLASS),
        ORANGE("Orange", Blocks.ORANGE_STAINED_GLASS),
        MAGENTA("Magenta", Blocks.MAGENTA_STAINED_GLASS),
        YELLOW("Yellow", Blocks.YELLOW_STAINED_GLASS),
        LIME("Lime", Blocks.LIME_STAINED_GLASS),
        GRAY("Gray", Blocks.GRAY_STAINED_GLASS),
        LIGHT_GRAY("Light Gray", Blocks.LIGHT_GRAY_STAINED_GLASS),
        PURPLE("Purple", Blocks.PURPLE_STAINED_GLASS),
        BLUE("Blue", Blocks.BLUE_STAINED_GLASS),
        BROWN("Brown", Blocks.BROWN_STAINED_GLASS),
        GREEN("Green", Blocks.GREEN_STAINED_GLASS);

        val state = block.defaultBlockState()

        companion object {
            val options = Glass.entries.map(Glass::displayName)
        }
    }
}
//#endif