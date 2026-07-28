package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

object RenderHelper {
    val partialTicks get() = mc.deltaTracker.getGameTimeDeltaPartialTick(true)

    val Entity.renderX get() = xo + (x - xo) * partialTicks
    val Entity.renderY get() = yo + (y - yo) * partialTicks
    val Entity.renderZ get() = zo + (z - zo) * partialTicks

    val Entity.renderVec get() = Vec3(renderX, renderY, renderZ)

    val Entity.renderBoundingBox get() = boundingBox.move(renderX - x, renderY - y, renderZ - z)

    fun String.width() = addColor().lineSequence().maxOf(mc.font::width)
    fun String.height() = mc.font.lineHeight * (count { it == '\n' } + 1)
}