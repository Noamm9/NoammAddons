package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.roundToInt

object F7PhaseStartTimers {
    private val startMessages = listOf(
        "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!",
        "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!",
        "[BOSS] Storm: I should have known that I stood no chance.",
        "[BOSS] Necron: I'm afraid, your journey ends now."
    )
    private var startTime = System.currentTimeMillis()
    private var msTime = 0


    @SubscribeEvent
    fun onPhaseStart(event: ClientChatReceivedEvent) {
        if (!config.F7M7PhaseStartTimers) return
        if (event.type.toInt() == 3) return
        val msg = event.message.unformattedText.removeFormatting()

        when {
            msg == startMessages[0] && config.P1StartTimer -> {
                startTime = System.currentTimeMillis()
                msTime = 7500
            }

            msg == startMessages[1] && config.P2StartTimer -> {
                startTime = System.currentTimeMillis()
                msTime = 6000
            }

            msg == startMessages[2] && config.P3StartTimer -> {
                startTime = System.currentTimeMillis()
                msTime = 5200
            }

            msg == startMessages[3] && config.P4StartTimer -> {
                startTime = System.currentTimeMillis()
                msTime = 3000
            }
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent.Pre) {
        if (!config.F7M7PhaseStartTimers) return
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return
        val timeLeft = (msTime - (System.currentTimeMillis() - startTime)).toFloat().roundToInt()
        if (timeLeft < 0) return

        RenderUtils.drawText(
            "&a$timeLeft",
            (mc.getWidth()/2 - mc.fontRendererObj.getStringWidth(timeLeft.toString()) *1.5 / 2),
            (mc.getHeight()/2 + 20).toDouble(),
            1.5
        )
    }
}


