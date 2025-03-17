package noammaddons.features.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.RenderScoreBoardEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawFloatingRect
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScoreboardUtils.cleanSB
import noammaddons.utils.ScoreboardUtils.sidebarLines
import java.awt.Color


object CustomScoreboard: Feature() {
    private val darkMode = Color(33, 33, 33, 180)


    object ScoreBoardElement: GuiElement(hudData.getData().CustomScoreBoard) {
        override val enabled get() = config.CustomScoreboard
        private val lines
            get() = sidebarLines.reversed().filterNot {
                cleanSB(it).contains("www.hypixel.net")
            }
        override val width: Float get() = lines.maxOf { getStringWidth(it) } + 9
        override val height: Float get() = getStringHeight(lines) + 8

        override fun draw() {
            if (! enabled) return
            drawCustomScoreboard(
                sidebarLines.reversed().filterNot {
                    cleanSB(it).contains("www.hypixel.net")
                },
                getX(), getY(),
                getScale()
            )
        }

        override fun exampleDraw() {
            val text =
                "  \uD83D\uDD2B\n" + " Late Winter 31s\uD83C\uDF6Bt\n" + " §75:30am §b☽\uD83D\uDCA3\n" + " §7⏣ §cDungeon H\uD83D\uDC7D§cub\n" + "      \uD83D\uDD2E\n" + "Purse: §62,135,3\uD83D\uDC0D§631,362\n" + "Bits: §b32,278\uD83D\uDC7E\n" + "         \uD83C\uDF20\n" + "Slayer Quest\uD83C\uDF6D\n" + "§4Tarantula Broo⚽§4dfather IV\n" + " §7(§e1,687§7/§c\uD83C\uDFC0§c2k§7) Combat X\n" + "             \uD83D\uDC79\n" + "§dNew Year Event\uD83C\uDF81§d!§f 15:17\n" + "               \uD83C\uDF89\n"

            drawCustomScoreboard(text.split("\n"), getX(), getY(), getScale())

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
        if (! config.CustomScoreboard) return
        event.isCanceled = true
        if (mc.currentScreen is HudEditorScreen) return

        ScoreBoardElement.draw()
    }

    fun drawCustomScoreboard(text: List<String>, x: Float, y: Float, scale: Float) {
        if (text.isEmpty()) return // fuck NoSuchElementException

        val textHeight = text.size * 9f

        val maxWidth = text.maxOf { line ->
            getStringWidth(line.removeFormatting())
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 1f)
        GlStateManager.scale(scale, scale, 1f)

        // Draw background box
        drawFloatingRect(
            - ((maxWidth + 6) + 3),
            0,
            (maxWidth + 6) + 3,
            (textHeight + 5f) + 3,
            darkMode,
        )

        text.forEachIndexed { index, line ->
            val lineY = index * 9f + 3f

            drawText(
                line,
                6 - ((maxWidth + 6) + 3),
                lineY
            )
        }

        GlStateManager.popMatrix()
    }
}