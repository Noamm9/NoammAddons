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
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils.inDungeon
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.round

object ScanUtils: ISelfInit, ICommandProvider, Shortcuts {
    val roomList = DataDownloader.loadJson<List<RoomData>>("rooms.json")
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
            val room = currentRoom ?: return@runs
            ChatUtils.chat(getRelativeCoord(PlayerUtils.getSelectionBlock() !!, room.clayPos !!, room.rotation !!))
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
        val sb = StringBuilder(150)
        val pos = BlockPos.MutableBlockPos(x, 0, z)

        for (y in 140 downTo 12) {
            val id = LegacyRegistry.getLegacyId(WorldUtils.getStateAt(pos.setY(y)))
            if (id.equalsOneOf(5, 54, 146)) continue
            sb.append(id)
        }
        return sb.toString().hashCode()
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