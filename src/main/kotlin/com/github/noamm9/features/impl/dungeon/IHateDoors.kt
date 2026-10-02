package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
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

    private const val doorStep = DungeonScanner.roomSize / 2

    @Volatile private var renderStates: Map<Block, BlockState> = emptyMap()

    override fun init() {
        register<GameStartEvent> { updateRenderStates() }
        register<DungeonEvent.BossEnterEvent> { invalidateDoors(renderStates, emptyMap()) }

        val dungeonEntry = register<MainThreadPacketReceivedEvent.Post> {
            if (event.packet !is ClientboundSetPlayerTeamPacket || ! LocationUtils.inDungeon) return@register
            listener.unregister()
            invalidateDoors(emptyMap(), renderStates)
        }
        register<WorldChangeEvent> { dungeonEntry.register() }
    }

    override fun onEnable() {
        super.onEnable()
        updateRenderStates()
    }

    override fun onDisable() {
        super.onDisable()
        updateRenderStates()
    }

    private fun updateRenderStates() {
        val next = if (enabled) buildMap {
            for (type in DoorType.entries) type.getGlass()?.let { put(type.source, it) }
        } else emptyMap()
        val previous = renderStates
        if (previous == next) return
        renderStates = next
        invalidateDoors(previous, next)
    }

    private fun invalidateDoors(previous: Map<Block, BlockState>, next: Map<Block, BlockState>) {
        val level = mc.level ?: return
        if (! LocationUtils.inDungeon) return
        for (row in 0 .. 10) for (column in 0 .. 10) {
            if ((row + column) and 1 == 0) continue
            val x = DungeonScanner.startX + column * doorStep
            val z = DungeonScanner.startZ + row * doorStep
            for (pos in BlockPos.betweenClosed(x - 1, 69, z - 1, x + 1, 72, z + 1)) {
                val block = level.getBlockState(pos).block
                if (previous[block] == next[block]) continue
                mc.levelRenderer.setBlocksDirty(x - 1, 69, z - 1, x + 1, 72, z + 1)
                break
            }
        }
    }

    @JvmStatic
    fun getRenderState(x: Int, y: Int, z: Int, original: BlockState): BlockState {
        val glass = renderStates[original.block] ?: return original
        if (! LocationUtils.inDungeon || LocationUtils.inBoss || y !in 69 .. 72) return original

        val gridX = x - DungeonScanner.startX + 1
        val gridZ = z - DungeonScanner.startZ + 1
        if (gridX !in 0 .. 10 * doorStep + 2 || gridZ !in 0 .. 10 * doorStep + 2 ||
            gridX % doorStep > 2 || gridZ % doorStep > 2 ||
            (gridX / doorStep + gridZ / doorStep) and 1 == 0) return original
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