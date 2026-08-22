@file:Suppress("NOTHING_TO_INLINE")

package com.github.noamm9.ui.gui

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.general.CommandShortcuts
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import gg.essential.universal.UKeyboard
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class CommandShortcutsScreen: Screen(Component.literal("Command Shortcuts")) {
    private class Row(var command: String, var replacement: String) {
        val commandInput = TextInputHandler({ command }, { command = it })
        val replacementInput = TextInputHandler({ replacement }, { replacement = it })
    }

    private val rows = CommandShortcuts.shortcuts.get().map { (command, replacement) -> Row(command, replacement) }.toMutableList()

    private var scrollTarget = 0f
    private val scrollAnim = Animation(200L)

    private val panelW = 450f
    private val panelH = 250f
    private val entryHeight = 26f
    private val inputHeight = 18f
    private val commandWidth = 100f
    private val arrowWidth = 20f
    private val deleteWidth = 16f
    private val addBtnW = 120f
    private val addBtnH = 20f

    private inline val panelX get() = (Resolution.width / 2) - (panelW / 2)
    private inline val panelY get() = (Resolution.height / 2) - (panelH / 2)
    private inline val viewX get() = panelX + 10
    private inline val viewY get() = panelY + 25
    private inline val viewW get() = panelW - 20
    private inline val viewH get() = panelH - 65
    private inline fun deleteX(x: Float) = x + viewW - deleteWidth - 2

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX).toFloat()
        val mY = Resolution.getMouseY(mouseY).toFloat()

        val x = panelX
        val y = panelY

        graphics.drawRect(x, y, panelW, panelH, Color(20, 20, 20, 240))
        graphics.drawRect(x, y, panelW, 2f, Style.accentColor)
        graphics.drawCenteredString("§lCommand Shortcuts", x + panelW / 2, y + 8)

        val totalHeight = rows.size * entryHeight
        val maxScroll = (totalHeight - viewH).coerceAtLeast(0f)
        scrollTarget = scrollTarget.coerceIn(- maxScroll, 0f)
        scrollAnim.update(scrollTarget)
        val currentScroll = scrollAnim.value

        graphics.enableScissor(viewX.toInt(), viewY.toInt(), (viewX + viewW).toInt(), (viewY + viewH).toInt())

        if (rows.isEmpty()) graphics.drawCenteredString("§8No command shortcuts yet :(", x + panelW / 2, viewY + viewH / 2 - 4)

        val startIndex = max(0, (- currentScroll / entryHeight).toInt())
        val endIndex = min(rows.size, startIndex + ceil(viewH / entryHeight).toInt() + 1)

        for (index in startIndex until endIndex) {
            val row = rows[index]
            val rowY = viewY + currentScroll + (index * entryHeight)
            positionRow(row, viewX, rowY)
            drawRow(graphics, row, viewX, rowY, mX, mY)
        }

        graphics.disableScissor()

        if (maxScroll > 0) {
            val thumbHeight = 20f.coerceAtLeast((viewH / totalHeight) * viewH)
            val thumbY = viewY + (- currentScroll / maxScroll) * (viewH - thumbHeight)
            graphics.drawRect(x + panelW - 4, viewY, 2f, viewH, Color(255, 255, 255, 15))
            graphics.drawRect(x + panelW - 4, thumbY, 2f, thumbHeight, Style.accentColor)
        }

        val btnX = x + (panelW / 2) - (addBtnW / 2)
        val btnY = y + panelH - 30
        val btnHovered = mX >= btnX && mX <= btnX + addBtnW && mY >= btnY && mY <= btnY + addBtnH
        graphics.drawRect(btnX, btnY, addBtnW, addBtnH, if (btnHovered) Color(35, 35, 35, 240) else Color(15, 15, 15, 200))
        graphics.drawRect(btnX, btnY + addBtnH - 1, addBtnW, 1f, Style.accentColor)
        graphics.drawCenteredString("§a+ §rAdd Shortcut", btnX + addBtnW / 2, btnY + 6)

        Resolution.pop(graphics)
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

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mx = Resolution.getMouseX(event.x).toFloat()
        val my = Resolution.getMouseY(event.y).toFloat()

        val btnX = panelX + (panelW / 2) - (addBtnW / 2)
        val btnY = panelY + panelH - 30
        if (mx >= btnX && mx <= btnX + addBtnW && my >= btnY && my <= btnY + addBtnH) {
            rows.add(Row("", ""))
            scrollTarget = - (rows.size * entryHeight - viewH).coerceAtLeast(0f)
            Style.playClickSound(1f)
            return true
        }

        val inView = mx >= viewX && mx <= viewX + viewW && my >= viewY && my <= viewY + viewH

        if (inView) rows.forEachIndexed { index, _ ->
            val rowY = viewY + scrollAnim.value + (index * entryHeight)
            val delX = deleteX(viewX)
            if (mx >= delX && mx <= delX + deleteWidth && my >= rowY && my <= rowY + entryHeight) {
                rows.removeAt(index)
                Style.playClickSound(1f)
                return true
            }
        }

        val firstVisible = max(0, (- scrollAnim.value / entryHeight).toInt())
        val lastVisible = min(rows.size, firstVisible + ceil(viewH / entryHeight).toInt() + 1)
        var consumed = false

        rows.forEachIndexed { index, row ->
            val canClick = inView && index in firstVisible until lastVisible
            val inputX = if (canClick) mx else - 1f
            val inputY = if (canClick) my else - 1f
            if (row.commandInput.mouseClicked(inputX, inputY, event)) consumed = true
            if (row.replacementInput.mouseClicked(inputX, inputY, event)) consumed = true
        }

        if (consumed) return true

        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        rows.forEach {
            it.commandInput.mouseReleased()
            it.replacementInput.mouseReleased()
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mx: Double, my: Double, h: Double, v: Double): Boolean {
        val totalHeight = rows.size * entryHeight
        val maxScroll = (totalHeight - viewH).coerceAtLeast(0f)

        if (maxScroll > 0) {
            scrollTarget += (v * 44).toFloat()
            scrollTarget = scrollTarget.coerceIn(- maxScroll, 0f)
        }
        else scrollTarget = 0f

        return true
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == UKeyboard.KEY_SLASH) return true
        if (keyEvent.key == UKeyboard.KEY_BACKSLASH) return true

        rows.forEach {
            if (it.commandInput.keyPressed(keyEvent)) return true
            if (it.replacementInput.keyPressed(keyEvent)) return true
        }

        if (keyEvent.key == UKeyboard.KEY_ESCAPE) {
            onClose()
            return true
        }

        return super.keyPressed(keyEvent)
    }

    override fun charTyped(e: CharacterEvent): Boolean {
        if (e.codepoint == '/'.code) return true
        if (e.codepoint == '\\'.code) return true

        rows.forEach {
            if (it.commandInput.keyTyped(e)) return true
            if (it.replacementInput.keyTyped(e)) return true
        }
        return super.charTyped(e)
    }

    override fun onClose() {
        val shortcuts = linkedMapOf<String, String>()
        rows.forEach { row ->
            val command = row.command.trim().substringBefore(" ").lowercase()
            val replacement = row.replacement.trim()
            if (command.isNotEmpty() && replacement.isNotEmpty()) shortcuts[command] = replacement
        }

        GuiUtils.setScreen(ClickGuiScreen())
        CommandShortcuts.shortcuts.set(shortcuts)
        @Suppress("UNCHECKED_CAST")
        CommandShortcuts.build(mc.connection?.commands as CommandDispatcher<FabricClientCommandSource>)
    }
}
