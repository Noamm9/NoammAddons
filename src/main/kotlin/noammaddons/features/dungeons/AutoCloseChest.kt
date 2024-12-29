package noammaddons.features.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send

object AutoCloseChest: Feature() {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet !is S2DPacketOpenWindow || ! inDungeons || inBoss) return
        if (! config.autoCloseSecretChests) return
        if (event.packet.windowTitle.formattedText.equalsOneOf("Chest§r", "Large Chest§r")
            && event.packet.slotCount.equalsOneOf(27, 54)
        ) {
            event.isCanceled = true
            C0DPacketCloseWindow(event.packet.windowId).send()
        }
    }
}
