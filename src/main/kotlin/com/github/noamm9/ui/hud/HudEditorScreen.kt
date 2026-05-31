package com.github.noamm9.ui.hud

import com.github.noamm9.config.Config
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color

object HudEditorScreen: Screen(Component.literal("HudEditor")) {
    val enabledElements get() = FeatureManager.hudElements.filter { it.toggle }

    override fun init() {
        super.init()

        val btnWidth = 100
        val btnHeight = 20

        addRenderableWidget(UIButton(
            width / 2 - btnWidth / 2,
            height - 100,
            btnWidth,
            btnHeight,
            "§cReset HUD"
        ) {
            FeatureManager.hudElements.forEach { element ->
                element.x = 20f
                element.y = 20f
                element.scale = 1f
            }
        })
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.refresh()
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)
        val midX = Resolution.width / 2

        enabledElements.forEach { it.drawEditor(graphics, mX, mY) }

        val element = enabledElements.find { it.isDragging }
        Render2D.drawCenteredString(graphics, element?.name.orEmpty(), midX, 10f, Color.WHITE, 1.2f)
        Render2D.drawCenteredString(graphics, "ESC to Save and Exit", midX, Resolution.height - 20f, Color.GRAY, shadow = false)

        Resolution.pop(graphics)

        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        enabledElements.forEach { element ->
            if (element.isDragging) {
                val increment = (vertical * 0.1).toFloat()
                element.scale = (element.scale + increment).coerceIn(0.5f, 5.0f)
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (super.mouseClicked(mouseButtonEvent, bl)) return true

        val mX = Resolution.getMouseX(mouseButtonEvent.x)
        val mY = Resolution.getMouseY(mouseButtonEvent.y)

        if (mouseButtonEvent.button() == 0) {
            enabledElements.forEach {
                it.startDragging(mX, mY)
                if (it.isDragging) return true
            }
        }

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        enabledElements.forEach { it.isDragging = false }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun onClose() {
        Config.save()
        super.onClose()
    }
}