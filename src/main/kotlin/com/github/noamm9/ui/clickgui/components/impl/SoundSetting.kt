package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.SoundUtils
import com.github.noamm9.utils.render.Render2D
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.max

class SoundSetting(name: String, defaultValue: SoundEvent): Setting<SoundEvent>(name, defaultValue), Savable {
    constructor(name: String, value: Holder.Reference<SoundEvent>): this(name, value.value())

    companion object {
        private val prettyNames = SoundUtils.REVERSE_MAP

        private val allSounds by lazy {
            BuiltInRegistries.SOUND_EVENT.entrySet()
                .mapNotNull { entry ->
                    val id = BuiltInRegistries.SOUND_EVENT.getKey(entry.value) ?: return@mapNotNull null
                    if (id !in prettyNames.keys) return@mapNotNull null
                    entry.value
                }
                .sortedBy { prettyNames[it.location()] }
        }

        private fun getSoundName(loc: Identifier): String {
            return prettyNames[loc] !!
        }

        private fun getSound(loc: Identifier): Holder.Reference<SoundEvent>? {
            return BuiltInRegistries.SOUND_EVENT.get(loc).orElse(null)
        }
    }

    private var filteredSounds = allSounds
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

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Render2D.drawRect(ctx, x, y, width, 20f, Style.bg)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val valStr = "§7${prettyNames[value.location]}"

        Render2D.drawString(ctx, valStr, x + width - mc.font.width(valStr) - 8f, y + 6f, Color.WHITE, 1f)

        if (openAnim.value > 0.01f) {
            ctx.enableScissor(x, y + 20, x + width, y + height)

            val contentY = y + 20f
            val totalContentHeight = (searchHeight + listMaxHeight) * openAnim.value
            Render2D.drawRect(ctx, x + 4f, contentY, width - 8f, totalContentHeight, Color(5, 5, 5, 150))

            val searchY = contentY + 2
            val searchW = width - 16f
            val searchH = 18f

            Render2D.drawRect(ctx, x + 8f, searchY, searchW, searchH, Color(30, 30, 30, 180))
            val searchFocus = if (searchHandler.listening) 1f else 0f
            Render2D.drawRect(ctx, x + 8f, searchY + searchH - 1f, searchW * searchFocus, 1f, Style.accentColor)

            searchHandler.x = x + 8f
            searchHandler.y = searchY
            searchHandler.width = searchW
            searchHandler.height = searchH

            if (searchQuery.isEmpty()) Render2D.drawString(ctx, "§8Search sound...", x + 12f, searchY + 5f)
            else searchHandler.draw(ctx, mouseX.toFloat(), mouseY.toFloat())

            val listY = searchY + searchHeight
            val viewableHeight = listMaxHeight - 4f

            val contentHeight = filteredSounds.size * entryHeight
            val maxScroll = max(0f, contentHeight - viewableHeight)
            scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

            ctx.enableScissor(x, listY.toInt(), x + width, (listY + viewableHeight).toInt())

            var entryY = listY - scrollOffset

            filteredSounds.forEach { sound ->
                if (entryY + entryHeight > listY && entryY < listY + viewableHeight) {
                    val isEntryHovered = mouseX >= x + 4 && mouseX <= x + width - 4 &&
                        mouseY >= entryY && mouseY < entryY + entryHeight

                    val isSelected = sound == value

                    if (isEntryHovered) Render2D.drawRect(ctx, x + 4f, entryY, width - 8f, entryHeight.toFloat(), Color(255, 255, 255, 20))
                    val textColor = if (isSelected) Style.accentColor else if (isEntryHovered) Color.WHITE else Color.GRAY

                    Render2D.drawString(ctx, prettyNames[sound.location] !!, x + 12f, entryY + 3f, textColor)
                }
                entryY += entryHeight
            }

            ctx.disableScissor()

            if (maxScroll > 0) {
                val barHeight = (viewableHeight / contentHeight) * viewableHeight
                val barY = listY + ((scrollOffset / maxScroll) * (viewableHeight - barHeight))
                Render2D.drawRect(ctx, x + width - 6f, barY, 2f, barHeight, Style.accentColor)
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
                        value = getSound(sound.location()) !!.value()
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

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (! expanded) return false
        val event = CharacterEvent(codePoint.code, modifiers)
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
        filteredSounds = if (searchQuery.isBlank()) allSounds
        else allSounds.filter { prettyNames[it.location()] !!.contains(searchQuery, ignoreCase = true) }
        scrollOffset = 0f
    }

    private fun isMouseOver(mx: Double, my: Double): Boolean {
        return mx >= x && mx <= x + width && my >= y && my <= y + height
    }

    override fun write(): JsonElement = JsonPrimitive(value.location().toString())

    override fun read(element: JsonElement?) {
        element?.asString?.let {
            val loc = Identifier.tryParse(it) ?: return
            val sound = getSound(loc) ?: return
            value = sound.value()
        }
    }
}
