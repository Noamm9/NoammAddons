package noammaddons.features.impl.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.core.map.RoomState
import noammaddons.features.impl.dungeons.dmap.core.map.RoomType
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf

object RoomAlerts: Feature() {
    private val clear by ToggleSetting("Cleared", true)
    private val secrets by ToggleSetting("Secrets Done", true)

    @SubscribeEvent
    fun onRoomStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (DungeonUtils.thePlayer !in event.roomPlayers) return
        if (! event.room.data.type.equalsOneOf(RoomType.NORMAL, RoomType.PUZZLE, RoomType.RARE, RoomType.TRAP)) return
        if (event.room.data.type == RoomType.PUZZLE && event.room.data.name != "Blaze") return

        when (event.newState) {
            RoomState.CLEARED -> if (clear) {
                alert((if (event.room.data.secrets == 0) "&a" else "") + "Cleared")
            }

            RoomState.GREEN -> if (secrets && event.room.data.secrets > 0) {
                alert("&aSecrets Done!")
            }

            else -> return
        }
    }

    private fun alert(msg: String) {
        ChatUtils.showTitle(msg)
        SoundUtils.Pling()
    }
}

