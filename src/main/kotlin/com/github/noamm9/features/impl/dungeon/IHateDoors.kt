package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.core.DoorTile
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object IHateDoors: Feature("Visually replaces dungeon doors with glass.") {
    private val glassEntrance by ToggleSetting("Glass Entrance Door").section("Doors")
    private val glassWither by ToggleSetting("Glass Wither Door")
    private val glassBlood by ToggleSetting("Glass Blood Door")

    private val entranceGlass by DropdownSetting("Entrance Door Glass", Glass.WHITE.ordinal, Glass.options).section("Glass Color").showIf { glassEntrance.value }
    private val witherGlass by DropdownSetting("Wither Door Glass", Glass.BLACK.ordinal, Glass.options).showIf { glassWither.value }
    private val bloodGlass by DropdownSetting("Blood Door Glass", Glass.RED.ordinal, Glass.options).showIf { glassBlood.value }

    @Volatile private var renderStates = emptyMap<Block, BlockState>()
    private const val doorStep = DungeonScanner.roomSize / 2

    override fun init() {
        register<WorldChangeEvent> {
            invalidateDoors(renderStates, emptyMap())
            checker.register()
        }

        register<DungeonEvent.TileScannedEvent> {
            if (event.tile !is DoorTile) return@register
            val aabb = event.tile.aabb
            mc.levelRenderer.setBlocksDirty(
                aabb.minX.toInt(),
                aabb.minY.toInt(),
                aabb.minZ.toInt(),
                aabb.maxX.toInt(),
                aabb.maxY.toInt(),
                aabb.maxZ.toInt()
            )
        }
    }

    val checker = EventBus.listener<MainThreadPacketReceivedEvent.Pre> {
        if (event.packet !is ClientboundPlayerPositionPacket) return@listener
        val pos = mc.level?.respawnData?.pos() ?: return@listener
        if (pos.x != 0 || pos.y != 100 || pos.z != 0) return@listener
        updateRenderStates()
        listener.unregister()
    }

    private fun updateRenderStates() {
        val next = if (enabled) buildMap {
            for (type in DoorType.entries) type.getGlass()?.let { put(type.source, it) }
        }
        else emptyMap()
        val previous = renderStates
        if (previous == next) return
        renderStates = next
        invalidateDoors(previous, next)
    }

    private fun invalidateDoors(previous: Map<Block, BlockState>, next: Map<Block, BlockState>) {
        if (! LocationUtils.inDungeon) return

        for (row in 0 .. 10) for (column in 0 .. 10) {
            if ((row + column) and 1 == 0) continue

            val x = DungeonScanner.startX + column * doorStep
            val z = DungeonScanner.startZ + row * doorStep

            for (pos in BlockPos.betweenClosed(x - 1, 69, z - 1, x + 1, 72, z + 1)) {
                val block = WorldUtils.getBlockAt(pos)
                if (previous[block] == next[block]) continue
                mc.levelRenderer.setBlocksDirty(x - 1, 69, z - 1, x + 1, 72, z + 1)
                break
            }
        }
    }

    @JvmStatic
    fun getRenderState(x: Int, y: Int, z: Int, original: BlockState): BlockState {
        if (! enabled) return original
        val glass = renderStates[original.block] ?: return original
        if (! LocationUtils.inDungeon) return original
        if (LocationUtils.inBoss) return original
        if (y !in 69 .. 72) return original

        val gridX = x - DungeonScanner.startX + 1
        val gridZ = z - DungeonScanner.startZ + 1

        if (gridX !in 0 .. 10 * doorStep + 2) return original
        if (gridZ !in 0 .. 10 * doorStep + 2) return original
        if (gridX % doorStep > 2) return original
        if (gridZ % doorStep > 2) return original
        if ((gridX / doorStep + gridZ / doorStep) and 1 == 0) return original

        return glass
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
        WHITE("White", Blocks.STAINED_GLASS.white()),
        BLACK("Black", Blocks.STAINED_GLASS.black()),
        CYAN("Cyan", Blocks.STAINED_GLASS.cyan()),
        LIGHT_BLUE("Light Blue", Blocks.STAINED_GLASS.lightBlue()),
        RED("Red", Blocks.STAINED_GLASS.red()),
        PINK("Pink", Blocks.STAINED_GLASS.pink()),
        ORANGE("Orange", Blocks.STAINED_GLASS.orange()),
        MAGENTA("Magenta", Blocks.STAINED_GLASS.magenta()),
        YELLOW("Yellow", Blocks.STAINED_GLASS.yellow()),
        LIME("Lime", Blocks.STAINED_GLASS.lime()),
        GRAY("Gray", Blocks.STAINED_GLASS.gray()),
        LIGHT_GRAY("Light Gray", Blocks.STAINED_GLASS.lightGray()),
        PURPLE("Purple", Blocks.STAINED_GLASS.purple()),
        BLUE("Blue", Blocks.STAINED_GLASS.blue()),
        BROWN("Brown", Blocks.STAINED_GLASS.brown()),
        GREEN("Green", Blocks.STAINED_GLASS.green());

        val state = block.defaultBlockState()

        companion object {
            val options = Glass.entries.map(Glass::displayName)
        }
    }
}
//#endif