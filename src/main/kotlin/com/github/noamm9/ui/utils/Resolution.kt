package com.github.noamm9.ui.utils

import com.github.noamm9.utils.NumbersUtils.div
import gg.essential.universal.UResolution
import gg.essential.universal.UMouse
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.min

object Resolution {
    private const val REFERANCE_WIDTH = 960f
    private const val REFERENCE_HEIGHT = 540f

    var scale = 1f
        private set

    var width = 960f
        private set

    var height = 540f
        private set

    private fun refresh() {
        val guiWidth = UResolution.scaledWidth.toFloat()
        val guiHeight = UResolution.scaledHeight.toFloat()

        scale = min(guiWidth / REFERANCE_WIDTH, guiHeight / REFERENCE_HEIGHT)

        width = guiWidth / scale
        height = guiHeight / scale
    }

    fun push(ctx: GuiGraphicsExtractor) {
        refresh()
        ctx.pose().pushMatrix()
        ctx.pose().scale(scale, scale)
    }

    fun pop(ctx: GuiGraphicsExtractor) {
        ctx.pose().popMatrix()
    }

    fun getMouseX(vanillaX: Number) = (vanillaX / scale).toInt()
    fun getMouseY(vanillaY: Number) = (vanillaY / scale).toInt()

    fun getMouseX() = (UMouse.Raw.x / UResolution.windowWidth * width).toInt()
    fun getMouseY() = (UMouse.Raw.y / UResolution.windowHeight * height).toInt()
}