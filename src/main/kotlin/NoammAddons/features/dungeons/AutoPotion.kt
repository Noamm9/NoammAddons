package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.sendChatMessage
import NoammAddons.utils.GuiUtils.clickSlot
import NoammAddons.utils.GuiUtils.isInGui
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.utils.PlayerUtils.closeScreen
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoPotion {
    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onDungeonStart(event: Chat) {
        if (!config.AutoPotion) return
        if (!event.component.unformattedText.removeFormatting()
			.matches(Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+"))
		) return

        GlobalScope.launch { getPotion() }
    }


    private suspend fun getPotion() {
        closeScreen()
        sendChatMessage(config.AutoPotionCommand.removeFormatting())

        while (!isInGui()) { delay(50) }
        delay(250)

        val container = mc.thePlayer.openContainer ?: return

        val slotCount = container.inventory.size - 36
        for (i in 0 until slotCount) {
            val item = container.inventory[i] ?: continue
            if (item.getItemId() == 373) {
                clickSlot(i, true)
                delay(250)

                closeScreen()
                return
            }
        }

        modMessage("&cNo potion found in the Potion Bag")
        closeScreen()
        return
    }
}
