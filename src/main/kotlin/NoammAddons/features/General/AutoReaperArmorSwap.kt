package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.sendChatMessage
import NoammAddons.utils.GuiUtils.isInGui
import NoammAddons.utils.GuiUtils.sendClickWindowPacket
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.utils.PlayerUtils.closeScreen
import NoammAddons.utils.PlayerUtils.toggleSneak
import NoammAddons.utils.RenderUtils.drawText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoReaperArmorSwap {
    private var PreviousArmorSlot = 0
    private var lastTrigger: Long = 0L

    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun autoReaperArmorSwap(event: Chat) {
        if (!config.AutoReaperArmorSwap) return
        if (event.component.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?") return

        GlobalScope.launch {
            delay(6700)
            reaperSwap()
        }
    }

    suspend fun reaperSwap() {
        if ((System.currentTimeMillis() - lastTrigger) < 5000) return
        lastTrigger = System.currentTimeMillis()

        closeScreen()
        sendChatMessage("/wd")

        while (!isInGui()) { delay(50) }
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

        delay(200)
        sendClickWindowPacket(reaperArmorSlot, 0, 0)

        delay(250)
        closeScreen()
        delay(200)

        toggleSneak(true)
        delay(100)
        toggleSneak(false)

        sendChatMessage("/wd")
        while (!isInGui()) { delay(50) }
        delay(250)

        sendClickWindowPacket(PreviousArmorSlot, 0, 0)
        delay(500)

        closeScreen()
        PreviousArmorSlot = 0
    }


    @SubscribeEvent
    fun test(event:GuiScreenEvent.DrawScreenEvent.Post) {
        if (mc.thePlayer == null) return
        if (!config.DevMode) return
        mc.thePlayer.openContainer.inventorySlots.forEach {
            drawText(
                it.slotNumber.toString(),
                it.xDisplayPosition.toDouble(),
                it.yDisplayPosition.toDouble(),
            )
        }
    }
}
