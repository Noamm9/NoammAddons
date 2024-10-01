package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.GuiUtils.isInGui
import noammaddons.utils.GuiUtils.sendClickWindowPacket
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.RenderUtils.drawText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.MessageSentEvent
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.DungeonUtils

object AutoReaperArmorSwap {
    private var PreviousArmorSlot = 0
    private var lastTrigger: Long = 0L

    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun autoReaperArmorSwap(event: Chat) {
        if (!config.AutoReaperArmorSwap) return
        if (event.component.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?") return
	    if (listOf("Archer", "Berserk").contains(DungeonUtils.thePlayer?.clazz?.name)) {
		    GlobalScope.launch {
			    delay(6700)
			    reaperSwap()
		    }
	    }
    }

    suspend fun reaperSwap() {
        if ((System.currentTimeMillis() - lastTrigger) < 25_000) return modMessage(
	        "&a[Ras] &bon Cooldown try again in &c${25_000 - (System.currentTimeMillis() - lastTrigger)}ms"
		)
	    lastTrigger = System.currentTimeMillis()

        closeScreen()
        sendChatMessage("/wd")

        while (!isInGui()) { delay(1) }
        delay(250)

        val container = mc.thePlayer.openContainer.inventory
        val reaperArmorSlot: Int = config.AutoReaperArmorSlot + 35

        for (i in 35 until 45) {
            val item = container[i] ?: continue
            if (item.getItemId() == 351 && item.metadata == 10) {
                PreviousArmorSlot = i
            }
        }

        if (PreviousArmorSlot == 0) {
            modMessage("&a[Ras] &cPrevious Armor Slot not found")
            closeScreen()
            return
        }
	    
        sendClickWindowPacket(reaperArmorSlot, 0, 0)

        delay(100)
        closeScreen()
        delay(200)

        toggleSneak(true)
        delay(100)
        toggleSneak(false)

        sendChatMessage("/wd")
        while (!isInGui()) { delay(1) }
        delay(250)

        sendClickWindowPacket(PreviousArmorSlot, 0, 0)
        delay(100)

        closeScreen()
        PreviousArmorSlot = 0
    }
}
