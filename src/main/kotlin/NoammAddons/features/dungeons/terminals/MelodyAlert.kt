package NoammAddons.features.dungeons.terminals

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority

object MelodyAlert {
    private var claySlots = mutableMapOf(
        25 to "/pc ${config.MelodyAlert.removeFormatting()} 1/4",
        34 to "/pc ${config.MelodyAlert.removeFormatting()} 2/4",
        43 to "/pc ${config.MelodyAlert.removeFormatting()} 3/4"
    )

    @SubscribeEvent
    fun onGuiOpened(event: GuiOpenEvent) {
        if (GuiUtils.currentChestName != "Click the button on time!") return
        if (config.MelodyAlert.removeFormatting().isNotEmpty()) return
        if (F7Phase != 3) return

        claySlots = mutableMapOf(
            25 to "/pc ${config.MelodyAlert.removeFormatting()} 1/4",
            34 to "/pc ${config.MelodyAlert.removeFormatting()} 2/4",
            43 to "/pc ${config.MelodyAlert.removeFormatting()} 3/4"
        )

        setTimeout(100) {
            if (GuiUtils.currentChestName != "Click the button on time!") return@setTimeout
            ChatUtils.sendChatMessage("/pc ${config.MelodyAlert.removeFormatting()}")
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onStep(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (GuiUtils.currentChestName != "Click the button on time!") return
        if (config.MelodyAlert.removeFormatting().isNotEmpty()) return
        if (F7Phase != 3) return

        val greenClays = claySlots.keys.filter {
            mc.thePlayer.openContainer?.getSlot(it)?.stack?.metadata == 5
        }

        if (greenClays.isEmpty()) return

        val lastClay = greenClays.last()
        ChatUtils.sendChatMessage(claySlots[lastClay] ?: "")
        greenClays.forEach{ claySlots.remove(it) }
    }
}