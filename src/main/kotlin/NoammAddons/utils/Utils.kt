package NoammAddons.utils

import gg.essential.universal.UChat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.StringUtils
import NoammAddons.NoammAddons.Companion.CHAT_PREFIX
import NoammAddons.NoammAddons.Companion.mc
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.math.sign


object Utils {
    fun Any?.equalsOneOf(vararg other: Any): Boolean = other.any { this == it }

    fun String.removeFormatting(): String = StringUtils.stripControlCodes(this)

    val ItemStack.itemID: String
        get() = this.getSubCompound("ExtraAttributes", false)?.getString("id") ?: ""

    val ItemStack.lore: List<String>
        get() = this.tagCompound?.getCompoundTag("display")?.getTagList("Lore", 8)?.let {
            val list = mutableListOf<String>()
            for (i in 0 until it.tagCount()) {
                list.add(it.getStringTagAt(i))
            }
            list
        } ?: emptyList()

    fun ModMessage(message: String) = UChat.chat("$CHAT_PREFIX $message")

    fun Double.toFixed(digits: Int) = "%.${digits}f".format(this)

    fun formatNumber(num1: String): String {
        val num = num1.replace(",", "").toDoubleOrNull() ?: return "0"
        if (num == 0.0) return "0"

        val sign = num.sign
        val absNum = num.absoluteValue

        // Handle numbers less than 1
        if (absNum < 1) return "${if (sign == -1.0) "-" else ""}${"%.2f".format(absNum)}"

        val abbrev = listOf("", "k", "m", "b", "t", "q", "Q")
        val index = (Math.log10(absNum) / 3).toInt().coerceIn(abbrev.indices)

        val formattedNumber = "${(sign * absNum / Math.pow(10.0, (index * 3).toDouble())).toFixed(1)}${abbrev[index]}"

        return formattedNumber
    }

    fun addRandomColorCodes(inputString: String): String {
        val colorCodes = listOf("§6", "§c", "§e", "§f")
        val result = StringBuilder()

        for (char in inputString) {
            val randomColor = colorCodes.random()
            result.append(randomColor).append(char).append("§r")
        }

        return result.toString()
    }
}
