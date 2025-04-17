package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.core.map.RoomState.*
import noammaddons.features.dungeons.dmap.core.map.RoomType.*
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf

object RoomAlerts: Feature() {
    @SubscribeEvent
    fun onRoomStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (DungeonUtils.thePlayer !in event.roomPlayers) return
        if (! event.room.data.type.equalsOneOf(NORMAL, PUZZLE, RARE, TRAP)) return
        if (event.room.data.type == PUZZLE && event.room.data.name != "Blaze") return

        when (event.newState) {
            CLEARED -> if (config.roomClearedAlert) alert("Cleared")
            GREEN -> if (config.roomSecretsDoneAlert) alert(
                if (event.room.data.secrets > 0) "&aSecrets Done!"
                else "&aCleared"
            )

            else -> return
        }
    }

    private fun alert(msg: String) {
        ChatUtils.showTitle(msg)
        SoundUtils.Pling()
    }
}

