package noammaddons.features.dungeons.terminals

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.ThreadUtils.setTimeout
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.GuiUtils.currentChestName

object MelodyAlert {
	private val msg get() = config.MelodyAlert.addColor().removeFormatting()
	
    private var claySlots = mutableMapOf(
	    25 to "/pc $msg 1/4",
	    34 to "/pc $msg 2/4",
	    43 to "/pc $msg 3/4"
    )

    @SubscribeEvent
    fun onGuiOpened(event: GuiOpenEvent) {
        if (currentChestName != "Click the button on time!") return
        if (msg.isEmpty()) return
        if (F7Phase != 3) return

        claySlots = mutableMapOf(
            25 to "/pc $msg 1/4",
            34 to "/pc $msg 2/4",
            43 to "/pc $msg 3/4"
        )

        setTimeout(100) {
            if (currentChestName != "Click the button on time!") return@setTimeout
            sendChatMessage("/pc $msg")
        }
    }

    @SubscribeEvent
    fun onStep(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (currentChestName != "Click the button on time!") return
        if (config.MelodyAlert.removeFormatting().isEmpty()) return
        if (F7Phase != 3) return

        val greenClays = claySlots.keys.filter {
            mc.thePlayer.openContainer?.getSlot(it)?.stack?.metadata == 5
        }

        if (greenClays.isEmpty()) return

        val lastClay = greenClays.last()
        sendChatMessage(claySlots[lastClay] ?: "")
        greenClays.forEach{ claySlots.remove(it) }
    }
}
