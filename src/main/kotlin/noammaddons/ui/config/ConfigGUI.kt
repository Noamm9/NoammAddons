package noammaddons.ui.config

import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ChatAllowedCharacters
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.scope
import noammaddons.ui.config.core.CategoryType
import noammaddons.ui.config.core.SubCategory
import noammaddons.ui.config.core.impl.Component.Companion.accentColor
import noammaddons.ui.config.core.impl.Component.Companion.compBackgroundColor
import noammaddons.ui.config.core.impl.Component.Companion.drawSmoothRect
import noammaddons.ui.config.core.impl.Component.Companion.hoverColor
import noammaddons.ui.config.core.save.Config
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.MathUtils.interpolateColor
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MouseUtils.isElementHovered
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.remove
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.reflect.KMutableProperty0


object ConfigGUI: GuiScreen() {
    val config = mutableMapOf<CategoryType, MutableSet<SubCategory>>()

    var selectedCategory = CategoryType.entries.first()
    var openedFeatureSettings: SubCategory? = null
    var filteredFeatures: List<SubCategory>? = null

    var categoryScroll = 0f
    var featuresScroll = 0f
    var settingsScroll = 0f

    // --- Layout Variables ---
    private var scale = 1.0 // Default
    private var guiX = 0.0
    private var guiY = 0.0
    private var guiWidth = 0.0
    private var guiHeight = 0.0

    // Panel Dimensions & Positions (Calculated in initGui)
    private var topBarHeight = 0.0
    private var categoryPanelX = 0.0
    private var categoryPanelWidth = 0.0
    private var featurePanelX = 0.0 // Middle panel
    private var featurePanelWidth = 0.0
    private var settingsPanelX = 0.0 // Right panel
    private var settingsPanelWidth = 0.0

    // --- Panel Padding ---
    private const val panelPadding = 6.0 // Base padding inside panels
    private const val panelGap = 1.0 // Gap between panels (for separator line)

    // --- Other State ---
    private var scrollJob: Job? = null
    private var dragging: Int? = null

    object SearchBar {
        var text = ""
        var isFocused = false
        var cursorIndex = 0

        var x = .0
        var y = .0
        var w = .0
        var h = .0

        fun reset() {
            text = ""
            isFocused = false
            cursorIndex = 0
        }

        const val padding = 6.0 * 2.0

        fun onClick(mx: Double, my: Double) {
            isFocused = isElementHovered(mx, my, x, y, w, h)
            if (isFocused) SoundUtils.click()
        }

        fun draw(mouseX: Double, mouseY: Double) {
            val isHovered = mouseX in x .. (x + w) && mouseY in y .. (y + h)

            drawRoundedRect(Color(20, 20, 20), x, y, w, h, 4f)

            val borderColor = when {
                isFocused -> accentColor
                isHovered -> Color(60, 60, 60)
                else -> Color(30, 30, 30)
            }

            drawRoundedBorder(borderColor, x, y, w, h, 4f, 1.2)

            val str = if (text.isEmpty()) "Search..." else trimTextToWidth(text, w - padding * 2)
            drawText(str, x + padding, y + 4)
        }

        fun onKeyTyped(typedChar: Char, keyCode: Int) {
            val ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)

            when {
                ctrl && keyCode == Keyboard.KEY_C -> {
                    setClipboardString(text)
                }

                ctrl && keyCode == Keyboard.KEY_V -> {
                    val paste = getClipboardString()?.filter { ChatAllowedCharacters.isAllowedCharacter(it) } ?: ""
                    text = text.substring(0, cursorIndex) + paste + text.substring(cursorIndex)
                    cursorIndex += paste.length
                }

                ctrl && keyCode == Keyboard.KEY_BACK -> {
                    val prevWord = prevWordIndex(text, cursorIndex)
                    text = text.removeRange(prevWord, cursorIndex)
                    cursorIndex = prevWord
                }

                keyCode == Keyboard.KEY_BACK -> {
                    if (cursorIndex > 0) {
                        text = text.removeRange(cursorIndex - 1, cursorIndex)
                        cursorIndex --
                    }
                }

                keyCode == Keyboard.KEY_LEFT -> cursorIndex = (cursorIndex - 1).coerceAtLeast(0)
                keyCode == Keyboard.KEY_RIGHT -> cursorIndex = (cursorIndex + 1).coerceAtMost(text.length)

                keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE -> reset()

                ChatAllowedCharacters.isAllowedCharacter(typedChar) -> {
                    val newText = text.substring(0, cursorIndex) + typedChar + text.substring(cursorIndex)
                    if (getStringWidth(newText) <= w - padding * 2) {
                        text = newText
                        cursorIndex ++
                    }
                }
            }

            cursorIndex = cursorIndex.coerceIn(0, text.length)
        }

        private fun prevWordIndex(text: String, fromIndex: Int): Int {
            val before = text.substring(0, fromIndex).trimEnd()
            return before.lastIndexOfAny(charArrayOf(' ', '\t', '\n')).takeIf { it != - 1 }?.plus(1) ?: 0
        }

        private fun trimTextToWidth(input: String, maxWidth: Double): String {
            var trimmed = input
            while (getStringWidth(trimmed) > maxWidth && trimmed.isNotEmpty()) {
                trimmed = trimmed.dropLast(1)
            }
            return trimmed
        }
    }

    object HudEditButton {
        private const val label = "Edit HUD"
        private var hovered = false
        private var hoverAnim = 0f

        var x = 10.0
        var y = .0
        var w = .0
        var h = .0

        fun onClick(mx: Number, my: Number) {
            if (! isElementHovered(mx, my, x, y, w, h)) return
            SoundUtils.click()
            ThreadUtils.setTimeout(100) {
                GuiUtils.openScreen(HudEditorScreen)
            }
        }

        fun draw(mx: Number, my: Number) {
            hovered = isElementHovered(mx, my, x, y, w, h)
            hoverAnim = (hoverAnim + (if (hovered) 1f else 0f - hoverAnim) * 0.2f).coerceIn(0f, 1f)

            val backgroundColor = interpolateColor(Color(35, 35, 35), hoverColor, hoverAnim)
            val outlineColor = interpolateColor(compBackgroundColor, accentColor, hoverAnim)

            drawRoundedRect(backgroundColor, x, y, w, h)
            drawRoundedBorder(outlineColor, x, y, w, h, 1.2f)

            val textX = x + w / 2
            val textY = y + (h - 9) / 2 + 1
            drawCenteredText(label, textX, textY)
        }
    }

    override fun initGui() {
        val sorted = config.toSortedMap(compareBy { it.ordinal })
        config.clear()
        config.putAll(sorted)

        scale = 2.0 / mc.getScaleFactor()
        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale
        guiWidth = (screenWidth * 0.6) //.coerceAtLeast(576.0)
        guiHeight = (screenHeight * 0.6) //.coerceAtLeast(324.0)
        guiX = (screenWidth - guiWidth) / 2.0
        guiY = (screenHeight - guiHeight) / 2.0

        topBarHeight = (guiHeight / 12.0) //.coerceAtLeast(25.0)

        val totalUsableWidth = guiWidth - panelGap * 2
        categoryPanelWidth = totalUsableWidth * 0.20
        featurePanelWidth = totalUsableWidth * 0.40
        settingsPanelWidth = totalUsableWidth - categoryPanelWidth - featurePanelWidth // Remaining space

        categoryPanelX = 0.0
        featurePanelX = categoryPanelX + categoryPanelWidth + panelGap
        settingsPanelX = featurePanelX + featurePanelWidth + panelGap

        // --- Initialize Sub-Components ---
        SearchBar.w = featurePanelWidth * 0.8
        SearchBar.h = topBarHeight * 0.6
        SearchBar.x = featurePanelX + (featurePanelWidth - SearchBar.w) / 2.0
        SearchBar.y = - (topBarHeight + SearchBar.h) / 2.0

        HudEditButton.w = categoryPanelWidth - panelPadding * 2
        HudEditButton.h = 20.0
        HudEditButton.x = panelPadding
        HudEditButton.y = guiHeight - HudEditButton.h - panelPadding
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val scaledMouseX = (mouseX / scale) - guiX
        val scaledMouseY = (mouseY / scale) - guiY

        GlStateManager.pushMatrix()
        GlStateManager.translate(guiX, guiY, 0.0)
        GlStateManager.scale(scale, scale, scale)

        drawSmoothRect(Color(26, 26, 26), 0.0, - topBarHeight, guiWidth, topBarHeight)
        drawSmoothRect(Color(13, 13, 13), 0.0, 0.0, guiWidth, guiHeight)

        drawSmoothRect(Color(26, 26, 26), categoryPanelX, 0.0, categoryPanelWidth, guiHeight) // Category Panel BG
        drawSmoothRect(Color(20, 20, 20), featurePanelX, 0.0, featurePanelWidth, guiHeight)   // Feature Panel BG
        drawSmoothRect(Color(23, 23, 23), settingsPanelX, 0.0, settingsPanelWidth, guiHeight)  // Settings Panel BG

        val separatorColor = Color(45, 45, 45)
        drawSmoothRect(separatorColor, featurePanelX - panelGap, 0.0, panelGap, guiHeight)
        drawSmoothRect(separatorColor, settingsPanelX - panelGap, 0.0, panelGap, guiHeight)

        drawText(FULL_PREFIX.remove("&n".addColor()), panelPadding, - (topBarHeight / 2.0 + 6), 1.6)
        SearchBar.draw(scaledMouseX, scaledMouseY)
        HudEditButton.draw(scaledMouseX, scaledMouseY)

        drawCategories(scaledMouseX, scaledMouseY)
        drawFeatures(scaledMouseX, scaledMouseY)
        drawSettings(scaledMouseX, scaledMouseY)

        GlStateManager.popMatrix()

        dragging?.let { btn ->
            openedFeatureSettings?.let { feature ->
                val settingX = settingsPanelX + panelPadding * 2
                var settingY = 20.0 - settingsScroll

                for (comp in feature.components) {
                    if (comp.hidden) continue
                    if (settingY > 0 && settingY < guiHeight - comp.height) {
                        comp.mouseDragged(settingX, settingY, scaledMouseX, scaledMouseY, btn)
                    }
                    settingY += 10 + comp.height
                }
            }
        }
    }

    fun drawCategories(mx: Double, my: Double) {
        var y = 10.0
        for (cat in config.keys) {
            val isHovered = mx in .0 .. categoryPanelWidth && my in y .. y + 20

            val color = when {
                cat == selectedCategory -> accentColor
                isHovered -> hoverColor
                else -> compBackgroundColor
            }

            drawSmoothRect(color, 10, y, categoryPanelWidth - 20, 20)
            drawCenteredText(cat.catName, categoryPanelWidth / 2, y + 6)
            y += 30
        }
    }

    fun drawFeatures(mx: Double, my: Double) {
        val x = featurePanelX + panelPadding * 2
        var y = 10.0 - featuresScroll
        for (cat in filteredFeatures ?: config[selectedCategory] ?: emptyList()) {
            if (y > 0 && y < guiHeight - cat.button1.height) {
                cat.button1.draw(x, y, mx, my)
            }
            y += 10 + cat.button1.height
        }
    }

    fun drawSettings(mx: Double, my: Double) {
        val settings = openedFeatureSettings ?: return
        val settingX = settingsPanelX + panelPadding * 2
        var settingY = 20.0 - settingsScroll


        drawCenteredText(settings.feature.name, settingX + 100, - topBarHeight + 8, 1.6)

        for (comp in settings.components) {
            if (comp.hidden) continue
            if (settingY > 0 && settingY < guiHeight - comp.height) {
                comp.draw(settingX, settingY, mx, my)
            }
            settingY += 10 + comp.height
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        onScroll()
    }

    fun onScroll() {
        val dWheel = Mouse.getEventDWheel()
        val scaledMouseX = MouseUtils.getMouseX() / scale - guiX
        val scaledMouseY = MouseUtils.getMouseY() / scale - guiY

        if (dWheel == 0) return
        if (scaledMouseY !in 0.0 .. guiHeight) return

        val scrollDelta = - dWheel * 0.2f

        fun scroll(current: Float, max: Float): Float {
            if (max <= guiHeight) return 0f
            return (current + scrollDelta).coerceIn(0f, max - guiHeight.toFloat())
        }

        fun animateScroll(targetRef: KMutableProperty0<Float>, targetValue: Float) {
            scrollJob?.cancel()
            scrollJob = scope.launch {
                val duration = 200L
                val steps = 20
                val delayTime = duration / steps

                repeat(steps) {
                    val currentValue = targetRef.get()
                    val interpolated = lerp(currentValue, targetValue, 0.2).toFloat()
                    targetRef.set(interpolated)
                    delay(delayTime)
                }

                targetRef.set(targetValue)
            }
        }

        when {
            scaledMouseX >= categoryPanelX && scaledMouseX < featurePanelX -> {
                // reserved category scrolling?
            }

            scaledMouseX >= featurePanelX && scaledMouseX < settingsPanelX -> {
                val maxHeight = (filteredFeatures ?: config[selectedCategory] !!).size * 30f
                animateScroll(::featuresScroll, scroll(featuresScroll, maxHeight))
            }

            scaledMouseX >= settingsPanelX && scaledMouseX < guiWidth -> openedFeatureSettings?.let { subCategory ->
                val maxHeight = subCategory.components.filterNot { it.hidden }.sumOf { it.height + 10f }.toFloat() + 20f
                animateScroll(::settingsScroll, scroll(settingsScroll, maxHeight))
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = mouseX / scale - guiX
        val scaledMouseY = mouseY / scale - guiY
        dragging = mouseButton

        SearchBar.onClick(scaledMouseX, scaledMouseY)
        HudEditButton.onClick(scaledMouseX, scaledMouseY)

        onCategoryClick(scaledMouseX, scaledMouseY, mouseButton)
        onFeatureClick(scaledMouseX, scaledMouseY, mouseButton)
        onSettingsClick(scaledMouseX, scaledMouseY, mouseButton)

        scope.launch {
            for (c in config.values) {
                for (s in c) {
                    for (comp in s.components) {
                        comp.updateVisibility()
                    }
                }
            }
        }
    }

    fun onCategoryClick(mx: Double, my: Double, btn: Int) {
        var y = 10.0
        for (cat in config.keys) {
            val clicked = mx in .0 .. categoryPanelWidth && my in y .. y + 20
            if (clicked) when (btn) {
                0 -> {
                    selectedCategory = cat
                    SoundUtils.click()
                    SearchBar.reset()
                    filteredFeatures = null
                }
            }
            y += 30
        }
    }

    fun onFeatureClick(mx: Double, my: Double, btn: Int) {
        val x = featurePanelX + panelPadding * 2
        var y = 10.0 - featuresScroll
        for (cat in filteredFeatures ?: config[selectedCategory] ?: emptyList()) {
            if (y > 0 && y < guiHeight - cat.button1.height) {
                cat.button1.mouseClicked(x, y, mx, my, btn)
            }
            y += 10 + cat.button1.height
        }
    }

    fun onSettingsClick(mx: Double, my: Double, btn: Int) {
        val settingX = settingsPanelX + panelPadding * 2
        var settingY = 20.0 - settingsScroll
        for (comp in openedFeatureSettings?.components ?: emptyList()) {
            if (comp.hidden) continue
            if (settingY > 0 && settingY < guiHeight - comp.height) {
                comp.mouseClicked(settingX, settingY, mx, my, btn)
            }
            settingY += 10 + comp.height
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val mx = mouseX / scale
        val my = mouseY / scale
        dragging = null

        val settingX = settingsPanelX + panelPadding * 2
        var settingY = 20.0 - settingsScroll
        for (comp in openedFeatureSettings?.components ?: emptyList()) {
            if (comp.hidden) continue
            if (settingY > 0 && settingY < guiHeight - comp.height) {
                comp.mouseRelease(settingX, settingY, mx, my, mouseButton)
            }
            settingY += 10 + comp.height
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (SearchBar.isFocused) {
            SearchBar.onKeyTyped(typedChar, keyCode)
            filteredFeatures = if (SearchBar.text.trim().isNotBlank()) {
                config.values.flatten().filter {
                    it.feature.name.contains(SearchBar.text, ignoreCase = true)
                }
            }
            else null
            return
        }
        else if (isCtrlKeyDown() && keyCode == Keyboard.KEY_F) SearchBar.isFocused = true

        var cancel = false

        (openedFeatureSettings?.components?.filterNot { it.hidden })?.forEach {
            if (! cancel) cancel = it.keyTyped(typedChar, keyCode)
        }

        if (cancel) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        Config.save()
        SearchBar.reset()
        filteredFeatures = null
        openedFeatureSettings = null
        scrollJob?.cancel() // Cancel animations
        scrollJob = null
    }
}