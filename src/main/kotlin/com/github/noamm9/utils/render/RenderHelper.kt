package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.render.Render2D.visualOrder
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

object RenderHelper {
    val partialTicks get() = mc.deltaTracker.getGameTimeDeltaPartialTick(true)

    val Entity.renderX get() = xo + (x - xo) * partialTicks
    val Entity.renderY get() = yo + (y - yo) * partialTicks
    val Entity.renderZ get() = zo + (z - zo) * partialTicks

    val Entity.renderVec get() = Vec3(renderX, renderY, renderZ)

    val Entity.renderBoundingBox get() = boundingBox.move(renderX - x, renderY - y, renderZ - z)

    fun String.width() = lineSequence().maxOf { mc.font.width(it.visualOrder()) }
    fun String.height() = mc.font.lineHeight * (count { it == '\n' } + 1)
    fun String.wrap(maxWidth: Int) = buildList {
        for (raw in lineSequence()) {
            var line = ""
            for (word in raw.split(" ")) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (candidate.width() > maxWidth && line.isNotEmpty()) {
                    add(line)
                    line = activeFormatting(line) + word
                }
                else line = candidate
            }
            add(line)
        }
    }

    private fun activeFormatting(text: String): String {
        var color = ""
        var styles = ""

        for (i in 0 until text.length - 1) {
            if (text[i] != '&' && text[i] != '§') continue
            when (val code = text[i + 1].lowercaseChar()) {
                in '0' .. '9', in 'a' .. 'f' -> {
                    color = "${text[i]}$code"
                    styles = ""
                }

                in 'k' .. 'o' -> styles += "${text[i]}$code"
                'r' -> {
                    color = ""
                    styles = ""
                }
            }
        }

        return color + styles
    }
}