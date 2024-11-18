package noammaddons.features.hud

import gg.essential.universal.UGraphics.getStringWidth
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.config.EditGui.components.PosElement
import noammaddons.events.RenderScoreBoardEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScoreboardUtils
import noammaddons.utils.ScoreboardUtils.cleanSB
import java.awt.Color


object CustomScoreboard: Feature() {
    private val darkMode = Color(33, 33, 33, 180)
    private val CustomScoreBoardPos = PosElement(hudData.getData().CustomScoreBoard) {
        val data = hudData.getData().CustomScoreBoard
        val text =
            "  \uD83D\uDD2B\n" + " Late Winter 31s\uD83C\uDF6Bt\n" + " §75:30am §b☽\uD83D\uDCA3\n" + " §7⏣ §cDungeon H\uD83D\uDC7D§cub\n" + "      \uD83D\uDD2E\n" + "Purse: §62,135,3\uD83D\uDC0D§631,362\n" + "Bits: §b32,278\uD83D\uDC7E\n" + "         \uD83C\uDF20\n" + "Slayer Quest\uD83C\uDF6D\n" + "§4Tarantula Broo⚽§4dfather IV\n" + " §7(§e1,687§7/§c\uD83C\uDFC0§c2k§7) Combat X\n" + "             \uD83D\uDC79\n" + "§dNew Year Event\uD83C\uDF81§d!§f 15:17\n" + "               \uD83C\uDF89\n"

        return@PosElement drawCustomScoreboard(text, data.x, data.y, data.scale)
    }

    @SubscribeEvent
    fun onRenderScoreboard(event: RenderScoreBoardEvent) {
        if (! config.CustomScoreboard) return
        event.isCanceled = true
        if (mc.currentScreen is HudEditorScreen) return

        CustomScoreBoardPos.run {
            drawCustomScoreboard(
                ScoreboardUtils.sidebarLines.reversed().filterNot {
                    cleanSB(it).contains("www.hypixel.net")
                }.joinToString("\n"),
                getX(), getY(),
                getScale()
            )
        }
    }

    fun drawCustomScoreboard(text: String, x: Float, y: Float, scale: Float): Pair<Float, Float> {
        val fixedScale = scale / mc.getScaleFactor()
        val scaledX = x / fixedScale
        val scaledY = y / fixedScale

        val lines = text.split("\n")
        val textHeight = lines.size * 9f

        val maxWidth = lines.maxOf { line ->
            getStringWidth(line.removeFormatting()).toFloat()
        }

        GlStateManager.pushMatrix()
        GlStateManager.scale(fixedScale, fixedScale, fixedScale)

        drawRoundedRect(
            darkMode,
            scaledX - maxWidth + 6,
            scaledY - textHeight / 2 - 5,
            maxWidth + 6,
            textHeight + 5f
        )

        drawRainbowRoundedBorder(
            scaledX - maxWidth + 6,
            scaledY - textHeight / 2 - 5,
            maxWidth + 6,
            textHeight + 5f
        )

        lines.forEachIndexed { index, line ->
            drawText(
                line,
                scaledX + 9 - maxWidth,
                scaledY - textHeight / 2 + 2 + index * 9f
            )
        }

        GlStateManager.popMatrix()

        return Pair((maxWidth + 4) * fixedScale, (textHeight + 5f) * fixedScale)
    }

}