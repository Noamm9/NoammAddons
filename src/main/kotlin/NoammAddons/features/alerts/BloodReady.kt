package NoammAddons.features.alerts

import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.ChatUtils.equalsOneOf
import NoammAddons.utils.ChatUtils.showTitle


object BloodReady {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (!config.bloodReadyNotify || !inDungeons) return
        if (stripControlCodes(event.component.unformattedText).equalsOneOf(
                "[BOSS] The Watcher: That will be enough for now.",
                "[BOSS] The Watcher: You have proven yourself. You may pass."
            )) {
            mc.thePlayer.playSound("random.orb", 1f, 0.5.toFloat())
            showTitle("§1[§6§kO§r§1] §dB§bl§do§bo§dd §bD§do§bn§de §1[§6§kO§r§1]")
        }
    }
}
