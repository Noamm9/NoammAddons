package com.github.noamm9.ui.hud

import com.github.noamm9.config.Config
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.ui.utils.MouseHelper
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color

class HudEditorScreen: Screen(Component.literal("HudEditor")) {
    private val huds = FeatureManager.hudElements.filter { it.toggle }
    private var resetConfirmed = false

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
        ) { button ->
            if (! resetConfirmed) {
                button.message = Component.literal("§c§lConfirm Reset?")
                resetConfirmed = true
                return@UIButton
            }

            resetConfirmed = false
            button.message = Component.literal("§cReset HUD")
            FeatureManager.hudElements.forEach { element ->
                element.x = 20f
                element.y = 20f
                element.scale = 1f
            }
        })
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)
        val midX = Resolution.width / 2

        huds.forEach { it.drawEditor(graphics, mX, mY) }

        val element = huds.find { it.isDragging }
        graphics.drawCenteredString(element?.name.orEmpty(), midX, 10f, scale = 1.2f)
        graphics.drawCenteredString("ESC to Save and Exit", midX, Resolution.height - 20f, Color.GRAY, shadow = false)

        Resolution.pop(graphics)

        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        huds.forEach { element ->
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
            huds.forEach {
                it.startDragging(mX, mY)
                if (it.isDragging) {
                    MouseHelper.setCursor(GLFW.GLFW_RESIZE_ALL_CURSOR)
                    return true
                }
            }
        }

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        huds.forEach { it.isDragging = false }
        MouseHelper.resetCursor()
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun onClose() {
        MouseHelper.resetCursor()
        Config.save()
        super.onClose()
    }
}