package noammaddons.features.impl.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.TextInputSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils
import noammaddons.utils.SoundUtils

object DungeonPlayerDeath: Feature() {
    private val sound = ToggleSetting("Death Alert", true).register1()
    private val sendMessage = ToggleSetting("Send Message", true).register1() as ToggleSetting
    private val message = TextInputSetting("Message", "{name} died! {reason}").addDependency(sendMessage).register1()


    @SubscribeEvent
    fun onPlayerDeath(event: DungeonEvent.PlayerDeathEvent) {
        if (sound.value) {
            val deathSound = if (event.name == mc.session.username) SoundUtils::bigFart else SoundUtils::smallFart
            repeat(5) { deathSound.invoke() }
        }
        if (sendMessage.value) {
            val msg = message.value
                .replace("{name}", event.name)
                .replace("{reason}", event.reason)
            ChatUtils.sendPartyMessage(msg)
        }
    }
}
