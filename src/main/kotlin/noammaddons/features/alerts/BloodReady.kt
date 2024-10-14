package noammaddons.features.alerts

import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.PlayerUtils.Player


object BloodReady {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (!config.bloodReadyNotify || !inDungeons) return
        if (stripControlCodes(event.component.unformattedText).equalsOneOf(
                "[BOSS] The Watcher: That will be enough for now.",
                "[BOSS] The Watcher: You have proven yourself. You may pass."
            )) {
	        Player!!.playSound("random.orb", 1f, 0.5f)
            showTitle("§1[§6§kO§r§1] §dB§bl§do§bo§dd §bD§do§bn§de §1[§6§kO§r§1]")
        }
    }
}
