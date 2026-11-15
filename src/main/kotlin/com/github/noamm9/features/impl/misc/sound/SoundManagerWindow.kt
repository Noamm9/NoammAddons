package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.FeatureWindow
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.utils.*
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.scissor
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.spaceCaps
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import java.awt.Color
import kotlin.math.*

class SoundManagerWindow(feature: Feature): FeatureWindow(feature) {
    private companion object {
        private const val SIDEBAR_WIDTH = 108f
        private const val ENTRY_HEIGHT = 26f
        private const val SLIDER_WIDTH = 140f
        private const val PLAY_SOUND_WIDTH = 34f
        private const val SCROLLBAR_WIDTH = 6f
    }

    override val defaultWidth = 540f
    override val defaultHeight = 290f
    override val minWidth = 420f
    override val minHeight = 230f

    private var searchQuery = ""
    private val searchHandler = TextInputHandler({ searchQuery }, { text ->
        searchQuery = text
        updateFilter()
    })

    private val scroll = ScrollState(minThumbHeight = 28f)
    private var draggingId: String? = null

    private var selectedCategory = SoundCategory.All
    private var recentSoundsVersion = - 1L

    private val allSounds = BuiltInRegistries.SOUND_EVENT.entrySet().map { entry ->
        val id = entry.key.identifier().toString()
        Sound(id, getCleanName(id), getCategory(id), entry.value)
    }.sortedBy(Sound::id)
    private val soundsById = allSounds.associateBy(Sound::id)
    private val filteredItems = mutableListOf<SoundItem>()

    private val bodyY get() = y + titleBarHeight
    private val bodyHeight get() = height - titleBarHeight
    private val viewX get() = x + SIDEBAR_WIDTH + 10
    private val viewY get() = bodyY + 8
    private val viewW get() = width - SIDEBAR_WIDTH - 34
    private val viewH get() = bodyHeight - 52

    init {
        updateFilter()
    }

    override fun drawContentBackground(context: GuiGraphicsExtractor) {
        context.drawRect(x, bodyY, SIDEBAR_WIDTH, bodyHeight, Color(10, 10, 10, 160))
    }

    override fun drawContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val mX = mouseX.toFloat()
        val mY = mouseY.toFloat()

        if (selectedCategory == SoundCategory.Recent && recentSoundsVersion != SoundManager.recentSoundsVersion) {
            updateFilter(resetScroll = false)
        }

        SoundCategory.entries.forEachIndexed { index, cat ->
            val catY = categoryY(index)
            val isHovered = mX >= x && mX <= x + SIDEBAR_WIDTH && mY >= catY && mY <= catY + 20
            val isSelected = selectedCategory == cat

            if (isSelected) context.drawRect(x, catY, SIDEBAR_WIDTH, 20f, Style.accentColor.withAlpha(40))
            if (isHovered && ! isSelected) context.drawRect(x, catY, SIDEBAR_WIDTH, 20f, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            context.drawString(cat.renderName, x + 10, catY + 6, color)
        }

        scroll.update(filteredItems.size * ENTRY_HEIGHT, viewH)
        scroll.mouseDragged(mY)

        val currentScroll = scroll.value

        context.scissor(viewX, viewY, viewW, viewH)

        val startIndex = max(0, (currentScroll / ENTRY_HEIGHT).toInt())
        val endIndex = min(filteredItems.size, startIndex + ceil(viewH / ENTRY_HEIGHT.toDouble()).toInt() + 1)

        for (i in startIndex until endIndex) {
            val item = filteredItems[i]
            val itemY = viewY - currentScroll + (i * ENTRY_HEIGHT)

            when (item) {
                is Header -> {
                    context.drawRect(viewX, itemY, viewW, ENTRY_HEIGHT, Color(255, 255, 255, 5))
                    context.drawCenteredString("§l${item.name}", viewX + viewW / 2, itemY + 9, Style.accentColor)
                }

                is Sound -> drawSoundRow(context, item, viewX, itemY, viewW, ENTRY_HEIGHT, mX, mY)
            }
        }

        context.disableScissor()

        scroll.drawScrollbar(context, x + width - 18f, viewY, SCROLLBAR_WIDTH, viewH, mX, mY)

        val searchX = viewX + (viewW / 2) - 100
        drawSearch(context, searchX, bodyY + bodyHeight - 30, 200f, 20f, mX, mY)
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
        val name = if (sound.cleanName.width() <= maxNameWidth) sound.cleanName
        else mc.font.plainSubstrByWidth(sound.cleanName, maxNameWidth - "...".width()) + "..."

        ctx.drawString(name, x + 5f, y + 9f)
        ctx.drawRect(sliderX, y + 17f, SLIDER_WIDTH, 5f, Color(255, 255, 255, 24))
        ctx.drawRect(sliderX, y + 17f, SLIDER_WIDTH * progress, 5f, Style.accentColor)
        ctx.drawRect(sliderX + (SLIDER_WIDTH * progress) - 2f, y + 15f, 4f, 9f)
        ctx.drawRect(playX, y + 6f, PLAY_SOUND_WIDTH, 15f, if (playHovered) Style.accentColor.withAlpha(100) else Color(255, 255, 255, 18))
        ctx.drawCenteredString("Play", playX + PLAY_SOUND_WIDTH / 2f, y + 10f, if (playHovered) Color.WHITE else Color.LIGHT_GRAY)

        val valueText = "$volume%"
        ctx.drawString(valueText, sliderX + SLIDER_WIDTH - valueText.width(), y + 5f, Color.GRAY)
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

        if (resetScroll) scroll.reset()
    }

    override fun onMouseClicked(mouseX: Float, mouseY: Float, button: Int) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return

        if (mouseX >= x && mouseX <= x + SIDEBAR_WIDTH) SoundCategory.entries.forEachIndexed { index, cat ->
            val catY = categoryY(index)
            if (mouseY >= catY && mouseY <= catY + 20) {
                if (selectedCategory != cat) {
                    selectedCategory = cat
                    updateFilter()
                    SoundManager.playPreview(SoundEvents.UI_BUTTON_CLICK.value())
                }
                return
            }
        }

        val event = MouseButtonEvent(mouseX.toDouble(), mouseY.toDouble(), MouseButtonInfo(button, 0))
        if (searchHandler.mouseClicked(mouseX, mouseY, event)) return

        if (scroll.mouseClicked(mouseX, mouseY)) return

        if (mouseX <= viewX || mouseX >= viewX + viewW || mouseY <= viewY || mouseY >= viewY + viewH) return

        val clickedIndex = ((mouseY - viewY + scroll.value) / ENTRY_HEIGHT).toInt()
        val item = filteredItems.getOrNull(clickedIndex) as? Sound ?: return

        val itemY = viewY - scroll.value + (clickedIndex * ENTRY_HEIGHT)
        val playX = viewX + viewW - PLAY_SOUND_WIDTH - 8f
        val sliderX = playX - SLIDER_WIDTH - 8f

        if (mouseX >= playX && mouseX <= playX + PLAY_SOUND_WIDTH && mouseY >= itemY + 6f && mouseY <= itemY + 21f) {
            return SoundManager.playPreview(item.sound)
        }

        if (mouseX >= sliderX - 5f && mouseX <= sliderX + SLIDER_WIDTH + 5f) {
            draggingId = item.id
            setVolume(item.id, mouseX, sliderX)
        }
    }

    private fun setVolume(id: String, mouseX: Float, sliderX: Float) {
        val progress = ((mouseX - sliderX) / SLIDER_WIDTH).coerceIn(0f, 1f)
        SoundManager.setVolumePercent(id, (progress * 200).toInt())
    }

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

    override fun onMouseReleased(button: Int) {
        draggingId = null
        scroll.mouseReleased()
        searchHandler.mouseReleased()
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        if (! contains(mouseX.toFloat(), mouseY.toFloat())) return false
        scroll.scroll((delta * ENTRY_HEIGHT * 2).toFloat())
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = searchHandler.keyPressed(KeyEvent(keyCode, scanCode, modifiers))

    override fun charTyped(codePoint: Char) = searchHandler.keyTyped(CharacterEvent(codePoint.code))

    override fun clearFocus() {
        searchHandler.listening = false
    }

    private fun categoryY(index: Int) = bodyY + 6 + (index * 20)

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