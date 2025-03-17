package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.Utils.equalsOneOf


object BloodReady: Feature() {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.bloodReadyNotify || ! inDungeon) return
        if (event.component.noFormatText.equalsOneOf( // @formatter:off
           "[BOSS] The Watcher: That will be enough for now.",
           "[BOSS] The Watcher: You have proven yourself. You may pass."
        )) {
            mc.thePlayer !!.playSound("random.orb", 1f, 0.5f)
            showTitle("§1[§6§kO§r§1] §dB§bl§do§bo§dd §bD§do§bn§de §1[§6§kO§r§1]")
        }
    }
}
