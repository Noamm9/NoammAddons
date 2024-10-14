package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import java.awt.Color

object M7RelicSpawnTimer {
    private var StartTimer = false
    private var ticks = 0
    
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onPacket(event: ServerTick) {
        if (!config.M7RelicSpawnTimer) return
        if (!StartTimer) return
        if (ticks <= 0) return

        ticks--
    }
    
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (event.component.unformattedText.removeFormatting() != "[BOSS] Necron: All this, for nothing...") return
        
        ticks = 50
        StartTimer = true
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun drawTimer(event: RenderOverlay) {
        if (!config.M7RelicSpawnTimer) return
        if (ticks <= 0) return
        
        drawCenteredText(
            (ticks / 20.0).toFixed(2),
            mc.getWidth() / 2f,
            mc.getHeight() * 0.4f,
            3f, thePlayer?.clazz?.color ?: Color.WHITE,
        )
    }
}
