package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import gg.essential.universal.UGraphics
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

object RenderHelper {
    val partialTicks get() = mc.deltaTracker.getGameTimeDeltaPartialTick(true)

    val Entity.renderX get() = xo + (x - xo) * partialTicks
    val Entity.renderY get() = yo + (y - yo) * partialTicks
    val Entity.renderZ get() = zo + (z - zo) * partialTicks

    val Entity.renderVec get() = Vec3(renderX, renderY, renderZ)

    val Entity.renderBoundingBox get() = boundingBox.move(renderX - x, renderY - y, renderZ - z)

    fun String.width() = addColor().lineSequence().maxOf(UGraphics::getStringWidth)
    fun String.height() = UGraphics.getFontHeight() * (count { it == '\n' } + 1)
    fun String.wrap(maxWidth: Int) = buildList {
        for (raw in addColor().lineSequence()) {
            var line = ""
            for (word in raw.split(" ")) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (candidate.width() > maxWidth && line.isNotEmpty()) {
                    add(line)
                    line = word
                }
                else line = candidate
            }
            add(line)
        }
    }
}