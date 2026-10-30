package com.github.noamm9.utils

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.TextColor
import java.awt.Color

object ColorUtils {
    fun chromaColor(time: Double, period: Double = 4000.0, offset: Float = 0f, saturation: Float = 1f): Color {
        val hue = ((time % period) / period).toFloat() + offset
        return Color.getHSBColor(hue, saturation, 1f)
    }

    fun Color.withAlpha(i: Int) = if (i == alpha) this else Color(red, green, blue, i.coerceIn(0, 255))
    fun Color.withAlpha(f: Float) = withAlpha((f * 255).toInt())
    fun Color.lerp(color: Color, value: Number) = lerpColor(this, color, value)
    fun Color.isVisable() = alpha > 0

    fun lerpColor(color1: Color, color2: Color, value: Number) = Color(
        MathUtils.lerp(color1.red, color2.red, value).toInt(),
        MathUtils.lerp(color1.green, color2.green, value).toInt(),
        MathUtils.lerp(color1.blue, color2.blue, value).toInt()
    )

    fun colorizeScore(score: Int): String {
        return when {
            score < 270 -> "§c${score}"
            score < 300 -> "§e${score}"
            else -> "§a${score}"
        }
    }

    fun colorCodeByPercent(value: Number, maxValue: Number, reversed: Boolean = false): String {
        val max = maxValue.toFloat().coerceAtLeast(1f)
        val current = value.toFloat().coerceIn(0f, max)

        val percentage = (current / max) * 100f

        return when {
            percentage > 75 -> if (reversed) "§c" else "§a"
            percentage > 50 -> if (reversed) "§6" else "§e"
            percentage > 25 -> if (reversed) "§e" else "§6"
            else -> if (reversed) "§a" else "§c"
        }
    }

    val Color.mcColor get() = TextColor.fromRgb(rgb)
    val ChatFormatting.code get() = toString()[1]
    fun ChatFormatting.toColor(): Color = TextColor.fromLegacyFormat(this)?.value?.let(::Color) ?: Color.WHITE
    fun Color.invert() = Color(255 - red, 255 - green, 255 - blue, alpha)
}