package com.github.noamm9.utils.dungeons.map.utils

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Shortcuts
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.core.RoomData
import com.github.noamm9.utils.dungeons.map.core.RoomTile
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner.startX
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner.startZ
import com.github.noamm9.utils.location.LocationUtils.inDungeon
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.util.IdentityHashMap
import kotlin.math.round

object ScanUtils: ISelfInit, ICommandProvider, Shortcuts {
    private const val CORE_HEIGHT = 129
    private val ignoredCoreBlocks = setOf(
        "minecraft:chest", "minecraft:trapped_chest",
        "minecraft:piston_head", "minecraft:moving_piston",
        "minecraft:water", "minecraft:lava",
        "minecraft:fire", "minecraft:soul_fire",
    )
    private val coreTokenCache = IdentityHashMap<Block, String>()

    val roomList = DataDownloader.loadJson<List<RoomData>>("rooms-modern.json")
    val secretMap = roomList.associate { it.name to it.secretCoords }
    var currentRoom: UniqueRoom? = null
    var lastKnownRoom: UniqueRoom? = null

    override fun init() {
        EventBus.register<WorldChangeEvent> {
            currentRoom = null
            lastKnownRoom = null
        }

        EventBus.register<TickEvent.End> {
            if (! inDungeon) return@register
            val room = getRoomFromPos(player.position())
            if (currentRoom == room) return@register

            lastKnownRoom = currentRoom
            currentRoom = room

            lastKnownRoom?.let { EventBus.post(DungeonEvent.RoomEvent.onExit(it)) }
            currentRoom?.let { EventBus.post(DungeonEvent.RoomEvent.onEnter(it)) }
        }
    }

    override fun CommandBuilder.command() {
        setName("relative")
        runs {
            val look = PlayerUtils.getSelectionBlock() !!
            val room = currentRoom !!
            getRelativeCoord(look, room.clayPos !!, room.rotation !!).let {
                ChatUtils.modMessage("relative: $it")
            }
        }
    }

    fun getRoomData(hash: Int) = roomList.find { hash in it.cores }
    fun getRoomData(name: String) = roomList.find { it.name == name }

    fun getRoomGraf(pos: Vec3): Pair<Int, Int> {
        val roomIndexX = round((pos.x - startX) / DungeonScanner.roomSize).toInt()
        val roomIndexZ = round((pos.z - startZ) / DungeonScanner.roomSize).toInt()
        val gridX = roomIndexX * 2
        val gridZ = roomIndexZ * 2
        return gridX.coerceIn(0, 10) to gridZ.coerceIn(0, 10)
    }

    fun getRoomFromPos(vec: Vec3): UniqueRoom? {
        val (gx, gz) = getRoomGraf(vec)
        val unq = (DungeonScanner.dungeonList[gz * 11 + gx] as? RoomTile)?.uniqueRoom
        return unq
    }

    fun getCore(x: Int, z: Int): Int {
        val blocks = ArrayList<String>(CORE_HEIGHT)
        val pos = BlockPos.MutableBlockPos(x, 0, z)

        for (y in 140 downTo 12) {
            val block = WorldUtils.getStateAt(pos.setY(y)).block
            blocks += coreTokenCache.getOrPut(block) {
                val name = BuiltInRegistries.BLOCK.getKey(block).toString()
                if (name in ignoredCoreBlocks || name.endsWith("_planks")) "minecraft:air" else name
            }
        }
        return murmur3(blocks)
    }

    private fun murmur3(blocks: List<String>): Int {
        var h = 0x9747b28c.toInt()
        for ((index, block) in blocks.withIndex()) {
            var k = block.hashCode() xor (index * 0x1b873593)
            k *= 0xcc9e2d51.toInt()
            k = (k shl 15) or (k ushr 17)
            k *= 0x1b873593
            h = h xor k
            h = (h shl 13) or (h ushr 19)
            h = h * 5 + 0xe6546b64.toInt()
        }
        h = h xor (blocks.size * 4)
        h = h xor (h ushr 16)
        h *= 0x85ebca6b.toInt()
        h = h xor (h ushr 13)
        h *= 0xc2b2ae35.toInt()
        return h xor (h ushr 16)
    }

    fun getHighestY(x: Int, z: Int): Int {
        val pos = BlockPos.MutableBlockPos(x, 0, z)
        var height = 0

        for (y in 256 downTo 0) {
            val blockState = WorldUtils.getStateAt(pos.setY(y))
            if (blockState.isAir || blockState.block == Blocks.GOLD_BLOCK) continue

            height = y
            break
        }

        return height
    }

    fun BlockPos.rotate(degree: Int): BlockPos {
        return when ((degree % 360 + 360) % 360) {
            0 -> BlockPos(x, y, z)
            90 -> BlockPos(z, y, - x)
            180 -> BlockPos(- x, y, - z)
            270 -> BlockPos(- z, y, x)
            else -> BlockPos(x, y, z)
        }
    }

    fun getRealCoord(pos: BlockPos, roomCenter: BlockPos, rotation: Int): BlockPos {
        val (cx, _, cz) = roomCenter.destructured()
        return pos.rotate(rotation).add(cx, 0, cz)
    }

    fun getRelativeCoord(realPos: BlockPos, roomCorner: BlockPos, rotation: Int): BlockPos {
        val (cx, _, cz) = roomCorner.destructured()
        val centeredPos = realPos.add(- cx, 0, - cz)
        return centeredPos.rotate(- rotation)
    }
}