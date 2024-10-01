package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.removeFormatting
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoRefillEnderPearls {
    @SubscribeEvent
    fun refillEnderPearls(event: Chat) {
        if (!config.refillEnderPearls) return
        if (event.component.unformattedText.removeFormatting() != "Starting in 1 second.") return

        val player = mc.thePlayer
        val inventory = player.inventory.mainInventory
        val enderPearls = inventory.find { it?.displayName?.removeFormatting() == "Ender Pearl" }
        val currentAmount = enderPearls?.stackSize ?: return player.sendChatMessage("/gfs ender_pearl 16")
        if (enderPearls.stackSize >= 16) return
        player.sendChatMessage("/gfs ender_pearl ${16 - currentAmount}")
    }
}
