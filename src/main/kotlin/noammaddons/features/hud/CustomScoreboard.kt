package noammaddons.features.hud

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.RenderScoreBoardEvent
import noammaddons.utils.ChatUtils
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.ScoreboardUtils
import noammaddons.utils.ThreadUtils.setTimeout
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScoreboardUtils.cleanSB
import java.awt.Color
import kotlin.math.max


object CustomScoreboard {
    private val darkMode = Color(33, 33, 33, 180)
    private var customScoreboard = mutableListOf<String>()
    private var width = 0f
    private var text = ""
    private var loading = true
	
	
    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        loading = true
        setTimeout(500) { loading = false }
    }

	
    @SubscribeEvent
    fun onStepEvent(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (loading || !config.CustomScoreboard) return

        width = 0f
        customScoreboard.clear()
	    
        customScoreboard.addAll(ScoreboardUtils.sidebarLines.reversed().filterNot { cleanSB(it).contains("www.hypixel.net") })

        width = max(width, customScoreboard.maxOfOrNull { mc.fontRendererObj.getStringWidth(it) + 10f } ?: 0f)
	    
        text = (customScoreboard.joinToString("\n"))
    }

    @SubscribeEvent
    fun onRenderScoreboard(event: RenderScoreBoardEvent) {
        if (!config.CustomScoreboard) return
        event.isCanceled = true

        val screenWidth = mc.getWidth()
        val screenHeight = mc.getHeight()
        val textHeight = mc.fontRendererObj.FONT_HEIGHT *text.split("\n").size

        GlStateManager.pushMatrix()

        RenderUtils.drawRoundedRect(
            darkMode.darker(),
            screenWidth - width * 1.05f,
            (screenHeight / 2) - (textHeight / 2) * 1.05f,
            width * 1.05f,
            textHeight * 1.05f + 5
        )

        RenderUtils.drawRoundedRect(
            darkMode,
            screenWidth - width * 1.025f,
            (screenHeight / 2f) - (textHeight / 2),
            width,
            textHeight + 5f
        )
	    
	    drawText(
		    text,
		    screenWidth - width*1f,
		    screenHeight/2f - textHeight/2f
		)
	    
        GlStateManager.popMatrix()
    }
}