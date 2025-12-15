package noammaddons.features.impl.hud


import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.config.editgui.HudEditorScreen
import noammaddons.events.RenderScoreBoardEvent
import noammaddons.features.Feature
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawFloatingRect
import noammaddons.utils.RenderUtils.drawRectBorder
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScoreboardUtils.cleanSB
import noammaddons.utils.ScoreboardUtils.sidebarLines
import java.awt.Color

object CustomScoreboard: Feature("Draws the scoreboard with a different design.") {
    object ScoreBoardElement: GuiElement(hudData.getData().customScoreBoard) {
        override val enabled get() = CustomScoreboard.enabled
        private val lines: List<String> get() = getText()

        override val width: Float
            get() = lines.maxOfOrNull { mc.fontRendererObj.getStringWidth(it) }?.toFloat() ?: 140F

        override val height: Float
            get() = if (lines.isEmpty()) 0f else (lines.size * mc.fontRendererObj.FONT_HEIGHT + 2).toFloat()

        private const val exampleText =
            "  \uD83D\uDD2B\n" + " Late Winter 31s\uD83C\uDF6Bt\n" + " §75:30am §b☽\uD83D\uDCA3\n" + " §7⏣ §cDungeon H\uD83D\uDC7D§cub\n" + "      \uD83D\uDD2E\n" + "Purse: §62,135,3\uD83D\uDC0D§631,362\n" + "Bits: §b32,278\uD83D\uDC7E\n" + "         \uD83C\uDF20\n" + "Slayer Quest\uD83C\uDF6D\n" + "§4Tarantula Broo⚽§4dfather IV\n" + " §7(§e1,687§7/§c\uD83C\uDFC0§c2k§7) Combat X\n" + "             \uD83D\uDC79\n" + "§dNew Year Event\uD83C\uDF81§d!§f 15:17\n" + "               \uD83C\uDF89\n"

        private fun getText() = sidebarLines.reversed().filterNot {
            cleanSB(it).contains("www.hypixel.net")
        }.takeUnless { it.isEmpty() } ?: if (HudEditorScreen.isOpen()) exampleText.split("\n") else emptyList()

        override fun draw() {
            val scoreList = this.lines
            if (scoreList.isEmpty()) return

            val padding = 4f

            val fontRenderer = mc.fontRendererObj
            val color = Color(33, 33, 33, 180)
            val maxWidth = this.width.toInt()

            GlStateManager.pushMatrix()
            GlStateManager.translate(getX(), getY(), 0f)
            GlStateManager.translate(- this.width * getScale(), 0f, 0f)
            GlStateManager.scale(getScale(), getScale(), 1f)

            val title = scoreList.first()
            val titleWidth = fontRenderer.getStringWidth(title)
            val fontHeight = fontRenderer.FONT_HEIGHT + 1

            drawFloatingRect(0, 0, maxWidth + padding * 2, (scoreList.size * fontHeight) + 2, color)
            drawText(title, padding + (maxWidth - titleWidth) / 2f, padding + 1.5f)

            var yOffset = fontHeight + 2 + padding
            scoreList.drop(1).forEach { score ->
                if (score == scoreList.drop(1).last() && cleanSB(score).isBlank()) return@forEach
                drawText(score, padding, yOffset.toFloat())
                yOffset += fontHeight
            }

            GlStateManager.popMatrix()
        }

        override fun isHovered(mouseX: Float, mouseY: Float): Boolean {
            val scaledWidth = width * getScale()
            val scaledHeight = height * getScale()
            return mouseX >= getX() - scaledWidth && mouseX <= getX() && mouseY >= getY() && mouseY <= getY() + scaledHeight
        }

        override fun renderBackground(isHovered: Boolean) {
            val alpha = if (isHovered) 140 else 90
            val borderColor = when {
                isHovered -> Color(100, 180, 255)
                else -> Color(100, 100, 120)
            }

            GlStateManager.pushMatrix()
            GlStateManager.translate(getX(), getY(), 0f)
            GlStateManager.translate(- this.width * getScale(), 0f, 0f)
            GlStateManager.scale(getScale(), getScale(), getScale())
            RenderUtils.drawRect(Color(30, 35, 45, alpha), - 2, - 2, width + 4, height + 4)
            drawRectBorder(borderColor, - 2, - 2, width + 4, height + 4)
            GlStateManager.popMatrix()
        }
    }

    @SubscribeEvent
    fun onRenderScoreboard(event: RenderScoreBoardEvent) {
        if (! ScoreBoardElement.enabled || HudEditorScreen.isOpen()) return
        ScoreBoardElement.draw()
    }
}