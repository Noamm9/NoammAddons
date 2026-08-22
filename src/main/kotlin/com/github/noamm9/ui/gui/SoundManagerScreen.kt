package com.github.noamm9.ui.gui

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.misc.sound.SoundManager
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.spaceCaps
import gg.essential.universal.UKeyboard
import gg.essential.universal.UGraphics
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class SoundManagerScreen: Screen(Component.literal("SoundManager")) {
    private companion object {
        private const val WINDOW_WIDTH = 540f
        private const val WINDOW_HEIGHT = 285f
        private const val SIDEBAR_HEIGHT = 108f
        private const val ENTRY_HEIGHT = 26f
        private const val SLIDER_WIDTH = 140f
        private const val PLAY_SOUND_WIDTH = 34f
        private const val SCROLLBAR_WIDTH = 6f
    }

    private var searchQuery = ""
    private val searchHandler = TextInputHandler({ searchQuery }, { text ->
        searchQuery = text
        updateFilter()
    })

    private var scrollTarget = 0f
    private val scrollAnim = Animation(200L)
    private var draggingId: String? = null
    private var draggingScrollbar = false
    private var scrollbarDragOffset = 0f

    private var selectedCategory = SoundCategory.All
    private var recentSoundsVersion = - 1L

    private val allSounds = BuiltInRegistries.SOUND_EVENT.entrySet().map { entry ->
        val id = entry.key.identifier().toString()
        Sound(id, getCleanName(id), getCategory(id), entry.value)
    }.sortedBy(Sound::id)
    private val soundsById = allSounds.associateBy(Sound::id)
    private val filteredItems = mutableListOf<SoundItem>()

    override fun init() {
        super.init()
        updateFilter()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX).toFloat()
        val mY = Resolution.getMouseY(mouseY).toFloat()
        val w = WINDOW_WIDTH
        val h = WINDOW_HEIGHT
        val x = (Resolution.width / 2) - (w / 2)
        val y = (Resolution.height / 2) - (h / 2)

        if (selectedCategory == SoundCategory.Recent && recentSoundsVersion != SoundManager.recentSoundsVersion) {
            updateFilter(resetScroll = false)
        }

        graphics.drawRect(x, y, w, h, Color(20, 20, 20, 240))
        graphics.drawRect(x, y, SIDEBAR_HEIGHT, h, Color(15, 15, 15, 200))
        graphics.drawRect(x, y, w, 2f, Style.accentColor)

        graphics.drawCenteredString("§lSound Manager", x + ((w + SIDEBAR_HEIGHT) / 2), y + 8)

        SoundCategory.entries.forEachIndexed { index, cat ->
            val catY = y + 30 + (index * 20)
            val isHovered = mX >= x && mX <= x + SIDEBAR_HEIGHT && mY >= catY && mY <= catY + 20
            val isSelected = selectedCategory == cat

            if (isSelected) graphics.drawRect(x, catY, SIDEBAR_HEIGHT, 20f, Style.accentColor.withAlpha(40))
            if (isHovered && ! isSelected) graphics.drawRect(x, catY, SIDEBAR_HEIGHT, 20f, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            graphics.drawString(cat.renderName, x + 10, catY + 6, color)
        }

        val viewX = x + SIDEBAR_HEIGHT + 10
        val viewY = y + 26
        val viewW = w - SIDEBAR_HEIGHT - 34
        val viewH = h - 70
        val totalHeight = filteredItems.size * ENTRY_HEIGHT
        val maxScroll = (totalHeight - viewH).coerceAtLeast(0f)
        val thumbHeight = getScrollbarThumbHeight(totalHeight, viewH)

        scrollTarget = scrollTarget.coerceIn(- maxScroll, 0f)

        if (draggingScrollbar && maxScroll > 0f) {
            val ratio = ((mY - viewY - scrollbarDragOffset) / (viewH - thumbHeight)).coerceIn(0f, 1f)
            scrollTarget = - ratio * maxScroll
            scrollAnim.set(scrollTarget)
        }
        else {
            scrollAnim.update(scrollTarget)
        }

        val currentScroll = scrollAnim.value.coerceIn(- maxScroll, 0f)

        graphics.enableScissor(viewX.toInt(), viewY.toInt(), (viewX + viewW).toInt(), (viewY + viewH).toInt())

        val startIndex = max(0, (- currentScroll / ENTRY_HEIGHT).toInt())
        val endIndex = min(filteredItems.size, startIndex + ceil(viewH / ENTRY_HEIGHT.toDouble()).toInt() + 1)

        for (i in startIndex until endIndex) {
            val item = filteredItems[i]
            val itemY = viewY + currentScroll + (i * ENTRY_HEIGHT)

            when (item) {
                is Header -> {
                    graphics.drawRect(viewX, itemY, viewW, ENTRY_HEIGHT, Color(255, 255, 255, 5))
                    graphics.drawCenteredString("§l${item.name}", viewX + viewW / 2, itemY + 9, Style.accentColor)
                }

                is Sound -> drawSoundRow(graphics, item, viewX, itemY, viewW, ENTRY_HEIGHT, mX, mY)
            }
        }

        graphics.disableScissor()

        if (maxScroll > 0f) {
            val scrollbarX = x + w - 18f
            val thumbY = viewY + (- currentScroll / maxScroll) * (viewH - thumbHeight)
            val hovered = mX >= scrollbarX - 3f && mX <= scrollbarX + SCROLLBAR_WIDTH + 3f && mY >= viewY && mY <= viewY + viewH

            graphics.drawRect(scrollbarX, viewY, SCROLLBAR_WIDTH, viewH, Color(255, 255, 255, if (hovered) 24 else 15))
            graphics.drawRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, Style.accentColor.withAlpha(if (hovered || draggingScrollbar) 255 else 190))
        }
        else draggingScrollbar = false

        val searchX = x + SIDEBAR_HEIGHT + (viewW / 2) - 100
        drawSearch(graphics, searchX, y + h - 30, 200f, 20f, mX, mY)

        Resolution.pop(graphics)
    }

    private fun drawSoundRow(ctx: GuiGraphicsExtractor, sound: Sound, x: Float, y: Float, w: Float, h: Float, mx: Float, my: Float) {
        val playX = x + w - PLAY_SOUND_WIDTH - 8f
        val sliderX = playX - SLIDER_WIDTH - 8f

        if (draggingId == sound.id) setVolume(sound.id, mx, sliderX)

        val volume = SoundManager.getVolumePercent(sound.id)
        val progress = volume.toFloat() / 200
        val isHovered = mx >= x && mx <= x + w && my >= y && my <= y + h
        val playHovered = mx >= playX && mx <= playX + PLAY_SOUND_WIDTH && my >= y + 6f && my <= y + 21f

        if (isHovered) ctx.drawRect(x, y, w, h, Color(255, 255, 255, 15))

        val maxNameWidth = (sliderX - x - 12f).toInt()
        val name = if (UGraphics.getStringWidth(sound.cleanName) <= maxNameWidth) sound.cleanName
        else mc.font.plainSubstrByWidth(sound.cleanName, maxNameWidth - UGraphics.getStringWidth("...")) + "..."

        ctx.drawString(name, x + 5f, y + 9f)
        ctx.drawRect(sliderX, y + 17f, SLIDER_WIDTH, 5f, Color(255, 255, 255, 24))
        ctx.drawRect(sliderX, y + 17f, SLIDER_WIDTH * progress, 5f, Style.accentColor)
        ctx.drawRect(sliderX + (SLIDER_WIDTH * progress) - 2f, y + 15f, 4f, 9f)
        ctx.drawRect(playX, y + 6f, PLAY_SOUND_WIDTH, 15f, if (playHovered) Style.accentColor.withAlpha(100) else Color(255, 255, 255, 18))
        ctx.drawCenteredString("Play", playX + PLAY_SOUND_WIDTH / 2f, y + 10f, if (playHovered) Color.WHITE else Color.LIGHT_GRAY)

        val valueText = "$volume%"
        ctx.drawString(valueText, sliderX + SLIDER_WIDTH - UGraphics.getStringWidth(valueText), y + 5f, Color.GRAY)
    }

    private fun updateFilter(resetScroll: Boolean = true) {
        val query = searchQuery.lowercase()
        val sounds = when (selectedCategory) {
            SoundCategory.Recent -> SoundManager.getRecentSoundIds().mapNotNull(soundsById::get)
            else -> allSounds.filter { selectedCategory == SoundCategory.All || it.category == selectedCategory }
        }.filter { query.isEmpty() || it.searchText.contains(query) }

        filteredItems.clear()

        if (selectedCategory == SoundCategory.Recent) {
            if (sounds.isNotEmpty()) filteredItems.add(Header(SoundCategory.Recent.renderName))
            filteredItems.addAll(sounds)
            recentSoundsVersion = SoundManager.recentSoundsVersion
        }
        else sounds.groupBy(Sound::category).toSortedMap().forEach { (category, categorySounds) ->
            filteredItems.add(Header(category.renderName))
            filteredItems.addAll(categorySounds)
        }


        if (resetScroll) {
            scrollTarget = 0f
            scrollAnim.set(0f)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, isDoubleClick)

        val mx = Resolution.getMouseX(event.x).toFloat()
        val my = Resolution.getMouseY(event.y).toFloat()
        val w = WINDOW_WIDTH
        val h = WINDOW_HEIGHT
        val x = (Resolution.width / 2) - (w / 2)
        val y = (Resolution.height / 2) - (h / 2)

        if (mx >= x && mx <= x + SIDEBAR_HEIGHT) SoundCategory.entries.forEachIndexed { index, cat ->
            val catY = y + 30 + (index * 20)
            if (my >= catY && my <= catY + 20) {
                if (selectedCategory != cat) {
                    selectedCategory = cat
                    updateFilter()
                    SoundManager.playPreview(SoundEvents.UI_BUTTON_CLICK.value())
                }
                return true
            }
        }

        if (searchHandler.mouseClicked(mx, my, event)) return true

        val viewX = x + SIDEBAR_HEIGHT + 10
        val viewY = y + 26
        val viewW = w - SIDEBAR_HEIGHT - 34
        val viewH = h - 70
        val totalHeight = filteredItems.size * ENTRY_HEIGHT
        val maxScroll = (totalHeight - viewH).coerceAtLeast(0f)
        val scrollbarX = x + w - 18f

        if (maxScroll > 0f && mx >= scrollbarX - 3f && mx <= scrollbarX + SCROLLBAR_WIDTH + 3f && my >= viewY && my <= viewY + viewH) {
            val thumbHeight = getScrollbarThumbHeight(totalHeight, viewH)
            val thumbY = viewY + (- scrollAnim.value.coerceIn(- maxScroll, 0f) / maxScroll) * (viewH - thumbHeight)

            scrollbarDragOffset = if (my in thumbY .. thumbY + thumbHeight) my - thumbY else thumbHeight / 2f
            if (my !in thumbY .. thumbY + thumbHeight) {
                val ratio = ((my - viewY - scrollbarDragOffset) / (viewH - thumbHeight)).coerceIn(0f, 1f)
                scrollTarget = - ratio * maxScroll
                scrollAnim.set(scrollTarget)
            }

            draggingScrollbar = true
            return true
        }

        if (mx > viewX && mx < viewX + viewW && my > viewY && my < viewY + viewH) {
            val clickedIndex = ((my - viewY - scrollAnim.value) / ENTRY_HEIGHT).toInt()
            val item = filteredItems.getOrNull(clickedIndex)

            if (item is Sound) {
                val itemY = viewY + scrollAnim.value + (clickedIndex * ENTRY_HEIGHT)
                val playX = viewX + viewW - PLAY_SOUND_WIDTH - 8f
                val sliderX = playX - SLIDER_WIDTH - 8f

                if (mx >= playX && mx <= playX + PLAY_SOUND_WIDTH && my >= itemY + 6f && my <= itemY + 21f) {
                    SoundManager.playPreview(item.sound)
                    return true
                }

                if (mx >= sliderX - 5f && mx <= sliderX + SLIDER_WIDTH + 5f) {
                    draggingId = item.id
                    setVolume(item.id, mx, sliderX)
                    return true
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    private fun setVolume(id: String, mouseX: Float, sliderX: Float) {
        val progress = ((mouseX - sliderX) / SLIDER_WIDTH).coerceIn(0f, 1f)
        SoundManager.setVolumePercent(id, (progress * 200).toInt())
    }

    private fun getScrollbarThumbHeight(totalHeight: Float, viewHeight: Float) = if (totalHeight <= viewHeight) viewHeight else ((viewHeight / totalHeight) * viewHeight).coerceAtLeast(28f)

    private fun drawSearch(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, mx: Float, my: Float) {
        ctx.drawRect(x, y, w, h, Color(15, 15, 15, 200))
        val color = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
        ctx.drawRect(x, y + h - 1, w, 1f, color)

        searchHandler.x = x
        searchHandler.y = y
        searchHandler.width = w
        searchHandler.height = h

        if (searchQuery.isEmpty() && ! searchHandler.listening) {
            ctx.drawCenteredString("§8Search...", x + w / 2, y + 6)
        }
        else searchHandler.draw(ctx, mx, my)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingId = null
        draggingScrollbar = false
        searchHandler.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mx: Double, my: Double, h: Double, v: Double): Boolean {
        val viewH = WINDOW_HEIGHT - 70f
        val totalHeight = filteredItems.size * ENTRY_HEIGHT
        val maxScroll = (totalHeight - viewH).coerceAtLeast(0f)

        if (maxScroll > 0) {
            scrollTarget += (v * ENTRY_HEIGHT * 2).toFloat()
            scrollTarget = scrollTarget.coerceIn(- maxScroll, 0f)
        }
        else scrollTarget = 0f

        return true
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (searchHandler.keyPressed(keyEvent)) return true
        if (keyEvent.key == UKeyboard.KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(e: CharacterEvent): Boolean {
        if (searchHandler.keyTyped(e)) return true
        return super.charTyped(e)
    }

    override fun onClose() = GuiUtils.setScreen(ClickGuiScreen())

    private enum class SoundCategory {
        All, Recent, Blocks, HostileMobs, NeutralMobs, Music, Ambient, Items, UI, Misc;

        val renderName = name.spaceCaps()
    }

    private sealed interface SoundItem
    private data class Header(val name: String): SoundItem
    private data class Sound(val id: String, val cleanName: String, val category: SoundCategory, val sound: SoundEvent): SoundItem {
        val searchText = "$id $cleanName"
    }

    private fun getCleanName(id: String): String {
        val path = id.removePrefix("minecraft:")
        return when {
            path.startsWith("entity.hostile.") -> path.removePrefix("entity.hostile.")
            path.startsWith("entity.") -> path.removePrefix("entity.")
            path.contains(".") -> path.substringAfter(".")
            else -> path
        }.replace(".", " ").replace("_", " ")
    }

    private fun getCategory(id: String): SoundCategory {
        val path = id.removePrefix("minecraft:")
        return when {
            path.startsWith("block") -> SoundCategory.Blocks
            path.startsWith("entity.hostile") -> SoundCategory.HostileMobs
            path.startsWith("entity") -> SoundCategory.NeutralMobs
            path.startsWith("music") -> SoundCategory.Music
            path.startsWith("ambient") -> SoundCategory.Ambient
            path.startsWith("item") -> SoundCategory.Items
            path.startsWith("ui") -> SoundCategory.UI
            else -> SoundCategory.Misc
        }
    }
}