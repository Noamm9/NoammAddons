package noammaddons.features.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send

object AutoCloseChest: Feature() {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! config.autoCloseSecretChests) return
        if (event.packet !is S2DPacketOpenWindow) return
        if (! inDungeon || inBoss) return
        if (! event.packet.windowTitle.noFormatText.equalsOneOf("Chest", "Large Chest")) return
        if (! event.packet.slotCount.equalsOneOf(27, 54)) return
        C0DPacketCloseWindow(event.packet.windowId).send()
        event.isCanceled = true
    }
}
