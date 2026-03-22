package com.github.noamm9.ui.gui

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.misc.sound.SoundManager
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class SoundManagerScreen: Screen(Component.literal("SoundManager")) {
    private var searchQuery = ""
    private val searchHandler = TextInputHandler({ searchQuery }, { text ->
        searchQuery = text
        updateFilter()
    })

    private var scrollTarget = 0f
    private val scrollAnim = Animation(200L)
    private var draggingId: String? = null

    private var selectedCategory = "All"
    private val categories = listOf("All", "Blocks", "Hostile Mobs", "Neutral Mobs", "Music", "Ambient", "Items", "UI", "Misc")

    private val allSounds = BuiltInRegistries.SOUND_EVENT.keySet().map { it.toString() }
    private val filteredItems = mutableListOf<SoundItem>()

    override fun init() {
        super.init()
        updateFilter()
    }

    override fun render(ctx: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        Resolution.refresh()
        Resolution.push(ctx)

        val mX = Resolution.getMouseX(mouseX).toFloat()
        val mY = Resolution.getMouseY(mouseY).toFloat()

        val w = 450f
        val h = 250f
        val x = (Resolution.width / 2) - (w / 2)
        val y = (Resolution.height / 2) - (h / 2)
        val sidebarWidth = 100f

        Render2D.drawRect(ctx, x, y, w, h, Color(20, 20, 20, 240))
        Render2D.drawRect(ctx, x, y, sidebarWidth, h, Color(15, 15, 15, 200))
        Render2D.drawRect(ctx, x, y, w, 2f, Style.accentColor)

        Render2D.drawCenteredString(ctx, "§lSound Manager", x + ((w + sidebarWidth) / 2), y + 8)

        categories.forEachIndexed { index, cat ->
            val catY = y + 30 + (index * 20)
            val isHovered = mX >= x && mX <= x + sidebarWidth && mY >= catY && mY <= catY + 20
            val isSelected = selectedCategory == cat

            if (isSelected) Render2D.drawRect(ctx, x, catY, sidebarWidth, 20f, Style.accentColor.withAlpha(40))
            if (isHovered && ! isSelected) Render2D.drawRect(ctx, x, catY, sidebarWidth, 20f, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            Render2D.drawString(ctx, cat, x + 10, catY + 6, color)
        }

        val viewX = x + sidebarWidth + 10
        val viewY = y + 25
        val viewW = w - sidebarWidth - 20
        val viewH = h - 65
        val entryHeight = 22f
        val totalHeight = filteredItems.size * entryHeight

        val maxScroll = if (totalHeight > viewH) totalHeight - viewH else 0f
        scrollTarget = scrollTarget.coerceIn(- maxScroll, 0f)
        scrollAnim.update(scrollTarget)

        val currentScroll = scrollAnim.value

        ctx.enableScissor(viewX.toInt(), viewY.toInt(), (viewX + viewW).toInt(), (viewY + viewH).toInt())

        val startIndex = max(0, (- currentScroll / entryHeight).toInt())
        val endIndex = min(filteredItems.size, startIndex + ceil(viewH / entryHeight.toDouble()).toInt() + 1)

        for (i in startIndex until endIndex) {
            val item = filteredItems[i]
            val itemY = viewY + currentScroll + (i * entryHeight)

            when (item) {
                is SoundItem.Header -> {
                    Render2D.drawRect(ctx, viewX, itemY, viewW, entryHeight, Color(255, 255, 255, 5))
                    Render2D.drawCenteredString(ctx, "§l${item.name}", viewX + viewW / 2, itemY + 7, Style.accentColor)
                }

                is SoundItem.Sound -> {
                    drawSoundRow(ctx, item, viewX, itemY, viewW, entryHeight, mX, mY)
                }
            }
        }
        ctx.disableScissor()

        if (maxScroll > 0) {
            val thumbHeight = 20f.coerceAtLeast((viewH / totalHeight) * viewH)
            val thumbY = viewY + (- currentScroll / maxScroll) * (viewH - thumbHeight)
            Render2D.drawRect(ctx, x + w - 4, viewY, 2f, viewH, Color(255, 255, 255, 15))
            Render2D.drawRect(ctx, x + w - 4, thumbY, 2f, thumbHeight, Style.accentColor)
        }

        val searchX = x + sidebarWidth + (viewW / 2) - 100
        drawSearch(ctx, searchX, y + h - 30, 200f, 20f, mX, mY)

        Resolution.pop(ctx)
    }

    private fun drawSoundRow(ctx: GuiGraphics, item: SoundItem.Sound, x: Float, y: Float, w: Float, h: Float, mx: Float, my: Float) {
        val sliderW = 100f
        val sliderX = x + w - sliderW - 10

        if (draggingId == item.id) {
            val pct = ((mx - sliderX) / sliderW).coerceIn(0.0f, 1.0f)
            SoundManager.volumes.getData()[item.id] = (pct * 2.0).toFloat()
        }

        val vol = SoundManager.getMultiplier(item.id)
        val isHovered = mx >= x && mx <= x + w && my >= y && my <= y + h

        if (isHovered) {
            Render2D.drawRect(ctx, x, y, w, h, Color(255, 255, 255, 15))
        }

        Render2D.drawString(ctx, item.cleanName, x + 5, y + 7, Color.WHITE, shadow = true)

        val sliderY = y + 11
        Render2D.drawRect(ctx, sliderX, sliderY, sliderW, 2f, Color(255, 255, 255, 20))
        Render2D.drawRect(ctx, sliderX, sliderY, (vol / 2f) * sliderW, 2f, Style.accentColor)

        val valStr = "${(vol * 100).toInt()}%"
        val textWidth = mc.font.width(valStr).toFloat()
        Render2D.drawString(ctx, valStr, sliderX + sliderW - textWidth, y + 1, Color.GRAY)
    }

    private fun updateFilter() {
        val query = searchQuery.lowercase()

        val matchingSounds = allSounds.filter {
            it.lowercase().contains(query) && (selectedCategory == "All" || getCategory(it) == selectedCategory)
        }

        val grouped = matchingSounds.groupBy { getCategory(it) }.toSortedMap()

        filteredItems.clear()
        grouped.forEach { (cat, sounds) ->
            filteredItems.add(SoundItem.Header(cat))

            sounds.sorted().forEach { id ->
                val path = id.removePrefix("minecraft:")
                val cleanName = when {
                    path.startsWith("entity.hostile.") -> path.removePrefix("entity.hostile.")
                    path.startsWith("entity.") -> path.removePrefix("entity.")
                    path.contains(".") -> path.substringAfter(".")
                    else -> path
                }.replace(".", " ").replace("_", " ")

                filteredItems.add(SoundItem.Sound(id, cleanName))
            }
        }
        
        scrollTarget = 0f
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mx = Resolution.getMouseX(event.x).toFloat()
        val my = Resolution.getMouseY(event.y).toFloat()

        val w = 450f
        val h = 250f
        val x = (Resolution.width / 2) - (w / 2)
        val y = (Resolution.height / 2) - (h / 2)
        val sidebarWidth = 100f

        if (mx >= x && mx <= x + sidebarWidth) {
            categories.forEachIndexed { index, cat ->
                val catY = y + 30 + (index * 20)
                if (my >= catY && my <= catY + 20) {
                    if (selectedCategory != cat) {
                        selectedCategory = cat
                        updateFilter()
                        Style.playClickSound(1.0f)
                    }
                    return true
                }
            }
        }

        if (searchHandler.mouseClicked(mx, my, event)) return true

        val viewX = x + sidebarWidth + 10
        val viewY = y + 25
        val viewW = w - sidebarWidth - 20
        val viewH = h - 65
        val entryHeight = 22f

        if (mx > viewX && mx < viewX + viewW && my > viewY && my < viewY + viewH) {
            val clickOffset = my - viewY - scrollAnim.value
            val clickedIndex = (clickOffset / entryHeight).toInt()

            if (clickedIndex in filteredItems.indices) {
                val item = filteredItems[clickedIndex]
                if (item is SoundItem.Sound) {
                    val sliderW = 100f
                    val sliderX = viewX + viewW - sliderW - 10
                    if (mx >= sliderX - 5 && mx <= sliderX + sliderW + 5) {
                        draggingId = item.id
                        return true
                    }
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    private fun drawSearch(ctx: GuiGraphics, x: Float, y: Float, w: Float, h: Float, mx: Float, my: Float) {
        Render2D.drawRect(ctx, x, y, w, h, Color(15, 15, 15, 200))
        val color = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
        Render2D.drawRect(ctx, x, y + h - 1, w, 1f, color)

        searchHandler.x = x; searchHandler.y = y; searchHandler.width = w; searchHandler.height = h
        if (searchQuery.isEmpty() && ! searchHandler.listening) {
            Render2D.drawCenteredString(ctx, "§8Search...", x + w / 2, y + 6)
        }
        else {
            searchHandler.draw(ctx, mx, my)
        }
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingId = null
        searchHandler.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mx: Double, my: Double, h: Double, v: Double): Boolean {
        val viewH = 250f - 65f
        val totalHeight = filteredItems.size * 22f
        val maxScroll = if (totalHeight > viewH) totalHeight - viewH else 0f

        if (maxScroll > 0) {
            scrollTarget += (v * 44).toFloat()
            scrollTarget = scrollTarget.coerceIn(- maxScroll, 0f)
        }
        else {
            scrollTarget = 0f
        }
        return true
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (searchHandler.keyPressed(keyEvent)) return true
        if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(e: CharacterEvent): Boolean {
        if (searchHandler.keyTyped(e)) return true
        return super.charTyped(e)
    }

    override fun onClose() {
        SoundManager.volumes.save()
        ThreadUtils.scheduledTask {
            NoammAddons.screen = ClickGuiScreen
        }
    }

    private sealed class SoundItem {
        data class Header(val name: String): SoundItem()
        data class Sound(val id: String, val cleanName: String): SoundItem()
    }

    private fun getCategory(id: String): String {
        val path = id.removePrefix("minecraft:")
        return when {
            path.startsWith("block") -> "Blocks"
            path.startsWith("entity.hostile") -> "Hostile Mobs"
            path.startsWith("entity") -> "Neutral Mobs"
            path.startsWith("music") -> "Music"
            path.startsWith("ambient") -> "Ambient"
            path.startsWith("item") -> "Items"
            path.startsWith("ui") -> "UI"
            else -> "Misc"
        }
    }
}