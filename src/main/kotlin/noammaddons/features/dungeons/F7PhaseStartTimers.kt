package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import java.awt.Color


object F7PhaseStartTimers: Feature() {
    private var tickTime = 1L
    private val startMessages = listOf(
        "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!",
        "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!",
        "[BOSS] Storm: I should have known that I stood no chance.",
        "[BOSS] Necron: I'm afraid, your journey ends now."
    )

    @SubscribeEvent
    fun onPhaseStart(event: Chat) {
        if (! config.F7M7PhaseStartTimers) return
        val msg = event.component.unformattedText.removeFormatting()

        when {
            msg == startMessages[0] && config.P1StartTimer -> tickTime = 7500 / 50
            msg == startMessages[1] && config.P2StartTimer -> tickTime = 6000 / 50
            msg == startMessages[2] && config.P3StartTimer -> tickTime = 5200 / 50
            msg == startMessages[3] && config.P4StartTimer -> tickTime = 3000 / 50
        }
    }

    @SubscribeEvent
    fun timer(event: ServerTick) {
        tickTime --
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.F7M7PhaseStartTimers) return
        if (! inBoss) return
        if (dungeonFloor != 7) return
        val timeLeft = (tickTime * 50.0) / 1000
        if (tickTime <= 0) return

        drawCenteredText(
            timeLeft.toFixed(1),
            mc.getWidth() / 2f,
            mc.getHeight() / 2f + 20f,
            1.5f, Color.GREEN
        )
    }
}


