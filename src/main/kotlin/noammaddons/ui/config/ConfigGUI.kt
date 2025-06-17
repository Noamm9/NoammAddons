package noammaddons.ui.config

import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ChatAllowedCharacters
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.features.impl.gui.ConfigGui.guiType
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.noammaddons.Companion.scope
import noammaddons.noammaddons.Companion.textRenderer
import noammaddons.ui.clickgui.ClickGuiScreen
import noammaddons.ui.config.core.CategoryType
import noammaddons.ui.config.core.FeatureElement
import noammaddons.ui.config.core.impl.Component.Companion.compBackgroundColor
import noammaddons.ui.config.core.impl.Component.Companion.drawSmoothRect
import noammaddons.ui.config.core.impl.Component.Companion.hoverColor
import noammaddons.ui.config.core.save.Config
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.MathUtils.interpolateColor
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MouseUtils.isElementHovered
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.Utils.remove
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.reflect.KMutableProperty0


object ConfigGUI: GuiScreen() {
    val config = mutableMapOf<CategoryType, MutableSet<FeatureElement>>()

    var selectedCategory = CategoryType.entries.first()
    var openedFeatureSettings: FeatureElement? = null
    var filteredFeatures: List<FeatureElement>? = null

    var categoryScroll = 0f
    var featuresScroll = 0f
    var settingsScroll = 0f

    private var scale = 1.0
    private var guiX = 0.0
    private var guiY = 0.0
    private var guiWidth = 0.0
    private var guiHeight = 0.0

    private var topBarHeight = 0.0
    private var categoryPanelX = 0.0
    private var categoryPanelWidth = 0.0
    private var featurePanelX = 0.0
    private var featurePanelWidth = 0.0
    private var settingsPanelX = 0.0
    private var settingsPanelWidth = 0.0

    private const val panelPadding = 6.0
    private const val panelGap = 1.0

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
            textRenderer.drawText(str, x + padding, y + h / 2 - textRenderer.fr.fontHeight / 2 + 1)
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
            openScreen(HudEditorScreen)
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
            textRenderer.drawCenteredText(label, textX, textY, color = Color.WHITE)
        }
    }

    override fun initGui() {
        val sorted = config.toSortedMap(compareBy { it.ordinal })
        config.clear()
        config.putAll(sorted)

        scale = 2.0 / mc.getScaleFactor()
        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale
        guiWidth = 576.0
        guiHeight = 324.0
        guiX = (screenWidth - guiWidth) / 2.0
        guiY = (screenHeight - guiHeight) / 2.0

        topBarHeight = (guiHeight / 12.0)

        val totalUsableWidth = guiWidth - panelGap * 2
        categoryPanelWidth = totalUsableWidth * 0.20
        featurePanelWidth = totalUsableWidth * 0.40
        settingsPanelWidth = totalUsableWidth - categoryPanelWidth - featurePanelWidth

        categoryPanelX = 0.0
        featurePanelX = categoryPanelX + categoryPanelWidth + panelGap
        settingsPanelX = featurePanelX + featurePanelWidth + panelGap

        SearchBar.w = 200.0
        SearchBar.h = 20.0
        SearchBar.x = featurePanelX + panelPadding * 2 + 2
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
        GlStateManager.scale(scale, scale, 1.0)
        GlStateManager.translate(guiX, guiY, 0.0)

        drawRect(Color(26, 26, 26), 0.0, - topBarHeight, guiWidth, topBarHeight)
        drawRect(Color(13, 13, 13), 0.0, 0.0, guiWidth, guiHeight)

        drawRect(Color(26, 26, 26), categoryPanelX, 0.0, categoryPanelWidth, guiHeight) // Category Panel BG
        drawRect(Color(20, 20, 20), featurePanelX, 0.0, featurePanelWidth, guiHeight)   // Feature Panel BG
        drawRect(Color(23, 23, 23), settingsPanelX, 0.0, settingsPanelWidth, guiHeight)  // Settings Panel BG

        val separatorColor = Color(45, 45, 45)
        drawSmoothRect(separatorColor, featurePanelX - panelGap, 0.0, panelGap, guiHeight)
        drawSmoothRect(separatorColor, settingsPanelX - panelGap, 0.0, panelGap, guiHeight)

        textRenderer.drawText("${FULL_PREFIX.remove("&n".addColor())} &6&lv$MOD_VERSION", 0.3 * topBarHeight, - 18)

        SearchBar.draw(scaledMouseX, scaledMouseY)
        HudEditButton.draw(scaledMouseX, scaledMouseY)

        drawCategories(scaledMouseX, scaledMouseY)
        drawFeatures(scaledMouseX, scaledMouseY)
        drawSettings(scaledMouseX, scaledMouseY)

        GlStateManager.popMatrix()

        dragging?.let { mouseButtonBeingDragged ->
            openedFeatureSettings?.let { currentFeatureSettings ->
                val contentAreaX = settingsPanelX + panelPadding * 2
                val contentAreaGuiY = 0.0
                val contentAreaWidth = settingsPanelWidth - (panelPadding * 4)
                var currentDrawingY_withinScrollableSpace = 15.0 - settingsScroll

                val descHeight = textRenderer.warpText(
                    currentFeatureSettings.feature.desc,
                    maxWidth = contentAreaWidth,
                )
                currentDrawingY_withinScrollableSpace += descHeight

                for (comp in currentFeatureSettings.components) {
                    if (comp.hidden) continue
                    val componentDrawScreenY = contentAreaGuiY + currentDrawingY_withinScrollableSpace
                    val componentBottomScreenY = componentDrawScreenY + comp.height

                    if (componentBottomScreenY > contentAreaGuiY &&
                        componentDrawScreenY < contentAreaGuiY + guiHeight
                    ) {
                        comp.mouseDragged(
                            contentAreaX,
                            componentDrawScreenY,
                            scaledMouseX,
                            scaledMouseY,
                            mouseButtonBeingDragged
                        )
                    }
                    currentDrawingY_withinScrollableSpace += 10 + comp.height
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

            textRenderer.drawCenteredText(cat.catName, categoryPanelWidth / 2, y + 6)
            y += 30
        }
    }

    fun drawFeatures(mx: Double, my: Double) {
        val contentAreaX = featurePanelX + panelPadding * 2
        val contentAreaGuiY = 0.0
        val contentAreaWidth = featurePanelWidth - (panelPadding * 4)
        val contentAreaVisibleHeight = guiHeight - 10

        StencilUtils.beginStencilClip {
            drawSmoothRect(Color.WHITE, contentAreaX, contentAreaGuiY + 10, contentAreaWidth, contentAreaVisibleHeight)
        }

        var currentDrawingY_withinScrollableSpace = 10.0 - featuresScroll
        val featuresToDraw = filteredFeatures ?: config[selectedCategory] ?: emptyList()

        for (featureElement in featuresToDraw) {
            val featureToggleDrawScreenY = contentAreaGuiY + currentDrawingY_withinScrollableSpace
            featureElement.featureToggle.draw(contentAreaX + 2, featureToggleDrawScreenY, mx, my)
            currentDrawingY_withinScrollableSpace += 10 + featureElement.featureToggle.height
        }

        StencilUtils.endStencilClip()
    }

    fun drawSettings(mx: Double, my: Double) {
        val currentFeatureSettings = openedFeatureSettings ?: return

        val contentAreaX = settingsPanelX + panelPadding * 2
        val contentAreaGuiY = 0.0
        val contentAreaWidth = settingsPanelWidth - (panelPadding * 4)
        val contentAreaVisibleHeight = guiHeight

        val titleDrawX = settingsPanelX + settingsPanelWidth / 2
        val titleDrawY = - topBarHeight + 9
        textRenderer.drawCenteredText(currentFeatureSettings.feature.name, titleDrawX, titleDrawY)

        StencilUtils.beginStencilClip {
            drawSmoothRect(Color.WHITE, contentAreaX, contentAreaGuiY, contentAreaWidth, contentAreaVisibleHeight)
        }

        var currentDrawingY_withinScrollableSpace = 15.0 - settingsScroll

        val descDrawY = contentAreaGuiY + currentDrawingY_withinScrollableSpace - 8.5
        val descHeight = textRenderer.drawWrappedText(
            currentFeatureSettings.feature.desc,
            contentAreaX, descDrawY,
            maxWidth = contentAreaWidth
        )
        currentDrawingY_withinScrollableSpace += descHeight

        for (comp in currentFeatureSettings.components) {
            if (comp.hidden) continue
            val componentDrawY = contentAreaGuiY + currentDrawingY_withinScrollableSpace

            comp.draw(contentAreaX, componentDrawY, mx, my)
            currentDrawingY_withinScrollableSpace += 10 + comp.height
        }

        StencilUtils.endStencilClip()
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
                val maxHeight = (filteredFeatures ?: config[selectedCategory] !!).size * 30f + 10f
                animateScroll(::featuresScroll, scroll(featuresScroll, maxHeight))
            }

            scaledMouseX >= settingsPanelX && scaledMouseX < guiWidth -> openedFeatureSettings?.let { subCategory ->
                val maxHeight = subCategory.components.filterNot { it.hidden }.sumOf { it.height + 10f }.toFloat() + 20f + textRenderer.warpText(subCategory.feature.desc, maxWidth = 200.0).toFloat()
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
                    SearchBar.reset()
                    selectedCategory = cat
                    filteredFeatures = null
                    featuresScroll = 0f
                }
            }
            y += 30
        }
    }

    fun onFeatureClick(mx: Double, my: Double, btn: Int) {
        val contentAreaX = featurePanelX + panelPadding * 2
        val contentAreaGuiY = 0.0
        val contentAreaWidth = featurePanelWidth - (panelPadding * 4)
        val contentAreaVisibleHeight = guiHeight

        if (mx < contentAreaX || mx > contentAreaX + contentAreaWidth || my < contentAreaGuiY || my > contentAreaGuiY + contentAreaVisibleHeight) return

        var currentDrawingY_withinScrollableSpace = 10.0 - featuresScroll
        val featuresToConsider = filteredFeatures ?: config[selectedCategory] ?: emptyList()

        for (featureElement in featuresToConsider) {
            val featureToggle = featureElement.featureToggle
            val featureToggleDrawScreenY = contentAreaGuiY + currentDrawingY_withinScrollableSpace

            if (mx >= contentAreaX && mx <= contentAreaX + featureToggle.width && my >= featureToggleDrawScreenY && my <= featureToggleDrawScreenY + featureToggle.height) {
                val componentBottomScreenY = featureToggleDrawScreenY + featureToggle.height
                if (componentBottomScreenY > contentAreaGuiY && featureToggleDrawScreenY < contentAreaGuiY + contentAreaVisibleHeight) {
                    featureToggle.mouseClicked(contentAreaX, featureToggleDrawScreenY, mx, my, btn)
                }
            }
            currentDrawingY_withinScrollableSpace += 10 + featureToggle.height
        }
    }

    fun onSettingsClick(mx: Double, my: Double, btn: Int) {
        val currentFeatureSettings = openedFeatureSettings ?: return

        val contentAreaX = settingsPanelX + panelPadding * 2
        val contentAreaGuiY = 0.0
        val contentAreaWidth = settingsPanelWidth - (panelPadding * 4)
        val contentAreaVisibleHeight = guiHeight

        if (mx < contentAreaX || mx > contentAreaX + contentAreaWidth || my < contentAreaGuiY || my > contentAreaGuiY + contentAreaVisibleHeight) return

        var currentDrawingY_withinScrollableSpace = 15.0 - settingsScroll

        val descHeight = textRenderer.warpText(
            currentFeatureSettings.feature.desc,
            maxWidth = contentAreaWidth,
        )

        currentDrawingY_withinScrollableSpace += descHeight

        for (comp in currentFeatureSettings.components) {
            if (comp.hidden) continue

            val componentDrawScreenY = contentAreaGuiY + currentDrawingY_withinScrollableSpace

            val compHeight = comp.height

            if (mx >= contentAreaX && mx <= contentAreaX + comp.width &&
                my >= componentDrawScreenY && my <= componentDrawScreenY + compHeight
            ) {

                val componentBottomScreenY = componentDrawScreenY + compHeight
                if (componentBottomScreenY > contentAreaGuiY &&
                    componentDrawScreenY < contentAreaGuiY + contentAreaVisibleHeight
                ) {
                    comp.mouseClicked(contentAreaX, componentDrawScreenY, mx, my, btn)
                }
            }
            currentDrawingY_withinScrollableSpace += 10 + comp.height
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = mouseX / scale - guiX
        val scaledMouseY = mouseY / scale - guiY

        dragging = null

        openedFeatureSettings?.let { currentFeatureSettings ->
            val contentAreaX = settingsPanelX + panelPadding * 2
            val contentAreaGuiY = 0.0
            val contentAreaWidth = settingsPanelWidth - (panelPadding * 4)

            var currentDrawingY_withinScrollableSpace = 15.0 - settingsScroll

            val descHeight = textRenderer.warpText(currentFeatureSettings.feature.desc, maxWidth = contentAreaWidth)
            currentDrawingY_withinScrollableSpace += descHeight

            for (comp in currentFeatureSettings.components) {
                if (comp.hidden) continue

                val componentDrawScreenY = contentAreaGuiY + currentDrawingY_withinScrollableSpace

                comp.mouseRelease(
                    contentAreaX,
                    componentDrawScreenY,
                    scaledMouseX,
                    scaledMouseY,
                    mouseButton
                )
                currentDrawingY_withinScrollableSpace += 10 + comp.height
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (SearchBar.isFocused) {
            SearchBar.onKeyTyped(typedChar, keyCode)
            filteredFeatures = if (SearchBar.text.isNotBlank()) {
                featuresScroll = 0f
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
        scrollJob?.cancel()
        scrollJob = null
    }

    override fun doesGuiPauseGame() = false

    fun openGui() {
        RenderHelper.optifineFastRender(false)
        openScreen(
            when (guiType) {
                0 -> this
                1 -> ClickGuiScreen
                else -> null
            }
        )
    }
}