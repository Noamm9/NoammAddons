package noammaddons.features.impl.dungeons.waypoints

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.RenderUtils
import noammaddons.utils.ScanUtils
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object SecretsWaypoints {
    private data class SecretWaypoint(val pos: BlockPos, val type: String, val clicked: Boolean = false) {
        val color = when (type) {
            "REDSTONE_KEY" -> Color.RED
            "WITHER_ESSANCE" -> Color.BLACK
            else -> Color.MAGENTA
        }
    }

    private val waypoints = ScanUtils.roomList.associate { it.name to it.secretCoords }
    private val currentRoomWaypoints: CopyOnWriteArrayList<SecretWaypoint> = CopyOnWriteArrayList()

    fun onRoomEnter(room: Room) {
        if (! DungeonWaypoints.secretWaypoints) return
        currentRoomWaypoints.clear()
        if (room.rotation == null) return

        val roomRotation = 360 - room.rotation !!
        val roomCorner = room.corner !!
        val roomName = room.data.name

        waypoints[roomName]?.let {
            val roomWaypoints = mutableListOf<SecretWaypoint>()
            it.redstoneKey.forEach { roomWaypoints.add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), "REDSTONE_KEY")) }
            it.wither.forEach { roomWaypoints.add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), "WITHER_ESSANCE")) }
            it.bat.forEach { roomWaypoints.add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), "BAT")) }
            it.item.forEach { roomWaypoints.add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), "ITEM")) }
            it.chest.forEach { roomWaypoints.add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), "CHEST")) }
            currentRoomWaypoints.addAll(roomWaypoints)
        }
    }

    fun onRenderWorld() {
        if (! DungeonWaypoints.secretWaypoints) return
        if (currentRoomWaypoints.isEmpty()) return

        for (waypoint in currentRoomWaypoints) {
            if (waypoint.type == "REDSTONE_KEY" && getBlockAt(waypoint.pos) != Blocks.skull) continue

            RenderUtils.drawBlockBox(
                waypoint.pos, waypoint.color,
                fill = false, outline = true,
                phase = true
            )
        }
    }

    fun onWorldUnload() {
        currentRoomWaypoints.clear()
    }
}