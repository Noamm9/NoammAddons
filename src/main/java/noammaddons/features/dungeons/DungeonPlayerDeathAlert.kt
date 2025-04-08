package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils
import noammaddons.utils.SoundUtils

object DungeonPlayerDeathAlert: Feature() {
    @SubscribeEvent
    fun onPlayerDeath(event: DungeonEvent.PlayerDeathEvent) {
        if (config.DungeonPlayerDeathAlert) {
            val sound = if (event.name == mc.session.username) SoundUtils::bigFart else SoundUtils::smallFart
            repeat(5) { sound.invoke() }
        }
        if (config.DungeonPlayerDeathAlertSendMessage) {
            val msg = config.DungeonPlayerDeathMessage
                .replace("{name}", event.name)
                .replace("{reason}", event.reason)
            ChatUtils.sendPartyMessage(msg)
        }
    }
}
