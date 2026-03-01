package com.github.noamm9.utils

import net.minecraft.network.chat.TextColor
import java.awt.Color

object ColorUtils {
    fun Color.withAlpha(i: Int) = Color(this.red, this.green, this.blue, i.coerceIn(0, 255))
    fun Color.withAlpha(f: Float) = Color(this.red, this.green, this.blue, (255 * f).coerceIn(0f, 255f).toInt())

    fun Color.lerp(color: Color, value: Float): Color {
        return MathUtils.lerpColor(this, color, value)
    }

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

    val Color.mcColor: TextColor get() = TextColor.fromRgb(this.rgb)
}