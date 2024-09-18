package NoammAddons.utils

import net.minecraft.client.Minecraft

object Utils {
    fun Minecraft.getFPS() = Minecraft.getDebugFPS()

    fun Double.toFixed(decimals: Int): String {
        require(decimals >= 0) { "Decimal places must be non-negative" }
        return "%.${decimals}f".format(this)
    }

    fun Float.toFixed(decimals: Int): String {
        require(decimals >= 0) { "Decimal places must be non-negative" }
        return "%.${decimals}f".format(this.toDouble())
    }

    fun String.toFixed(decimals: Int): String {
        require(decimals >= 0) { "Decimal places must be non-negative" }
        val number = this.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid string format")
        return "%.${decimals}f".format(number)
    }
}