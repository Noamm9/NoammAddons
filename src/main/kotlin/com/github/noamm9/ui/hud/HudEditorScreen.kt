package com.github.noamm9.ui.hud

import com.github.noamm9.config.ConfigManager
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

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

        for (hud in huds) hud.drawEditor(graphics, mX, mY)

        val element = huds.find { it.isDragging }
        graphics.drawCenteredString(element?.name.orEmpty(), midX, 10f, scale = 1.2f)

        Resolution.pop(graphics)

        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        for (hud in huds) if (hud.isDragging) {
            val increment = (vertical * 0.1).toFloat()
            hud.scale = (hud.scale + increment).coerceIn(0.5f, 5.0f)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (super.mouseClicked(mouseButtonEvent, bl)) return true

        val mX = Resolution.getMouseX(mouseButtonEvent.x)
        val mY = Resolution.getMouseY(mouseButtonEvent.y)

        if (mouseButtonEvent.button() == 0) huds.forEach {
            it.startDragging(mX, mY)
            if (it.isDragging) return true
        }

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        for (hud in huds) hud.isDragging = false
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun onClose() {
        ConfigManager.save()
        super.onClose()
    }
}