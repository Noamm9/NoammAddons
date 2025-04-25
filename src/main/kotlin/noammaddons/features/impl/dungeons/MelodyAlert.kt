package noammaddons.features.impl.dungeons

import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.TextInputSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.getContainerName
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.ThreadUtils.setTimeout

object MelodyAlert: Feature() {
    private val msg by TextInputSetting("Melody Messaage", "I ❤ Melody")
    private fun inMelody(guiScreen: GuiScreen? = mc.currentScreen): Boolean {
        return getContainerName(guiScreen).removeFormatting() == "Click the button on time!"
    }

    private var claySlots = mutableMapOf(
        25 to "$msg 1/4",
        34 to "$msg 2/4",
        43 to "$msg 3/4"
    )

    @SubscribeEvent
    fun onGuiOpened(event: GuiOpenEvent) {
        if (msg.isBlank()) return
        if (! inMelody(event.gui)) return
        if (F7Phase != 3) return

        claySlots = mutableMapOf(
            25 to "$msg 1/4",
            34 to "$msg 2/4",
            43 to "$msg 3/4"
        )

        setTimeout(100) {
            if (currentChestName.removeFormatting() == "Click the button on time!") {
                sendPartyMessage(msg)
            }
        }
    }

    @SubscribeEvent
    fun onTick(e: Tick) {
        if (msg.isBlank()) return
        if (! inMelody()) return
        if (F7Phase != 3) return

        val greenClays = claySlots.keys.filter {
            mc.thePlayer.openContainer?.getSlot(it)?.stack?.metadata == 5
        }

        if (greenClays.isEmpty()) return

        val lastClay = greenClays.last()
        sendPartyMessage(claySlots[lastClay] ?: return)
        greenClays.forEach { claySlots.remove(it) }
    }
}
