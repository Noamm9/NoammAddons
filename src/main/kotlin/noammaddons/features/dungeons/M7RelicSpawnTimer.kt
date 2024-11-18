package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import java.awt.Color

object M7RelicSpawnTimer: Feature() {
    private var StartTimer = false
    private var ticks = 0

    @SubscribeEvent
    fun onPacket(event: ServerTick) {
        if (! config.M7RelicSpawnTimer) return
        if (! StartTimer) return
        if (ticks <= 0) return

        ticks --
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (event.component.unformattedText.removeFormatting() != "[BOSS] Necron: All this, for nothing...") return

        ticks = 50
        StartTimer = true
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (! config.M7RelicSpawnTimer) return
        if (ticks <= 0) return

        drawCenteredText(
            (ticks / 20.0).toFixed(2),
            mc.getWidth() / 2f,
            mc.getHeight() * 0.4f,
            3f, thePlayer?.clazz?.color ?: Color.WHITE,
        )
    }
}
