package noammaddons.features.impl.dungeons.waypoints

import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.PogObject
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object DungeonWaypoints: Feature("add a custom waypoint with /dw add while looking at a block") {
    data class DungeonWaypoint(val pos: BlockPos, val color: Color, val filled: Boolean, val outline: Boolean, val phase: Boolean)

    val waypoints = PogObject<Map<String, List<DungeonWaypoint>>>("dungeonWaypoints", mapOf())
    val currentRoomWaypoints: CopyOnWriteArrayList<DungeonWaypoint> = CopyOnWriteArrayList()

    val secretWaypoints by ToggleSetting("Secret Waypoints")

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        SecretsWaypoints.onRoomEnter(event.room)
        currentRoomWaypoints.clear()

        val roomName = event.room.data.name.takeUnless { it == "Unknown" } ?: return
        val roomRotation = event.room.rotation ?: return
        val roomCorner = event.room.corner ?: return

        waypoints.getData()[roomName]?.map {
            DungeonWaypoint(
                ScanUtils.getRealCoord(it.pos, roomCorner, 360 - roomRotation),
                it.color, it.filled, it.outline, it.phase
            )
        }?.let { currentRoomWaypoints.addAll(it) }
    }

    @SubscribeEvent
    fun onBossEnter(event: DungeonEvent.BossEnterEvent) {
        SecretsWaypoints.onWorldUnload()
        currentRoomWaypoints.clear()
        waypoints.getData()["B" + LocationUtils.dungeonFloorNumber]?.let { currentRoomWaypoints.addAll(it) }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        SecretsWaypoints.onRenderWorld()
        if (currentRoomWaypoints.isEmpty()) return

        for (waypoint in currentRoomWaypoints) {
            RenderUtils.drawBox(
                waypoint.pos, waypoint.color,
                outline = waypoint.outline,
                fill = waypoint.filled,
                1f, 1f, phase = waypoint.phase
            )
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        SecretsWaypoints.onWorldUnload()
        currentRoomWaypoints.clear()
    }

    fun saveWaypoint(absPos: BlockPos, relPos: BlockPos, roomName: String, color: Color, filled: Boolean, outline: Boolean, phase: Boolean) {
        val newWaypoint = DungeonWaypoint(relPos, color, filled, outline, phase)

        val currentData = waypoints.getData().toMutableMap()
        val roomList = currentData.getOrDefault(roomName, emptyList()).toMutableList()
        val wasReplaced = roomList.removeIf { it.pos == relPos }

        roomList.add(newWaypoint)
        currentData[roomName] = roomList

        waypoints.setData(currentData)
        waypoints.save()

        currentRoomWaypoints.removeIf { it.pos == absPos }

        val absoluteWaypoint = DungeonWaypoint(absPos, color, filled, outline, phase)
        currentRoomWaypoints.add(absoluteWaypoint)

        if (wasReplaced) modMessage("§e$roomName: Waypont updated at ${absPos.x}, ${absPos.y}, ${absPos.z}.")
        else modMessage("§a$roomName: Waypoint added at ${absPos.x}, ${absPos.y}, ${absPos.z}.")
    }
}