package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.GuiUtils.clickSlot
import noammaddons.utils.GuiUtils.isInGui
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.PlayerUtils.closeScreen
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.ItemUtils.SkyblockID

object AutoPotion {
    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onDungeonStart(event: Chat) {
        if (!config.AutoPotion) return
        if (!event.component.unformattedText.removeFormatting()
			.matches(Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+"))
		) return
	    if (hasPotion()) return


        GlobalScope.launch { getPotion() }
    }


	private fun hasPotion(): Boolean {
		return mc.thePlayer?.inventory?.mainInventory?.any {
			it?.displayName?.removeFormatting()
				?.toLowerCase()?.contains("Dungeon VII Potion") ?: false
		} == true
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
