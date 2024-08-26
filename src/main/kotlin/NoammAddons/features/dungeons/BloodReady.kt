package NoammAddons.features.dungeons

import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.RenderUtils.drawText
import NoammAddons.utils.ChatUtils.equalsOneOf

object BloodReady {

    private var bloodReady = false
    private var timer: Long = 0

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!config.bloodReadyNotify || !inDungeons) return
        if (!bloodReady && stripControlCodes(event.message.unformattedText).equalsOneOf(
                "[BOSS] The Watcher: That will be enough for now.",
                "[BOSS] The Watcher: You have proven yourself. You may pass."
            )
        ) {
            mc.thePlayer.playSound("random.orb", 1f, 0.5.toFloat())
            timer = System.currentTimeMillis() + 1500
            bloodReady = true
        }
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !config.bloodReadyNotify || !inDungeons || mc.ingameGUI == null) return
        if (timer > System.currentTimeMillis()) {
            val sr = ScaledResolution(mc)
            drawText(
                "§1[§6§kO§r§1] §dB§bl§do§bo§dd §bD§dovbn§de §1[§6§kO§r§1]",
                (sr.scaledWidth / 2 - mc.fontRendererObj.getStringWidth("Blood Spawned") * 2).toDouble(),
                (sr.scaledHeight / 4).toDouble(),
                4.0
            )
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load?) {
        if (config.bloodReadyNotify) bloodReady = false
    }
}
