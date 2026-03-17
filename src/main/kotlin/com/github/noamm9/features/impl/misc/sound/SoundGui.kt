package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.NoammAddons
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color

object SoundGui: Screen(Component.literal("SoundManager")) {
    private var searchQuery = ""
    private val searchHandler = TextInputHandler({ searchQuery }, { searchQuery = it })

    private var scrollTarget = 0f
    private val scrollAnim = Animation(200L)
    private var draggingId: String? = null

    private var selectedCategory = "All"
    private val categories = listOf("All", "Blocks", "Hostile Mobs", "Neutral Mobs", "Music", "Ambient", "Items", "UI", "Misc")

    private var filteredItems = mutableListOf<SoundItem>()
    private var lastQuery = "!!!"
    private var lastCategory = "!!!"

    override fun render(ctx: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        Resolution.refresh()
        Resolution.push(ctx)
        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)

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
            if (isHovered) Render2D.drawRect(ctx, x, catY, sidebarWidth, 20f, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            Render2D.drawString(ctx, cat, x + 10, catY + 6, color)
        }

        updateFilter()
        val viewX = x + sidebarWidth + 10
        val viewY = y + 25
        val viewW = w - sidebarWidth - 20
        val viewH = h - 65

        val entryHeight = 22f
        val totalHeight = filteredItems.size * entryHeight
        val maxScroll = if (totalHeight > viewH) totalHeight - viewH else 0f

        ctx.enableScissor(viewX.toInt(), viewY.toInt(), (viewX + viewW).toInt(), (viewY + viewH).toInt())
        scrollAnim.update(scrollTarget.coerceIn(- maxScroll, 0f))
        var currentY = viewY + scrollAnim.value

        filteredItems.forEach { item ->
            when (item) {
                is SoundItem.Header -> {
                    Render2D.drawRect(ctx, viewX, currentY, viewW, entryHeight, Color(255, 255, 255, 5))
                    Render2D.drawCenteredString(ctx, "§l${item.name}", viewX + viewW / 2, currentY + 7, Style.accentColor)
                }

                is SoundItem.Sound -> {
                    drawSoundRow(ctx, item.id, viewX, currentY, viewW, entryHeight, mX.toDouble(), mY.toDouble())
                }
            }
            currentY += entryHeight
        }
        ctx.disableScissor()

        if (maxScroll > 0) {
            val thumbHeight = (viewH / totalHeight) * viewH
            val thumbY = viewY + (- scrollAnim.value / totalHeight) * viewH
            Render2D.drawRect(ctx, x + w - 4, viewY, 2f, viewH, Color(255, 255, 255, 15))
            Render2D.drawRect(ctx, x + w - 4, thumbY, 2f, thumbHeight, Style.accentColor)
        }

        val searchX = x + sidebarWidth + (viewW / 2) - 100
        drawSearch(ctx, searchX, y + h - 30, 200f, 20f, mX.toDouble(), mY.toDouble())

        Resolution.pop(ctx)
    }

    private fun drawSoundRow(ctx: GuiGraphics, id: String, x: Float, y: Float, w: Float, h: Float, mx: Double, my: Double) {
        val viewY = (Resolution.height / 2) - 125 + 25
        val viewH = 250 - 65

        val isWithinViewport = my >= viewY && my <= viewY + viewH
        val isHovered = isWithinViewport && mx >= x && mx <= x + w && my >= y && my <= y + h

        val path = id.removePrefix("minecraft:")
        val cleanName = when {
            path.startsWith("entity.hostile.") -> path.removePrefix("entity.hostile.")
            path.startsWith("entity.") -> path.removePrefix("entity.")
            path.contains(".") -> path.substringAfter(".")
            else -> path
        }.replace(".", " ").replace("_", " ")

        val vol = SoundManager.getMultiplier(id)
        val sliderW = 100f
        val sliderX = x + w - sliderW - 10

        if (draggingId == id) {
            val pct = ((mx - sliderX) / sliderW).coerceIn(0.0, 1.0)
            SoundManager.volumes.getData()[id] = (pct * 2.0).toFloat()
        }

        if (isHovered) {
            Render2D.drawRect(ctx, x, y, w, h, Color(255, 255, 255, 15))
        }

        Render2D.drawString(ctx, cleanName, x + 5, y + 7, Color.WHITE, shadow = true)

        val sliderY = y + 11
        Render2D.drawRect(ctx, sliderX, sliderY, sliderW, 2f, Color(255, 255, 255, 20))
        Render2D.drawRect(ctx, sliderX, sliderY, (vol / 2f) * sliderW, 2f, Style.accentColor)

        val valStr = "${(vol * 100).toInt()}%"
        Render2D.drawString(ctx, valStr, sliderX + sliderW - valStr.width(), y + 1, Color.GRAY)
    }

    private fun updateFilter() {
        if (searchQuery == lastQuery && selectedCategory == lastCategory) return
        lastQuery = searchQuery
        lastCategory = selectedCategory

        val rawList = BuiltInRegistries.SOUND_EVENT.entrySet()
            .mapNotNull { entry -> BuiltInRegistries.SOUND_EVENT.getKey(entry.value)?.toString() }
            .filter { it.contains(searchQuery, ignoreCase = true) }
            .filter { selectedCategory == "All" || getCategory(it) == selectedCategory }

        val grouped = rawList.groupBy { getCategory(it) }.toSortedMap()

        filteredItems.clear()
        grouped.forEach { (cat, sounds) ->
            filteredItems.add(SoundItem.Header(cat))
            sounds.sorted().forEach { filteredItems.add(SoundItem.Sound(it)) }
        }
        scrollTarget = 0f
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = Resolution.getMouseX(mouseButtonEvent.x)
        val my = Resolution.getMouseY(mouseButtonEvent.y)

        val w = 450f
        val h = 250f
        val x = (Resolution.width / 2) - (w / 2)
        val y = (Resolution.height / 2) - (h / 2)
        val sidebarWidth = 100f

        if (mx >= x && mx <= x + sidebarWidth) {
            categories.forEachIndexed { index, cat ->
                val catY = y + 30 + (index * 20)
                if (my >= catY && my <= catY + 20) {
                    selectedCategory = cat
                    Style.playClickSound(1.0f)
                    return true
                }
            }
        }

        if (searchHandler.mouseClicked(mx.toFloat(), my.toFloat(), mouseButtonEvent)) return true

        val viewY = y + 25
        val viewH = h - 65

        if (mx > x + sidebarWidth && mx < x + w && my > viewY && my < viewY + viewH) {
            var curY = viewY + scrollAnim.value
            filteredItems.forEach { item ->
                if (item is SoundItem.Sound) {
                    val sliderW = 100f
                    val sliderX = x + sidebarWidth + 10 + (w - sidebarWidth - 20) - sliderW - 10

                    if (mx >= sliderX - 5 && mx <= sliderX + sliderW + 5 && my >= curY && my <= curY + 22) {
                        draggingId = item.id
                        return true
                    }
                }
                curY += 22
            }
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    private fun drawSearch(ctx: GuiGraphics, x: Float, y: Float, w: Float, h: Float, mx: Double, my: Double) {
        Render2D.drawRect(ctx, x, y, w, h, Color(15, 15, 15, 200))
        val color = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
        Render2D.drawRect(ctx, x, y + h - 1, w, 1f, color)
        searchHandler.x = x; searchHandler.y = y; searchHandler.width = w; searchHandler.height = h
        if (searchQuery.isEmpty() && ! searchHandler.listening) {
            Render2D.drawCenteredString(ctx, "§8Search...", x + w / 2, y + 6)
        }
        else searchHandler.draw(ctx, mx.toFloat(), my.toFloat())
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        draggingId = null
        searchHandler.mouseReleased()
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(mx: Double, my: Double, h: Double, v: Double): Boolean {
        val viewH = 250f - 65f
        val totalHeight = filteredItems.size * 22f
        val maxScroll = if (totalHeight > viewH) totalHeight - viewH else 0f

        if (maxScroll > 0) {
            scrollTarget += (v * 44).toFloat()
            if (scrollTarget > 0f) scrollTarget = 0f
            if (scrollTarget < - maxScroll) scrollTarget = - maxScroll
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
        data class Sound(val id: String): SoundItem()
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
