package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.SoundSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.scissor
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.sounds.SoundEvent
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.max

class SoundWidget(config: SoundSetting): Widget<SoundEvent>(config) {
    private inline val cfg get() = config as SoundSetting

    private var filteredSounds = SoundSetting.allSounds
    private var searchQuery = ""

    private val searchHandler = TextInputHandler(
        textProvider = { searchQuery },
        textSetter = {
            searchQuery = it
            updateFilter()
        }
    )

    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    private val searchHeight = 24
    private val entryHeight = 14
    private val listMaxHeight = 14 * 5
    private var scrollOffset = 0f

    override val height: Int
        get() = 20 + (openAnim.value * (searchHeight + listMaxHeight)).toInt()

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        ctx.drawRect(x, y, width, 20f, Style.settingBackgroundColor)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val valStr = "§7${cfg.prettyName(value)}"

        ctx.drawString(valStr, x + width - valStr.width() - 8f, y + 6f, scale = 1f)

        if (openAnim.value > 0.01f) {
            ctx.scissor(x, y + 20, width, height)

            val contentY = y + 20f
            val totalContentHeight = (searchHeight + listMaxHeight) * openAnim.value
            ctx.drawRect(x + 4f, contentY, width - 8f, totalContentHeight, Color(5, 5, 5, 150))

            val searchY = contentY + 2
            val searchW = width - 16f
            val searchH = 18f

            ctx.drawRect(x + 8f, searchY, searchW, searchH, Color(30, 30, 30, 180))
            val searchFocus = if (searchHandler.listening) 1f else 0f
            ctx.drawRect(x + 8f, searchY + searchH - 1f, searchW * searchFocus, 1f, Style.accentColor)

            searchHandler.x = x + 8f
            searchHandler.y = searchY
            searchHandler.width = searchW
            searchHandler.height = searchH

            if (searchQuery.isEmpty()) ctx.drawString("§8Search sound...", x + 12f, searchY + 5f)
            else searchHandler.draw(ctx, mouseX.toFloat(), mouseY.toFloat())

            val listY = searchY + searchHeight
            val viewableHeight = listMaxHeight - 4f

            val contentHeight = filteredSounds.size * entryHeight
            val maxScroll = max(0f, contentHeight - viewableHeight)
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            ctx.scissor(x, listY, width, viewableHeight)

            var entryY = listY - scrollOffset

            filteredSounds.forEach { sound ->
                if (entryY + entryHeight > listY && entryY < listY + viewableHeight) {
                    val isEntryHovered = mouseX >= x + 4 && mouseX <= x + width - 4 &&
                        mouseY >= entryY && mouseY < entryY + entryHeight

                    val isSelected = sound == value

                    if (isEntryHovered) ctx.drawRect(x + 4f, entryY, width - 8f, entryHeight.toFloat(), Color(255, 255, 255, 20))
                    val textColor = if (isSelected) Style.accentColor else if (isEntryHovered) Color.WHITE else Color.GRAY

                    ctx.drawString(cfg.prettyName(sound) !!, x + 12f, entryY + 3f, textColor)
                }
                entryY += entryHeight
            }

            ctx.disableScissor()

            if (maxScroll > 0) {
                val barHeight = (viewableHeight / contentHeight) * viewableHeight
                val barY = listY + ((scrollOffset / maxScroll) * (viewableHeight - barHeight))
                ctx.drawRect(x + width - 6f, barY, 2f, barHeight, Style.accentColor)
            }

            ctx.disableScissor()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            expanded = ! expanded
            Style.playClickSound(1f)
            return true
        }

        if (expanded) {
            val event = MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, GLFW.GLFW_PRESS))
            if (searchHandler.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), event)) return true

            val listY = y + 20 + searchHeight
            if (mouseX >= x && mouseX <= x + width && mouseY >= listY && mouseY <= listY + listMaxHeight) {
                val relativeY = (mouseY - listY) + scrollOffset
                val index = (relativeY / entryHeight).toInt()

                if (index in filteredSounds.indices) {
                    val sound = filteredSounds[index]
                    if (button == 0) {
                        value = SoundSetting.getSound(sound.location()) !!.value()
                        Style.playClickSound(1f)
                        expanded = false
                    }
                    return true
                }
            }
        }

        if (expanded && ! isMouseOver(mouseX, mouseY)) {
            expanded = false
        }

        return false
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        if (! expanded) return false
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scrollOffset -= (delta * 15).toFloat()
            return true
        }
        return false
    }

    override fun charTyped(codePoint: Char): Boolean {
        if (! expanded) return false
        val event = CharacterEvent(codePoint.code)
        return searchHandler.keyTyped(event)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (! expanded) return false
        val event = KeyEvent(keyCode, scanCode, modifiers)
        return searchHandler.keyPressed(event)
    }

    override fun mouseReleased(button: Int) {
        searchHandler.mouseReleased()
    }

    private fun updateFilter() {
        filteredSounds = if (searchQuery.isBlank()) SoundSetting.allSounds
        else SoundSetting.allSounds.filter { cfg.prettyName(it) !!.contains(searchQuery, ignoreCase = true) }
        scrollOffset = 0f
    }

    private fun isMouseOver(mx: Double, my: Double): Boolean {
        return mx >= x && mx <= x + width && my >= y && my <= y + height
    }
}