package noammaddons.features.impl.misc

import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.DropdownSetting

object TimeChanger: Feature("Changes the world time") {
    private val TIME_VALUES = longArrayOf(1000L, 6000L, 12000L, 13000L, 18000L, 23000L)
    private val timeChangerMode by DropdownSetting("Time", listOf("Day", "Noon", "Sunset", "Night", "Midnight", "Sunrise"))

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet is S03PacketTimeUpdate) {
            mc.theWorld?.worldTime = TIME_VALUES[timeChangerMode]
            event.isCanceled = true
        }
    }
}
