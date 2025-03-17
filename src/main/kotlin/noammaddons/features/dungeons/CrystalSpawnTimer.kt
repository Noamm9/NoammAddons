package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText

object CrystalSpawnTimer: Feature() {
    private var tickTimer: Int? = null
    private val messages = listOf(
        "[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!",
        "[BOSS] Maxor: YOU TRICKED ME!"
    )

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.CrystalSpawnTimer) return
        if (! messages.contains(event.component.noFormatText)) return
        tickTimer = 34
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        tickTimer?.let { ticks ->
            tickTimer = when {
                ticks > 0 -> ticks - 1
                else -> null
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! config.CrystalSpawnTimer) return
        tickTimer?.let { ticks ->
            val timeLeft = "&b" + (ticks / 20.0).toFixed(2)
            drawCenteredText(
                timeLeft,
                mc.getWidth() / 2,
                mc.getHeight() * 0.5 + 10,
                2.5f,
            )
        }
    }
}