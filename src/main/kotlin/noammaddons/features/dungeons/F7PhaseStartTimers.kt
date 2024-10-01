package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
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
    fun onPhaseStart(event: Chat) {
        if (!config.F7M7PhaseStartTimers) return
        val msg = event.component.unformattedText.removeFormatting()

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
    fun onRender(event: RenderOverlay) {
        if (!config.F7M7PhaseStartTimers) return
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


