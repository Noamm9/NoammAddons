package NoammAddons.features.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.ReceivePacketEvent
import NoammAddons.utils.LocationUtils.inDungeons

object AutoCloseChest {
    @SubscribeEvent
    fun onPacket(event: ReceivePacketEvent) {
        if (event.packet !is S2DPacketOpenWindow || !inDungeons) return
        if (config.autoCloseSecretChests) {
            if (event.packet.windowTitle.unformattedText == "Chest") {
                event.isCanceled = true
                mc.netHandler.networkManager.sendPacket(C0DPacketCloseWindow((event.packet.windowId)))
            }
        }
    }
}
