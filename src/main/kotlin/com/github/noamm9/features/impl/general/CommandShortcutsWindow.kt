@file:Suppress("NOTHING_TO_INLINE")

package com.github.noamm9.features.impl.general

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.FeatureWindow
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.utils.*
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.scissor
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.*
import java.awt.Color
import kotlin.math.*

class CommandShortcutsWindow(feature: Feature): FeatureWindow(feature) {
    private class Row(var command: String, var replacement: String) {
        val commandInput = TextInputHandler({ command }, { command = it })
        val replacementInput = TextInputHandler({ replacement }, { replacement = it })
    }

    override val defaultWidth = 450f
    override val defaultHeight = 255f
    override val minWidth = 300f
    override val minHeight = 150f

    private val rows = CommandShortcuts.shortcuts.get().map { (command, replacement) -> Row(command, replacement) }.toMutableList()

    private val scroll = ScrollState()

    private val entryHeight = 26f
    private val inputHeight = 18f
    private val commandWidth = 100f
    private val arrowWidth = 20f
    private val deleteWidth = 16f
    private val addBtnW = 120f
    private val addBtnH = 20f

    private inline val bodyY get() = y + titleBarHeight
    private inline val viewX get() = x + 10
    private inline val viewY get() = bodyY + 6
    private inline val viewW get() = width - 20
    private inline val viewH get() = height - titleBarHeight - 46
    private inline val btnX get() = x + (width / 2) - (addBtnW / 2)
    private inline val btnY get() = y + height - 30
    private inline fun deleteX(x: Float) = x + viewW - deleteWidth - 2

    override fun drawContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val mX = mouseX.toFloat()
        val mY = mouseY.toFloat()

        scroll.update(rows.size * entryHeight, viewH)
        scroll.mouseDragged(mY)
        val currentScroll = scroll.value

        context.scissor(viewX, viewY, viewW, viewH)

        if (rows.isEmpty()) context.drawCenteredString("§8No command shortcuts yet :(", x + width / 2, viewY + viewH / 2 - 4)

        val startIndex = max(0, (currentScroll / entryHeight).toInt())
        val endIndex = min(rows.size, startIndex + ceil(viewH / entryHeight).toInt() + 1)

        for (index in startIndex until endIndex) {
            val row = rows[index]
            val rowY = viewY - currentScroll + (index * entryHeight)
            positionRow(row, viewX, rowY)
            drawRow(context, row, viewX, rowY, mX, mY)
        }

        context.disableScissor()

        scroll.drawScrollbar(context, x + width - 4, viewY, 2f, viewH, mX, mY)

        val btnHovered = mX >= btnX && mX <= btnX + addBtnW && mY >= btnY && mY <= btnY + addBtnH
        context.drawRect(btnX, btnY, addBtnW, addBtnH, if (btnHovered) Color(35, 35, 35, 240) else Color(15, 15, 15, 200))
        context.drawRect(btnX, btnY + addBtnH - 1, addBtnW, 1f, Style.accentColor)
        context.drawCenteredString("§a+ §rAdd Shortcut", btnX + addBtnW / 2, btnY + 6)
    }

    private fun positionRow(row: Row, x: Float, y: Float) {
        val inputY = y + (entryHeight - inputHeight) / 2

        row.commandInput.x = x
        row.commandInput.y = inputY
        row.commandInput.width = commandWidth
        row.commandInput.height = inputHeight

        row.replacementInput.x = x + commandWidth + arrowWidth
        row.replacementInput.y = inputY
        row.replacementInput.width = viewW - commandWidth - arrowWidth - deleteWidth - 10
        row.replacementInput.height = inputHeight
    }

    private fun drawRow(ctx: GuiGraphicsExtractor, row: Row, x: Float, y: Float, mx: Float, my: Float) {
        drawInput(ctx, row.commandInput, row.command.isEmpty(), "hello", mx, my)
        ctx.drawCenteredString("➜", x + commandWidth + arrowWidth / 2, y + 9)
        drawInput(ctx, row.replacementInput, row.replacement.isEmpty(), "pc hello world", mx, my)

        val delX = deleteX(x)
        val delHovered = mx >= delX && mx <= delX + deleteWidth && my >= y && my <= y + entryHeight
        ctx.drawCenteredString("✕", delX + deleteWidth / 2, y + 9, if (delHovered) Color.RED else Color.GRAY)
    }

    private fun drawInput(ctx: GuiGraphicsExtractor, handler: TextInputHandler, showPlaceholder: Boolean, placeholder: String, mx: Float, my: Float) {
        ctx.drawRect(handler.x, handler.y, handler.width, handler.height, Color(15, 15, 15, 200))
        val color = if (handler.listening) Style.accentColor else Color(255, 255, 255, 30)
        ctx.drawRect(handler.x, handler.y + handler.height - 1, handler.width, 1f, color)

        if (showPlaceholder && ! handler.listening) ctx.drawString("§8$placeholder", handler.x + 4, handler.y + 5)
        else handler.draw(ctx, mx, my)
    }

    override fun onMouseClicked(mouseX: Float, mouseY: Float, button: Int) {
        if (mouseX >= btnX && mouseX <= btnX + addBtnW && mouseY >= btnY && mouseY <= btnY + addBtnH) {
            rows.add(Row("", ""))
            scroll.scrollTo(rows.size * entryHeight - viewH)
            return Style.playClickSound(1f)
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT && scroll.mouseClicked(mouseX, mouseY)) return

        val inView = mouseX >= viewX && mouseX <= viewX + viewW && mouseY >= viewY && mouseY <= viewY + viewH

        if (inView) rows.forEachIndexed { index, _ ->
            val rowY = viewY - scroll.value + (index * entryHeight)
            val delX = deleteX(viewX)
            if (mouseX >= delX && mouseX <= delX + deleteWidth && mouseY >= rowY && mouseY <= rowY + entryHeight) {
                rows.removeAt(index)
                return Style.playClickSound(1f)
            }
        }

        val firstVisible = max(0, (scroll.value / entryHeight).toInt())
        val lastVisible = min(rows.size, firstVisible + ceil(viewH / entryHeight).toInt() + 1)

        rows.forEachIndexed { index, row ->
            val canClick = inView && index in firstVisible until lastVisible
            val inputX = if (canClick) mouseX else - 1f
            val inputY = if (canClick) mouseY else - 1f
            val event = MouseButtonEvent(inputX.toDouble(), inputY.toDouble(), MouseButtonInfo(button, 0))
            row.commandInput.mouseClicked(inputX, inputY, event)
            row.replacementInput.mouseClicked(inputX, inputY, event)
        }
    }

    override fun onMouseReleased(button: Int) {
        scroll.mouseReleased()
        rows.forEach {
            it.commandInput.mouseReleased()
            it.replacementInput.mouseReleased()
        }
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        if (! contains(mouseX.toFloat(), mouseY.toFloat())) return false
        scroll.scroll((delta * 44).toFloat())
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (! isListening) return false
        if (keyCode == InputConstants.KEY_SLASH || keyCode == InputConstants.KEY_BACKSLASH) return true

        val event = KeyEvent(keyCode, scanCode, modifiers)
        rows.forEach {
            if (it.commandInput.keyPressed(event)) return true
            if (it.replacementInput.keyPressed(event)) return true
        }
        return false
    }

    override fun charTyped(codePoint: Char): Boolean {
        if (! isListening) return false
        if (codePoint == '/' || codePoint == '\\') return true

        val event = CharacterEvent(codePoint.code)
        rows.forEach {
            if (it.commandInput.keyTyped(event)) return true
            if (it.replacementInput.keyTyped(event)) return true
        }
        return false
    }

    override fun clearFocus() = rows.forEach {
        it.commandInput.listening = false
        it.replacementInput.listening = false
    }

    override fun onClose() {
        val shortcuts = linkedMapOf<String, String>()
        rows.forEach { row ->
            val command = row.command.trim().substringBefore(" ").lowercase()
            val replacement = row.replacement.trim()
            if (command.isNotEmpty() && replacement.isNotEmpty()) shortcuts[command] = replacement
        }

        CommandShortcuts.shortcuts.set(shortcuts)
        CommandShortcuts.rebuild()
    }

    private val isListening get() = rows.any { it.commandInput.listening || it.replacementInput.listening }
}