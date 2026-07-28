package com.github.noamm9.ui.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.NumbersUtils.div
import net.minecraft.client.Minecraft
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
        val window = Minecraft.getInstance().window
        val guiWidth = window.guiScaledWidth.toFloat()
        val guiHeight = window.guiScaledHeight.toFloat()

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

    fun getMouseX() = (mc.mouseHandler.xpos() / mc.window.screenWidth.toDouble() * width).toInt()
    fun getMouseY() = (mc.mouseHandler.ypos() / mc.window.screenHeight.toDouble() * height).toInt()
}