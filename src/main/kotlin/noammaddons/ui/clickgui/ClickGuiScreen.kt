package noammaddons.ui.clickgui

import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.noammaddons.Companion.scope
import noammaddons.noammaddons.Companion.textRenderer
import noammaddons.ui.components.TextField
import noammaddons.ui.config.ConfigGUI
import noammaddons.ui.config.core.CategoryType
import noammaddons.ui.config.core.save.Config
import noammaddons.ui.font.GlyphPageFontRenderer
import noammaddons.ui.font.TextRenderer
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MouseUtils
import noammaddons.utils.MouseUtils.isElementHovered
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.Utils.remove
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0


object ClickGuiScreen: GuiScreen() {
    val titleRenderer25 = TextRenderer(GlyphPageFontRenderer.create("Inter", 25, bold = true))
    val searchBar = TextField(0, 0, 0, 0, 5, titleRenderer25)
    var currentSettingMenu: FeatureSettingMenu? = null
    val panels = mutableSetOf<Panel>()

    var scrollJob: Job? = null

    private var dragging: Int? = null
    var draggingPanel: Panel? = null
    var dragOffsetX: Float = 0f
    var dragOffsetY: Float = 0f

    var scale = 2f

    override fun initGui() {
        scale = 2f / mc.getScaleFactor()

        searchBar.width = 120
        searchBar.height = 30
        searchBar.x = (mc.getWidth() / scale) / 2f - searchBar.width.toFloat() / 2f
        searchBar.y = (mc.getHeight() / scale) * 0.9f - searchBar.height.toFloat() / 2f

        val categoryMap = ConfigGUI.config.entries.associate { it.key to it.value.map { f -> f.feature }.toMutableList() }
        val createdPanels = mutableMapOf<CategoryType, Panel>()

        CategoryType.entries.forEach { cat ->
            if (cat in panels.map { it.category }) return@forEach
            categoryMap[cat]?.let { feats ->
                val panel = Panel(cat, feats)
                panels.add(panel)
                createdPanels[cat] = panel
            }
        }

        val panelsInFirstRow = CategoryType.entries.take(7)

        var currentX = 10f
        for (catType in panelsInFirstRow) {
            createdPanels[catType]?.let { panel ->
                panel.x = currentX
                panel.y = 20f
                currentX += 120f + 10f
            }
        }

        createdPanels[CategoryType.MISC]?.let { miscPanel ->
            createdPanels[CategoryType.SLAYERS]?.let { slayersPanel ->
                miscPanel.x = slayersPanel.x
                miscPanel.y = slayersPanel.y + slayersPanel.height
            }
        }

        createdPanels[CategoryType.DEV]?.let { devPanel ->
            createdPanels[CategoryType.ESP]?.let { espPanel ->
                devPanel.x = espPanel.x
                devPanel.y = espPanel.y + espPanel.height
            }
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val mx = mouseX / scale
        val my = mouseY / scale

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        panels.forEach { it.draw(mx, my) }
        currentSettingMenu?.draw(mx, my)
        searchBar.draw(mx.toDouble(), my.toDouble())

        val str = "${FULL_PREFIX.remove("&n".addColor())} &6&lv$MOD_VERSION"
        val x = mc.getWidth() / scale - textRenderer.getStringWidth(str) - 2
        val y = mc.getHeight() / scale - textRenderer.getStringHeight(str)
        textRenderer.drawText(str, x, y)

        GlStateManager.popMatrix()

        dragging?.let { btn ->
            currentSettingMenu?.mouseDragged(mx, my, btn)
            if (draggingPanel != null && btn == 0) {
                draggingPanel?.x = mx - dragOffsetX
                draggingPanel?.y = my - dragOffsetY
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val mx = mouseX / scale
        val my = mouseY / scale

        if (currentSettingMenu == null) {
            searchBar.mouseClicked(mx.toDouble(), my.toDouble(), mouseButton)
            panels.forEach { it.mouseClicked(mx, my, mouseButton) }
            if (mouseButton == 0) {
                draggingPanel = panels.firstOrNull { isElementHovered(mx, my, it.x, it.y, it.width, 20) }

                if (draggingPanel != null) {
                    dragOffsetX = mx - draggingPanel !!.x
                    dragOffsetY = my - draggingPanel !!.y

                    panels.remove(draggingPanel)
                    panels.add(draggingPanel ?: return)
                }
            }
        }

        currentSettingMenu?.mouseClicked(mx, my, mouseButton)
        dragging = mouseButton
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        currentSettingMenu?.run {
            if (! keyTyped(typedChar, keyCode)) return@run
            return@keyTyped
        }

        if (currentSettingMenu != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) currentSettingMenu = null
            currentSettingMenu?.keyTyped(typedChar, keyCode)
            return
        }

        if (keyCode == Keyboard.KEY_F && isCtrlKeyDown()) {
            searchBar.focused = ! searchBar.focused
            return
        }

        if (searchBar.keyTyped(typedChar, keyCode)) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = null
        draggingPanel = null
        dragOffsetX = 0f
        dragOffsetY = 0f

        currentSettingMenu?.mouseReleased(mouseX / scale, mouseY / scale, state)
        searchBar.mouseRelease(state)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        val dWheel = Mouse.getDWheel().takeIf { it != 0 } ?: return
        val mx = MouseUtils.getMouseX() / scale
        val my = MouseUtils.getMouseY() / scale

        val scrollSensitivity = 0.7f
        val scrollDelta = - dWheel * scrollSensitivity * 0.2f

        if (currentSettingMenu == null) {
            val panelToScroll = panels.toList().asReversed().find {
                it.isHovered(mx, my) && it.open
            }

            if (panelToScroll != null && panelToScroll.isCurrentlyScrollable()) {
                val currentScrollY = panelToScroll.scrollY
                val newTargetScrollY = (currentScrollY + scrollDelta).coerceIn(0f, panelToScroll.getMaxScroll())

                if (abs(currentScrollY - newTargetScrollY) > 0.01f || abs(scrollDelta) > 0.01f) {
                    animateScroll(panelToScroll::scrollY, newTargetScrollY)
                }
            }
        }
        else if (currentSettingMenu?.isHovered(mx, my) == true) {
            val menu = currentSettingMenu ?: return
            if (menu.isCurrentlyScrollable()) {
                val currentScrollY = menu.scrollY
                val newTargetScrollY = (currentScrollY + scrollDelta).coerceIn(0f, menu.getMaxScroll())
                if (abs(currentScrollY - newTargetScrollY) > 0.01f || abs(scrollDelta) > 0.01f) {
                    animateScroll(menu::scrollY, newTargetScrollY)
                }
            }
        }
    }

    override fun onGuiClosed() {
        Config.save()
        searchBar.value = ""
        searchBar.focused = false
        currentSettingMenu = null
    }

    private fun animateScroll(targetRef: KMutableProperty0<Float>, targetValue: Float) {
        scrollJob?.cancel()

        scrollJob = scope.launch {
            val currentVal = targetRef.get()
            if (abs(currentVal - targetValue) < 0.01f) {
                if (isActive) targetRef.set(targetValue)
                return@launch
            }

            val smoothingFactor = 0.25f
            val epsilon = 0.1f
            var iterations = 0
            val maxIterations = 300

            while (isActive && abs(targetRef.get() - targetValue) > epsilon && iterations < maxIterations) {
                val currentValue = targetRef.get()
                val interpolatedValue = lerp(currentValue, targetValue, smoothingFactor)
                targetRef.set(interpolatedValue.toFloat())

                delay(7L)
                iterations ++
            }

            if (isActive) {
                targetRef.set(targetValue)
            }
        }
    }
}