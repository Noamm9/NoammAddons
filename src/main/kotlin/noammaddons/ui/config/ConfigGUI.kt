package noammaddons.ui.config

import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ChatAllowedCharacters
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.noammaddons.Companion.scope
import noammaddons.ui.config.core.CategoryType
import noammaddons.ui.config.core.SubCategory
import noammaddons.ui.config.core.impl.Component.Companion.accentColor
import noammaddons.ui.config.core.impl.Component.Companion.compBackgroundColor
import noammaddons.ui.config.core.impl.Component.Companion.drawSmoothRect
import noammaddons.ui.config.core.impl.Component.Companion.hoverColor
import noammaddons.ui.config.core.impl.button1
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
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.reflect.KMutableProperty0

object ConfigGUI: GuiScreen() {
    val config = mutableMapOf<CategoryType, MutableSet<SubCategory>>()

    var selectedCategory = CategoryType.entries.first()
    var selectedSubCategory: SubCategory? = null
    var filteredComponents: List<SubCategory>? = null

    var subCategoryScroll = 0f
    var componentsScroll = 0f

    private var scale = .0
    private var guiWidth = .0
    private var guiHeight = .0
    private var guiX = .0
    private var guiY = .0

    private var leftPanelWidth = .0

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

        fun init(guiWidth: Double, guiHeight: Double) {
            w = guiWidth / 5.0
            h = (guiHeight / 12.0) * 0.5

            x = guiWidth / 2 - w / 2 - padding
            y = - (h * 1.5)
        }

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

            val visibleText = if (text.isEmpty()) {
                drawText("Search...", x + padding, y + 2, 1f)
                ""
            }
            else {
                val trimmed = trimTextToWidth(text, w - padding * 2)
                drawText(trimmed, x + padding, y + 2)
                trimmed
            }
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

        fun init(guiWidth: Double, guiHeight: Double) {
            y = guiHeight * 0.9
            w = 80.0
            h = 20.0
        }

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

        leftPanelWidth = 100.0

        scale = 2.0 / mc.getScaleFactor()
        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale
        guiWidth = (screenWidth * 0.6)
        guiHeight = (screenHeight * 0.6)
        guiX = (screenWidth - guiWidth) / 2.0
        guiY = (screenHeight - guiHeight) / 2.0

        SearchBar.init(guiWidth, guiHeight)
        HudEditButton.init(guiWidth, guiHeight)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        glPushMatrix()
        glScaled(scale, scale, scale)
        glTranslated(guiX, guiY, 0.0)

        val topPanelY = guiHeight / 12
        drawSmoothRect(Color(26, 26, 26), 0, - topPanelY, guiWidth, topPanelY)
        drawText("${FULL_PREFIX.remove("&n".addColor())} &6&lv$MOD_VERSION", 10, - (topPanelY / 2 + 6), 1.6)
        drawSmoothRect(Color(13, 13, 13), 0, 0, guiWidth, guiHeight)
        drawSmoothRect(Color(26, 26, 26), 0, 0, leftPanelWidth, guiHeight)

        SearchBar.draw(scaledMouseX - guiX, scaledMouseY - guiY)
        HudEditButton.draw(scaledMouseX - guiX, scaledMouseY - guiY)

        var y = 10
        for (cat in config.keys) {
            val isHovered = scaledMouseX in guiX .. (guiX + leftPanelWidth) &&
                    scaledMouseY in (guiY + y) .. (guiY + y + 20)

            val color = when {
                cat == selectedCategory -> accentColor
                isHovered -> hoverColor
                else -> compBackgroundColor
            }

            drawSmoothRect(color, 10, y, leftPanelWidth - 20, 20)
            drawCenteredText(cat.catName, leftPanelWidth / 2, y + 6)
            y += 30
        }


        val subCategoryPanelX = leftPanelWidth + 20.0
        var subCategoryPanelY = 20.0 - subCategoryScroll
        for (cat in filteredComponents ?: config[selectedCategory] ?: emptyList()) {
            if (subCategoryPanelY > 0 && subCategoryPanelY < guiHeight - cat.button1.height) cat.button1.draw(
                subCategoryPanelX, subCategoryPanelY,
                scaledMouseX - guiX, scaledMouseY - guiY
            )
            subCategoryPanelY += 10 + cat.button1.height
        }

        selectedSubCategory?.let {
            val panelX = guiWidth - leftPanelWidth * 2 - 20
            var panelY = 20.0 - componentsScroll

            drawSmoothRect(Color.WHITE, (panelX - (panelX - (subCategoryPanelX + button1.width)) / 2), 20, 1.5, guiHeight - 40)
            drawCenteredText(it.feature.name, panelX + 100, - 18.0, 1.3)

            for (comp in it.components) {
                if (comp.hidden) continue
                if (panelY > 0 && panelY + comp.height < guiHeight) {
                    comp.draw(panelX, panelY, scaledMouseX - guiX, scaledMouseY - guiY)
                }
                panelY += 10 + comp.height
            }
        }

        glPopMatrix()

        dragging?.let { btn ->
            selectedSubCategory?.let { subCategory ->
                val panelX = guiX + guiWidth - leftPanelWidth - 100
                var panelY = guiY + 20.0 - subCategoryScroll
                for (comp in subCategory.components) {
                    if (comp.hidden) continue
                    if (panelY > 0) {
                        comp.mouseDragged(
                            panelX, panelY,
                            scaledMouseX, scaledMouseY,
                            btn
                        )
                    }
                    panelY += 10 + comp.height
                }
            }
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
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

        when (scaledMouseX) {
            in 0.0 .. leftPanelWidth -> {
                // reserved
            }

            in leftPanelWidth + 20.0 .. leftPanelWidth + 220.0 -> {
                val maxHeight = (filteredComponents ?: config[selectedCategory] !!).size * 30f
                animateScroll(::subCategoryScroll, scroll(subCategoryScroll, maxHeight))
            }

            in guiWidth - leftPanelWidth - 70 .. guiWidth -> {
                selectedSubCategory?.let { subCategory ->
                    val maxHeight = subCategory.components.filterNot { it.hidden }.sumOf { it.height + 10f }.toFloat() + 20f
                    animateScroll(::componentsScroll, scroll(componentsScroll, maxHeight))
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = mouseX / scale - guiX
        val scaledMouseY = mouseY / scale - guiY
        dragging = mouseButton

        SearchBar.onClick(scaledMouseX, scaledMouseY)
        HudEditButton.onClick(scaledMouseX, scaledMouseY)

        var y = 10.0
        for (cat in config.keys) {
            if (scaledMouseX in .0 .. leftPanelWidth && scaledMouseY in y .. y + 20) {
                selectedCategory = cat
                SoundUtils.click()
                subCategoryScroll = 0f
                componentsScroll = 0f
                SearchBar.reset()
            }
            y += 30
        }

        val subCategoryPanelX = leftPanelWidth + 20.0
        var subCategoryPanelY = 20.0 - subCategoryScroll
        for (cat in filteredComponents ?: config[selectedCategory] ?: emptyList()) {
            if (subCategoryPanelY > 0 && subCategoryPanelY < guiHeight - cat.button1.height) cat.button1.mouseClicked(
                subCategoryPanelX, subCategoryPanelY,
                scaledMouseX, scaledMouseY, mouseButton
            )
            subCategoryPanelY += 10 + cat.button1.height
        }

        selectedSubCategory?.let {
            val panelX = guiWidth - leftPanelWidth - 100
            var panelY = 20.0 - componentsScroll
            for (comp in it.components) {
                if (comp.hidden) continue
                if (panelY > 0) {
                    comp.mouseClicked(
                        panelX, panelY,
                        scaledMouseX, scaledMouseY,
                        mouseButton
                    )
                }
                panelY += 10 + comp.height
            }
        }

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

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale
        dragging = null

        selectedSubCategory?.let {
            val panelX = guiX + guiWidth - leftPanelWidth - 70
            var panelY = guiY + 20.0 - subCategoryScroll
            for (comp in it.components) {
                if (comp.hidden) continue
                if (panelY > 0) {
                    comp.mouseRelease(
                        panelX, panelY,
                        scaledMouseX, scaledMouseY,
                        state
                    )
                }
                panelY += 10 + comp.height
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (SearchBar.isFocused) {
            SearchBar.onKeyTyped(typedChar, keyCode)
            filteredComponents = if (SearchBar.text.trim().isNotBlank()) {
                config.values.flatten().filter {
                    it.feature.name.contains(SearchBar.text, ignoreCase = true)
                }
            }
            else null
            return
        }
        else if (isCtrlKeyDown() && keyCode == Keyboard.KEY_F) SearchBar.isFocused = true

        var cancel = false

        (selectedSubCategory?.components?.filterNot { it.hidden })?.forEach {
            if (! cancel) cancel = it.keyTyped(typedChar, keyCode)
        }

        if (cancel) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        Config.save()
    }

}