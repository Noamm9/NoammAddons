package noammaddons.features.impl.dungeons.waypoints

import com.google.gson.reflect.TypeToken
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import java.awt.Color
import java.io.*
import java.util.concurrent.CopyOnWriteArrayList

object DungeonWaypoints: Feature("Add a custom waypoint with /dw add while looking at a block") {
    data class DungeonWaypoint(val pos: BlockPos, val color: Color, val filled: Boolean, val outline: Boolean, val phase: Boolean)

    private val configFile = File("config/$MOD_NAME/dungeonWaypoints.json")
    val waypoints = mutableMapOf<String, List<DungeonWaypoint>>()
    val currentRoomWaypoints: CopyOnWriteArrayList<DungeonWaypoint> = CopyOnWriteArrayList()

    val secretWaypoints by ToggleSetting("Secret Waypoints")

    override fun init() {
        val reader = configFile.takeIf(File::exists)?.let(::FileReader) ?: return
        val type = object: TypeToken<MutableMap<String, List<DungeonWaypoint>>>() {}.type
        val loadedData = runCatching { JsonUtils.gsonBuilder.fromJson<MutableMap<String, List<DungeonWaypoint>>?>(reader, type) }.getOrNull()

        if (loadedData != null) {
            waypoints.clear()
            waypoints.putAll(loadedData)
        }

        reader.close()
    }

    fun saveConfig() {
        configFile.parentFile.takeUnless(File::exists)?.let(File::mkdirs)
        val writer = FileWriter(configFile)
        JsonUtils.gsonBuilder.toJson(waypoints, writer)
        writer.close()
    }

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        SecretsWaypoints.onRoomEnter(event.room)
        currentRoomWaypoints.clear()

        val roomName = event.room.name
        val roomRotation = event.room.rotation ?: return
        val roomCenter = ScanUtils.getRoomCenter(event.room.mainRoom)

        waypoints[roomName]?.map {
            DungeonWaypoint(
                ScanUtils.getRealCoord(it.pos, roomCenter, 360 - roomRotation),
                it.color, it.filled, it.outline, it.phase
            )
        }?.let { currentRoomWaypoints.addAll(it) }
    }

    @SubscribeEvent
    fun onBossEnter(event: DungeonEvent.BossEnterEvent) {
        SecretsWaypoints.onWorldUnload()
        currentRoomWaypoints.clear()
        waypoints["B" + LocationUtils.dungeonFloorNumber]?.let { currentRoomWaypoints.addAll(it) }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        SecretsWaypoints.onRenderWorld()
        if (currentRoomWaypoints.isEmpty()) return

        for (waypoint in currentRoomWaypoints) {
            RenderUtils.drawBlockBox(
                waypoint.pos, waypoint.color, outline = waypoint.outline,
                fill = waypoint.filled, phase = waypoint.phase
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

        val currentData = waypoints.toMutableMap()
        val roomList = currentData.getOrDefault(roomName, emptyList()).toMutableList()
        val wasReplaced = roomList.removeIf { it.pos == relPos }

        roomList.add(newWaypoint)
        currentData[roomName] = roomList

        waypoints.clear()
        waypoints.putAll(currentData)
        saveConfig()

        currentRoomWaypoints.removeIf { it.pos == absPos }

        val absoluteWaypoint = DungeonWaypoint(absPos, color, filled, outline, phase)
        currentRoomWaypoints.add(absoluteWaypoint)

        if (wasReplaced) modMessage("§e$roomName: Waypont updated at ${absPos.x}, ${absPos.y}, ${absPos.z}.")
        else modMessage("§a$roomName: Waypoint added at ${absPos.x}, ${absPos.y}, ${absPos.z}.")
    }
}
