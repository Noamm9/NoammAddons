package noammaddons.features.dungeons.terminals

import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ThreadUtils.setTimeout

object MelodyAlert {
	private val msg get() = config.MelodyAlert.removeFormatting()
	private val inMelody get() = currentChestName.removeFormatting() == "Click the button on time!"
	
    private var claySlots = mutableMapOf(
	    25 to "/pc $msg 1/4",
	    34 to "/pc $msg 2/4",
	    43 to "/pc $msg 3/4"
    )

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onGuiOpened(event: GuiOpenEvent) {
        if (!inMelody) return
        if (msg.isEmpty()) return
        if (F7Phase != 3) return

        claySlots = mutableMapOf(
            25 to "/pc $msg 1/4",
            34 to "/pc $msg 2/4",
            43 to "/pc $msg 3/4"
        )

        setTimeout(100) {
            if (inMelody) {
				sendChatMessage("/pc $msg")
            }
        }
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onTick(e: Tick) {
        if (!inMelody) return
        if (msg.isEmpty()) return
        if (F7Phase != 3) return

        val greenClays = claySlots.keys.filter {
	        Player!!.openContainer?.getSlot(it)?.stack?.metadata == 5
        }

        if (greenClays.isEmpty()) return

        val lastClay = greenClays.last()
        sendChatMessage(claySlots[lastClay] ?: "")
        greenClays.forEach{ claySlots.remove(it) }
    }
}
