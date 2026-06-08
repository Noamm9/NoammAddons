package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import org.lwjgl.glfw.GLFW

object ScrollableTooltip: Feature("Allows you to scroll through long tooltips.") {
    val scale by SliderSetting("Tooltip Scale", 100, 30, 150, 0.1).withDescription("how fast the tooltip scrolls")
    internal val scrollSpeed by SliderSetting("Scroll Speed", 3, 1, 10, 1).withDescription("how fast the tooltip scrolls")
    internal val scaleSpeed by SliderSetting("Scale Speed", 3, 1, 10, 1).withDescription("how fast the tooltip scales")

    @JvmField
    var scrollAmountX = 0f

    @JvmField
    var scrollAmountY = 0f

    @JvmField
    var scaleOverride = 0f

    @JvmStatic
    var slot = 0
        set(value) {
            if (value == field) return
            scrollAmountX = 0f
            scrollAmountY = 0f
            scaleOverride = 0f
            field = value
        }

    override fun init() {
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (! enabled) return@register
            val containerScreen = screen as? AbstractContainerScreen<*> ?: return@register
            ScreenMouseEvents.afterMouseScroll(containerScreen).register { _, _, _, _, verticalAmount, _ ->
                if (containerScreen is ContainerScreen && StorageOverlay.activeFor(containerScreen) != null) return@register false
                val hoveredSlot = (containerScreen as IAbstractContainerScreen).hoveredSlot ?: return@register false
                if (hoveredSlot.item.isEmpty) return@register false

                val scroll = (verticalAmount * scrollSpeed.value).toFloat()
                val holdingShift = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                val holdingCtrl = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS

                if (holdingShift && ! holdingCtrl) scrollAmountX -= scroll
                else if (! holdingShift && holdingCtrl) scaleOverride += (verticalAmount / 10f).toFloat() * scaleSpeed.value.toFloat()
                else scrollAmountY += scroll

                true
            }
        }
    }
}