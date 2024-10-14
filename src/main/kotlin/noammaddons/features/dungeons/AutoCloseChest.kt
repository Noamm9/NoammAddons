package noammaddons.features.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.PacketEvent
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons

object AutoCloseChest {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet !is S2DPacketOpenWindow || !inDungeons || inBoss) return
        if (!config.autoCloseSecretChests) return
	    if (event.packet.windowTitle.formattedText.equalsOneOf("Chest§r", "Large Chest§r")
	        && event.packet.slotCount.equalsOneOf(27, 54)
		) {
			event.isCanceled = true
		    mc.netHandler.addToSendQueue(C0DPacketCloseWindow(event.packet.windowId))
		}
    }
}
