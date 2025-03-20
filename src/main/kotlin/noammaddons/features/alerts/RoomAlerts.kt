package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.core.map.RoomState.*
import noammaddons.features.dungeons.dmap.core.map.RoomType.*
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf

object RoomAlerts: Feature() {
    private var text = ""

    private fun alert(msg: String) {
        text = msg
        ThreadUtils.setTimeout(2000) { text = "" }
        SoundUtils.Pling()
    }

    @SubscribeEvent
    fun onRoomStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (ScanUtils.currentRoom?.name != event.room.name) return
        if (! event.room.type.equalsOneOf(NORMAL, PUZZLE, RARE, TRAP)) return
        if (event.room.type == PUZZLE && event.room.name != "Blaze") return

        when (event.newState) {
            CLEARED -> if (config.roomClearedAlert) alert("Cleared")
            GREEN -> if (config.roomSecretsDoneAlert) alert(
                if (event.room.secrets > 0) "&aSecrets Done!"
                else "&aCleared"
            )

            else -> return
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (text.isBlank()) return
        RenderUtils.drawTitle(text, "", false)
    }
}

