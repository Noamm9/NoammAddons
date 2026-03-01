package com.github.noamm9.utils.dungeons.map.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventDispatcher
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.core.RoomData
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner.startX
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner.startZ
import com.github.noamm9.utils.location.LocationUtils.inDungeon
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.round

object ScanUtils {
    val roomList by lazy { DataDownloader.loadJson<List<RoomData>>("rooms.json") }

    init {
        register<WorldChangeEvent> {
            currentRoom = null
            lastKnownRoom = null
        }

        ThreadUtils.loop(250) {
            if (! inDungeon) return@loop
            ThreadUtils.scheduledTask {

                val room = getRoomFromPos(mc.player?.position() ?: return@scheduledTask)
                if (currentRoom == room) return@scheduledTask

                lastKnownRoom = currentRoom
                currentRoom = room

                EventDispatcher.checkForRoomChange(currentRoom, lastKnownRoom)
            }
        }
    }

    @JvmField
    var currentRoom: UniqueRoom? = null

    @JvmField
    var lastKnownRoom: UniqueRoom? = null

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
        val unq = (DungeonInfo.dungeonList[gz * 11 + gx] as? Room)?.uniqueRoom
        return unq
    }

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)

        for (y in 140 downTo 12) {
            val id = LegacyRegistry.getLegacyId(WorldUtils.getStateAt(BlockPos(x, y, z)))
            if (id.equalsOneOf(5, 54, 146)) continue
            sb.append(id)
        }
        return sb.toString().hashCode()
    }

    fun getHighestY(x: Int, z: Int): Int {
        var height = 0

        for (idx in 256 downTo 0) {
            val blockState = WorldUtils.getStateAt(x, idx, z)
            val block = blockState?.block ?: continue
            if (blockState.isAir || block == Blocks.GOLD_BLOCK) continue

            height = idx
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