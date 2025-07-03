package noammaddons.features.impl.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.RenderScoreBoardEvent
import noammaddons.features.Feature
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawFloatingRect
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScoreboardUtils.cleanSB
import noammaddons.utils.ScoreboardUtils.sidebarLines
import java.awt.Color


object CustomScoreboard: Feature("Draws the scoreboard with a diffrent design.") {
    private val darkMode = Color(33, 33, 33, 180)

    object ScoreBoardElement: GuiElement(hudData.getData().customScoreBoard) {
        override val enabled get() = CustomScoreboard.enabled
        override val width: Float get() = getText().maxOf { getStringWidth(it) } + 9
        override val height: Float get() = getStringHeight(getText()) + 8

        private const val exampleText =
            "  \uD83D\uDD2B\n" + " Late Winter 31s\uD83C\uDF6Bt\n" + " §75:30am §b☽\uD83D\uDCA3\n" + " §7⏣ §cDungeon H\uD83D\uDC7D§cub\n" + "      \uD83D\uDD2E\n" + "Purse: §62,135,3\uD83D\uDC0D§631,362\n" + "Bits: §b32,278\uD83D\uDC7E\n" + "         \uD83C\uDF20\n" + "Slayer Quest\uD83C\uDF6D\n" + "§4Tarantula Broo⚽§4dfather IV\n" + " §7(§e1,687§7/§c\uD83C\uDFC0§c2k§7) Combat X\n" + "             \uD83D\uDC79\n" + "§dNew Year Event\uD83C\uDF81§d!§f 15:17\n" + "               \uD83C\uDF89\n"

        private fun getText() = sidebarLines.reversed().filterNot {
            cleanSB(it).contains("www.hypixel.net")
        }.takeUnless { it.isEmpty() } ?: exampleText.split("\n")

        override fun draw() = drawCustomScoreboard(getText(), getX(), getY(), getScale())

        override fun exampleDraw() {
            draw()
            drawRect(Color.black, getX() - width * getScale(), getY(), width * getScale(), height * getScale())
        }

        override fun isHovered(mouseX: Float, mouseY: Float): Boolean {
            val scaledWidth = width * getScale()
            val scaledHeight = height * getScale()
            return mouseX >= getX() - scaledWidth && mouseX <= getX() && mouseY >= getY() && mouseY <= getY() + scaledHeight
        }
    }

    @SubscribeEvent
    fun onRenderScoreboard(event: RenderScoreBoardEvent) {
        if (! ScoreBoardElement.enabled) return
        if (HudEditorScreen.isOpen()) return
        ScoreBoardElement.draw()
    }

    fun drawCustomScoreboard(text: List<String>, x: Float, y: Float, scale: Float) {
        if (text.isEmpty()) return

        val maxWidth = text.maxOfOrNull(::getStringWidth) ?: return
        val textHeight = text.size * 9f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 1f)
        GlStateManager.scale(scale, scale, 1f)

        drawFloatingRect(- ((maxWidth + 6) + 3), 0, (maxWidth + 6) + 3, (textHeight + 5f) + 3, darkMode)
        text.forEachIndexed { index, line -> drawText(line, 6 - ((maxWidth + 6) + 3), index * 9f + 3f) }

        GlStateManager.popMatrix()
    }
}