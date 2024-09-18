package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.RenderScoreBoardEvent
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.RenderUtils
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import NoammAddons.utils.ScoreboardUtils
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color
import kotlin.math.max


object CustomScoreboard {
    private val darkMode = Color(33, 33, 33, 180)
    private var customScoreboard = mutableListOf<String>()
    private var width = 0
    private var text = ChatUtils.Text("", .0, .0, 1.0)
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

        width = 0
        customScoreboard.clear()

        // Get scoreboard title and lines
        val lines = ScoreboardUtils.sidebarLines.reversed()

        customScoreboard.addAll(lines)

        width = max(width, customScoreboard.maxOfOrNull { mc.fontRendererObj.getStringWidth(it) + 10 } ?: 0)

        if (customScoreboard.size >= 2) {
            customScoreboard = customScoreboard.dropLast(2).toMutableList()
        }

        text.text = (customScoreboard.joinToString("\n"))
    }

    @SubscribeEvent
    fun onRenderScoreboard(event: RenderScoreBoardEvent) {
        if (!config.CustomScoreboard) return
        event.isCanceled = true

        val screenWidth = mc.getWidth()
        val screenHeight = mc.getHeight()
        val textHeight = mc.fontRendererObj.FONT_HEIGHT *text.text.split("\n").size+1

        GlStateManager.pushMatrix()

        RenderUtils.drawRoundedRect(
            darkMode.darker(),
            screenWidth - width * 1.05,
            (screenHeight / 2) - (textHeight / 2) * 1.05,
            width * 1.05,
            textHeight * 1.05 + 5
        )

        RenderUtils.drawRoundedRect(
            darkMode,
            screenWidth - width * 1.025,
            ((screenHeight / 2) - (textHeight / 2)).toDouble(),
            width.toDouble(),
            textHeight.toDouble() + 5
        )



        val string = text.text
        val x = (screenWidth - width) *1f
        val y = (screenHeight / 2) - (textHeight / 2f)

        var yOffset = y - (mc.fontRendererObj.FONT_HEIGHT) / 2
        if (string.contains("\n")) {
            string.split("\n").forEach {
                yOffset += (mc.fontRendererObj.FONT_HEIGHT)
                mc.fontRendererObj.drawStringWithShadow(it, x, yOffset, Color.WHITE.rgb)
            }
        }
        else mc.fontRendererObj.drawStringWithShadow(string, x, y, Color.WHITE.rgb)

        GlStateManager.popMatrix()
    }
}