package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage

object AutoRefillEnderPearls: Feature() {
    @SubscribeEvent
    fun refillEnderPearls(event: Chat) {
        if (! config.refillEnderPearls) return
        if (event.component.noFormatText != "Starting in 1 second.") return

        val inventory = mc.thePlayer?.inventory?.mainInventory ?: return
        val enderPearls = inventory.find { it?.displayName?.removeFormatting() == "Ender Pearl" }
        val currentAmount = enderPearls?.stackSize ?: return sendChatMessage("/gfs ender_pearl 16")
        if (enderPearls.stackSize >= 16) return
        sendChatMessage("/gfs ender_pearl ${16 - currentAmount}")
    }
}
